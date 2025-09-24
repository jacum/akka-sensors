package akka.sensors

import io.prometheus.metrics.core.metrics.{Gauge, Histogram}
import io.prometheus.metrics.model.registry.{Collector, PrometheusRegistry}
import akka.sensors.PrometheusCompat._

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
  def make(): DispatcherMetrics =
    DispatcherMetrics(
      queueTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_dispatchers_queue_time_millis")
        .help(s"Milliseconds in queue")
        .labelNames("dispatcher")
        .create(),
      runTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_dispatchers_run_time_millis")
        .help(s"Milliseconds running")
        .labelNames("dispatcher")
        .create(),
      activeThreads = Histogram
        .builder()
        .classicOnly()
        .name("akka_sensors_dispatchers_active_threads")
        .help(s"Active worker threads")
        .labelNames("dispatcher")
        .create(),
      threadStates = Gauge
        .builder()
        .name("akka_sensors_dispatchers_thread_states")
        .help("Threads per state and dispatcher")
        .labelNames("dispatcher", "state")
        .create(),
      threads = Gauge
        .builder()
        .name("akka_sensors_dispatchers_threads")
        .help("Threads per dispatcher")
        .labelNames("dispatcher")
        .create(),
      executorValue = Gauge
        .builder()
        .name("akka_sensors_dispatchers_executor_value")
        .help("Internal executor values per type")
        .labelNames("dispatcher", "value")
        .create()
    )

  def makeAndRegister(cr: PrometheusRegistry): DispatcherMetrics = {
    val metrics = make()
    metrics.allCollectors.foreach(c => cr.register(c))
    metrics
  }
}
