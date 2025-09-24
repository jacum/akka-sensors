package akka.sensors.metered

import akka.dispatch.{DispatcherPrerequisites, ExecutorServiceConfigurator, ExecutorServiceFactory, ForkJoinExecutorConfigurator, ThreadPoolExecutorConfigurator}
import akka.sensors.AkkaSensors
import akka.sensors.PrometheusCompat.GaugeLabelsCompat
import com.typesafe.config.Config

import java.util.concurrent.{ExecutorService, ForkJoinPool, ThreadFactory, ThreadPoolExecutor}

class MeteredInstrumentedExecutor(val config: Config, val prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {
  private val metrics = MeteredDispatcherSetup.setupOrThrow(prerequisites).metrics

  private lazy val delegate: ExecutorServiceConfigurator =
    serviceConfigurator(config.getString("instrumented-executor.delegate"))

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val esf = delegate.createExecutorServiceFactory(id, threadFactory)
    new ExecutorServiceFactory {
      def createExecutorService: ExecutorService = {
        val es = esf.createExecutorService

        val activeCount       = metrics.executorValue.labels(id, "activeCount")
        val corePoolSize      = metrics.executorValue.labels(id, "corePoolSize")
        val largestPoolSize   = metrics.executorValue.labels(id, "largestPoolSize")
        val maximumPoolSize   = metrics.executorValue.labels(id, "maximumPoolSize")
        val queueSize         = metrics.executorValue.labels(id, "queueSize")
        val completedTasks    = metrics.executorValue.labels(id, "completedTasks")
        val poolSize          = metrics.executorValue.labels(id, "poolSize")
        val steals            = metrics.executorValue.labels(id, "steals")
        val parallelism       = metrics.executorValue.labels(id, "parallelism")
        val queuedSubmissions = metrics.executorValue.labels(id, "queuedSubmissions")
        val queuedTasks       = metrics.executorValue.labels(id, "queuedTasks")
        val runningThreads    = metrics.executorValue.labels(id, "runningThreads")

        es match {
          case tp: ThreadPoolExecutor =>
            AkkaSensors.schedule(
              id,
              () => {
                activeCount.set(tp.getActiveCount)
                corePoolSize.set(tp.getCorePoolSize)
                largestPoolSize.set(tp.getLargestPoolSize)
                maximumPoolSize.set(tp.getMaximumPoolSize)
                queueSize.set(tp.getQueue.size())
                completedTasks.set(tp.getCompletedTaskCount.toDouble)
                poolSize.set(tp.getPoolSize)
              }
            )

          case fj: ForkJoinPool =>
            AkkaSensors.schedule(
              id,
              () => {
                poolSize.set(fj.getPoolSize)
                steals.set(fj.getStealCount.toDouble)
                parallelism.set(fj.getParallelism)
                activeCount.set(fj.getActiveThreadCount)
                queuedSubmissions.set(fj.getQueuedSubmissionCount)
                queuedTasks.set(fj.getQueuedTaskCount.toDouble)
                runningThreads.set(fj.getRunningThreadCount)
              }
            )

          case _ =>

        }

        es
      }
    }
  }

  private def serviceConfigurator(executor: String): ExecutorServiceConfigurator =
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
