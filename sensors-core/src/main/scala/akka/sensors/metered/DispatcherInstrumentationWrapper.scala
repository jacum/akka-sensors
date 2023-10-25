package akka.sensors.metered

import akka.sensors.dispatch.DispatcherInstrumentationWrapper.{InstrumentedRun, Run}
import akka.sensors.dispatch.RunnableWrapper
import akka.sensors.{DispatcherMetrics, RunnableWatcher}
import com.typesafe.config.Config

import java.util.concurrent.atomic.LongAdder
import scala.concurrent.duration.Duration

private[metered] class DispatcherInstrumentationWrapper(metrics: DispatcherMetrics, config: Config) {
  import DispatcherInstrumentationWrapper._
  import akka.sensors.dispatch.Helpers._

  private val executorConfig = config.getConfig("instrumented-executor")

  private val instruments: List[InstrumentedRun] =
    List(
      if (executorConfig.getBoolean("measure-runs")) Some(meteredRun(metrics, config.getString("id"))) else None,
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
}
private[metered] object DispatcherInstrumentationWrapper {
  def meteredRun(metrics: DispatcherMetrics, id: String): InstrumentedRun = {
    val currentWorkers = new LongAdder
    val queue          = metrics.queueTime.labels(id)
    val run            = metrics.runTime.labels(id)
    val active         = metrics.activeThreads.labels(id)

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

  private def watchedRun(id: String, tooLongThreshold: Duration, checkInterval: Duration): InstrumentedRun = {
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
