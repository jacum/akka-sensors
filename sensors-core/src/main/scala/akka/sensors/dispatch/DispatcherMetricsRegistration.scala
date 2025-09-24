package akka.sensors.dispatch

import akka.sensors.{AkkaSensors, DispatcherMetrics}
import io.prometheus.metrics.core.metrics.{Gauge, Histogram}
import io.prometheus.metrics.model.registry.PrometheusRegistry

/** Creates and registers Dispatcher metrics in the global registry */
private[dispatch] object DispatcherMetricsRegistration {
  val registry: PrometheusRegistry = AkkaSensors.prometheusRegistry

  private val metrics          = DispatcherMetrics.makeAndRegister(registry)
  def queueTime: Histogram     = metrics.queueTime
  def runTime: Histogram       = metrics.runTime
  def activeThreads: Histogram = metrics.activeThreads
  def threadStates: Gauge      = metrics.threadStates
  def threads: Gauge           = metrics.threads
  def executorValue: Gauge     = metrics.executorValue
}
