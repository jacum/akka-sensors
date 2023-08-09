package akka.sensors

import io.prometheus.client.{Collector, CollectorRegistry, Gauge, Histogram}

final case class DispatcherMetrics(
  queueTime: Histogram,
  runTime: Histogram,
  activeThreads: Histogram,
  threadStates: Gauge,
  threads: Gauge,
  executorValue: Gauge
) {
  val allCollectors: List[Collector] = List(queueTime, runTime, activeThreads, threadStates, threads, executorValue)
}

object DispatcherMetrics {
  def make(metricsBuilders: BasicMetricBuilders): DispatcherMetrics = {
    import metricsBuilders._

    DispatcherMetrics(
      queueTime = millisHistogram
        .name("queue_time_millis")
        .help(s"Milliseconds in queue")
        .labelNames("dispatcher")
        .create(),
      runTime = millisHistogram
        .name("run_time_millis")
        .help(s"Milliseconds running")
        .labelNames("dispatcher")
        .create(),
      activeThreads = valueHistogram(max = 32)
        .name("active_threads")
        .help(s"Active worker threads")
        .labelNames("dispatcher")
        .create(),
      threadStates = gauge
        .name("thread_states")
        .help("Threads per state and dispatcher")
        .labelNames("dispatcher", "state")
        .create(),
      threads = gauge
        .name("threads_total")
        .help("Threads per dispatcher")
        .labelNames("dispatcher")
        .create(),
      executorValue = gauge
        .name("executor_value")
        .help("Internal executor values per type")
        .labelNames("dispatcher", "value")
        .create()
    )
  }

  def register(metrics: DispatcherMetrics, cr: CollectorRegistry): Unit =
    metrics.allCollectors.foreach(cr.register)

  def makeAndRegister(metricBuilders: BasicMetricBuilders, cr: CollectorRegistry): DispatcherMetrics = {
    val metrics = make(metricBuilders)
    register(metrics, cr)
    metrics
  }
}
