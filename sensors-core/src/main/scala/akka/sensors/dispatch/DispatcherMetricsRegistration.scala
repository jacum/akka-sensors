package akka.sensors.dispatch

import akka.sensors.{DispatcherMetrics, MetricsBuilders}
import io.prometheus.client.{Gauge, Histogram}

/** Creates and registers Dispatcher metrics in the global registry */
private[dispatch] object DispatcherMetricsRegistration extends MetricsBuilders {
  def namespace: String = "akka_sensors"
  def subsystem: String = "dispatchers"

  private val metrics          = DispatcherMetrics.makeAndRegister(this, registry)
  def queueTime: Histogram     = metrics.queueTime
  def runTime: Histogram       = metrics.runTime
  def activeThreads: Histogram = metrics.activeThreads
  def threadStates: Gauge      = metrics.threadStates
  def threads: Gauge           = metrics.threads
  def executorValue: Gauge     = metrics.executorValue
}
