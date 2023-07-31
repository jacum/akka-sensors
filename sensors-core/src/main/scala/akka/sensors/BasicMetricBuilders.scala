package akka.sensors

import io.prometheus.client.{Counter, Gauge, Histogram}

trait BasicMetricBuilders {
  def namespace: String
  def subsystem: String
  def millisHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(.0005, .001, .005, .01, .05, .1, .5, 1, 5, 10, 50, 100, 500, 1000, 5000)
  def secondsHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(0, 1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000)
  def valueHistogram(max: Int): Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .linearBuckets(0, 1, max)
  def counter: Counter.Builder = Counter.build().namespace(namespace).subsystem(subsystem)
  def gauge: Gauge.Builder     = Gauge.build().namespace(namespace).subsystem(subsystem)
}

object BasicMetricBuilders {
  private final class Impl(nameSpace: String, subSystem: String) extends BasicMetricBuilders {
    override val namespace: String = nameSpace
    override val subsystem: String = subSystem
  }
  def make(namespace: String, subsystem: String): BasicMetricBuilders = new Impl(namespace, subsystem)
}
