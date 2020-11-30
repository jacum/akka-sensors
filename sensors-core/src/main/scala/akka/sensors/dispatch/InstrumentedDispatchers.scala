package akka.sensors.dispatch

import java.lang.management.{ManagementFactory, ThreadInfo, ThreadMXBean}
import java.util.concurrent._
import java.util.concurrent.atomic.LongAdder

import akka.dispatch._
import akka.event.Logging.{Error, Warning}
import akka.sensors.dispatch.DispatcherInstrumentationWrapper.Run
import akka.sensors.{AkkaSensors, MetricsBuilders, RunnableWatcher}
import com.typesafe.config.Config
import io.prometheus.client.{Gauge, Histogram}

import scala.PartialFunction.condOpt
import scala.concurrent.duration.{Duration, FiniteDuration}

object DispatcherMetrics extends MetricsBuilders {
  def namespace: String = "akka_sensors"
  def subsystem: String = "dispatchers"

  val queueTime: Histogram = millisHistogram
    .name("queue_time_millis")
    .help(s"Milliseconds in queue")
    .labelNames("dispatcher")
    .register(registry)

  val runTime: Histogram = millisHistogram
    .name("run_time_millis")
    .help(s"Milliseconds running")
    .labelNames("dispatcher")
    .register(registry)

  val activeThreads: Histogram = valueHistogram(max = 32)
    .name("active_threads_total")
    .help(s"Active worker threads")
    .labelNames("dispatcher")
    .register(registry)

  val threadStates: Gauge = gauge
    .name("threads_total")
    .help("Threads per state and dispatcher")
    .labelNames("dispatcher", "state")
    .register(registry)

  val executorValue: Gauge = gauge
    .name("executor_value")
    .help("Internal executor values per type")
    .labelNames("dispatcher", "value")
    .register(registry)
}

object AkkaRunnableWrapper {
  def unapply(runnable: Runnable): Option[Run => Runnable] =
    condOpt(runnable) {
      case runnable: Batchable => new BatchableWrapper(runnable, _)
      case runnable: Mailbox   => new MailboxWrapper(runnable, _)
    }

  class BatchableWrapper(self: Batchable, r: Run) extends Batchable {
    def run(): Unit          = r(() => self.run())
    def isBatchable: Boolean = self.isBatchable
  }

  class MailboxWrapper(self: Mailbox, r: Run) extends ForkJoinTask[Unit] with Runnable {
    def getRawResult: Unit          = self.getRawResult()
    def setRawResult(v: Unit): Unit = self.setRawResult(v)
    def exec(): Boolean             = r(() => self.exec())
    def run(): Unit = { exec(); () }
  }
}

class DispatcherInstrumentationWrapper(config: Config) {
  import DispatcherInstrumentationWrapper._
  import Helpers._

  private val executorConfig = config.getConfig("instrumented-executor")

  private val instruments: List[InstrumentedRun] =
    List(
      if (executorConfig.getBoolean("measure-runs")) Some(meteredRun(config.getString("id"))) else None,
      if (executorConfig.getBoolean("watch-long-runs"))
        Some(watchedRun(config.getString("id"), executorConfig.getMillisDuration("watch-too-long-run"), executorConfig.getMillisDuration("watch-check-interval")))
      else None
    ) flatten

  def apply(runnable: Runnable, execute: Runnable => Unit): Unit = {
    val beforeRuns = for { f <- instruments } yield f()
    val run = new Run {
      def apply[T](run: () => T): T = {
        val afterRuns = for { f <- beforeRuns } yield f()
        try run()
        finally for { f <- afterRuns } f()
      }
    }
    execute(RunnableWrapper(runnable, run))
  }

  object RunnableWrapper {

    def apply(runnableParam: Runnable, r: Run): Runnable =
      runnableParam match {
        case AkkaRunnableWrapper(runnable)  => runnable(r)
        case ScalaRunnableWrapper(runnable) => runnable(r)
        case runnable                       => new Default(runnable, r)
      }

    class Default(self: Runnable, r: Run) extends Runnable {
      def run(): Unit = r(() => self.run())
    }
  }
}

object DispatcherInstrumentationWrapper {
  trait Run { def apply[T](f: () => T): T }

  type InstrumentedRun = () => BeforeRun
  type BeforeRun       = () => AfterRun
  type AfterRun        = () => Unit

  val Empty: InstrumentedRun = () => () => () => ()

  import DispatcherMetrics._
  def meteredRun(id: String): InstrumentedRun = {
    val currentWorkers = new LongAdder
    val queue          = queueTime.labels(id)
    val run            = runTime.labels(id)
    val active         = activeThreads.labels(id)

    () => {
      val created = System.currentTimeMillis()
      () => {
        val started = System.currentTimeMillis()
        queue.observe((started - created).toDouble)
        currentWorkers.increment()
        active.observe(currentWorkers.intValue())
        () => {
          val stopped = System.currentTimeMillis()
          run.observe((stopped - started).toDouble)
          currentWorkers.decrement()
          active.observe(currentWorkers.intValue)
          ()
        }
      }
    }
  }

  def watchedRun(id: String, tooLongThreshold: Duration, checkInterval: Duration): InstrumentedRun = {
    val watcher = RunnableWatcher(tooLongRunThreshold = tooLongThreshold, checkInterval = checkInterval)

    () => { () =>
      val stop = watcher.start()
      () => {
        stop()
        ()
      }
    }
  }
}

class InstrumentedExecutor(val config: Config, val prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {

  lazy val delegate: ExecutorServiceConfigurator =
    configurator(config.getString("instrumented-executor.delegate"))

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val esf = delegate.createExecutorServiceFactory(id, threadFactory)
    import DispatcherMetrics._
    new ExecutorServiceFactory {
      def createExecutorService: ExecutorService = {
        val es                = esf.createExecutorService
        val activeCount       = executorValue.labels(id, "activeCount")
        val corePoolSize      = executorValue.labels(id, "corePoolSize")
        val largestPoolSize   = executorValue.labels(id, "largestPoolSize")
        val maximumPoolSize   = executorValue.labels(id, "maximumPoolSize")
        val queueSize         = executorValue.labels(id, "queueSize")
        val completedTasks    = executorValue.labels(id, "completedTasks")
        val poolSize          = executorValue.labels(id, "queueSize")
        val steals            = executorValue.labels(id, "steals")
        val parallelism       = executorValue.labels(id, "parallelism")
        val queuedSubmissions = executorValue.labels(id, "queuedSubmissions")
        val queuedTasks       = executorValue.labels(id, "queuedTasks")
        val runningThreads    = executorValue.labels(id, "runningThreads")

        es match {
          case tp: ThreadPoolExecutor =>
            AkkaSensors.executor.scheduleWithFixedDelay(
              () => {
                activeCount.set(tp.getActiveCount)
                corePoolSize.set(tp.getCorePoolSize)
                largestPoolSize.set(tp.getLargestPoolSize)
                maximumPoolSize.set(tp.getMaximumPoolSize)
                queueSize.set(tp.getQueue.size())
                completedTasks.set(tp.getCompletedTaskCount.toDouble)
                poolSize.set(tp.getPoolSize)
              },
              1L,
              1L,
              TimeUnit.SECONDS
            ) // todo parametrise

          case fj: ForkJoinPool =>
            AkkaSensors.executor.scheduleWithFixedDelay(
              () => {
                poolSize.set(fj.getPoolSize)
                steals.set(fj.getStealCount.toDouble)
                parallelism.set(fj.getParallelism)
                activeCount.set(fj.getActiveThreadCount)
                queuedSubmissions.set(fj.getQueuedSubmissionCount)
                queuedTasks.set(fj.getQueuedTaskCount.toDouble)
                runningThreads.set(fj.getRunningThreadCount)
              },
              1L,
              1L,
              TimeUnit.SECONDS
            ) // todo parametrise

          case _ =>
          // don't
        }

        es
      }
    }
  }

  def configurator(executor: String): ExecutorServiceConfigurator =
    executor match {
      case null | "" | "fork-join-executor" => new ForkJoinExecutorConfigurator(config.getConfig("fork-join-executor"), prerequisites)
      case "thread-pool-executor"           => new ThreadPoolExecutorConfigurator(config.getConfig("thread-pool-executor"), prerequisites)
      case fqcn =>
        val args = List(classOf[Config] -> config, classOf[DispatcherPrerequisites] -> prerequisites)
        prerequisites.dynamicAccess
          .createInstanceFor[ExecutorServiceConfigurator](fqcn, args)
          .recover({
            case exception =>
              throw new IllegalArgumentException(
                """Cannot instantiate ExecutorServiceConfigurator ("executor = [%s]"), defined in [%s],
                make sure it has an accessible constructor with a [%s,%s] signature"""
                  .format(fqcn, config.getString("id"), classOf[Config], classOf[DispatcherPrerequisites]),
                exception
              )
          })
          .get
    }

}

trait InstrumentedDispatcher extends Dispatcher {

  def actorSystemName: String
  private lazy val wrapper = new DispatcherInstrumentationWrapper(configurator.config)

  private val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean
  private val interestingStateNames = Set("runnable", "waiting", "timed_waiting", "blocked")
  private val interestingStates = Thread.State.values.filter(s => interestingStateNames.contains(s.name().toLowerCase))
  AkkaSensors.executor.scheduleWithFixedDelay(
    () => {
      val threads = threadMXBean
        .getThreadInfo(threadMXBean.getAllThreadIds, 0)
        .filter(t => t != null
          && interestingStateNames.contains(t.getThreadState.name().toLowerCase)
          && t.getThreadName.startsWith(s"$actorSystemName-$id"))

      interestingStates foreach { state =>
        val stateLabel = state.toString.toLowerCase
        val count      = threads.count(_.getThreadState.name().equalsIgnoreCase(stateLabel))
        DispatcherMetrics.threadStates
          .labels(id, stateLabel)
          .set(count)
      }
    },
    1L,
    1L,
    TimeUnit.SECONDS
  ) // todo configure thread state dump frequency?

  override def execute(runnable: Runnable): Unit = wrapper(runnable, super.execute)

  /**
   * Clone of [[Dispatcher.executorServiceFactoryProvider]]
   */
  protected[akka] override def registerForExecution(mbox: Mailbox, hasMessageHint: Boolean, hasSystemMessageHint: Boolean): Boolean =
    if (mbox.canBeScheduledForExecution(hasMessageHint, hasSystemMessageHint))
      if (mbox.setAsScheduled())
        try {
          wrapper(mbox, executorService.execute)
          true
        } catch {
          case _: RejectedExecutionException =>
            try {
              wrapper(mbox, executorService.execute)
              true
            } catch { //Retry once
              case e: RejectedExecutionException =>
                mbox.setAsIdle()
                eventStream.publish(Error(e, getClass.getName, getClass, "registerForExecution was rejected twice!"))
                throw e
            }
        }
      else false
    else false
}

/** Instrumented clone of [[akka.dispatch.DispatcherConfigurator]]. */
class InstrumentedDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {

  import Helpers._

  private val instance = new Dispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    config.getNanosDuration("throughput-deadline-time"),
    configureExecutor(),
    config.getMillisDuration("shutdown-timeout")
  ) with InstrumentedDispatcher {
    def actorSystemName: String = prerequisites.mailboxes.settings.name
  }

  def dispatcher(): MessageDispatcher = instance

}

/** Instrumented clone of [[akka.dispatch.PinnedDispatcherConfigurator]]. */
class InstrumentedPinnedDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {
  import Helpers._

  private val threadPoolConfig: ThreadPoolConfig = configureExecutor() match {
    case e: ThreadPoolExecutorConfigurator => e.threadPoolConfig
    case _ =>
      prerequisites.eventStream.publish(
        Warning(
          "PinnedDispatcherConfigurator",
          this.getClass,
          "PinnedDispatcher [%s] not configured to use ThreadPoolExecutor, falling back to default config.".format(config.getString("id"))
        )
      )
      ThreadPoolConfig()
  }

  override def dispatcher(): MessageDispatcher =
    new PinnedDispatcher(this, null, config.getString("id"), config.getMillisDuration("shutdown-timeout"), threadPoolConfig) with InstrumentedDispatcher {
      def actorSystemName: String = prerequisites.mailboxes.settings.name
    }

}

object Helpers {

  /**
   * INTERNAL API
   */
  private[akka] implicit final class ConfigOps(val config: Config) extends AnyVal {
    def getMillisDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.MILLISECONDS)

    def getNanosDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.NANOSECONDS)

    private def getDuration(path: String, unit: TimeUnit): FiniteDuration =
      Duration(config.getDuration(path, unit), unit)
  }
}
