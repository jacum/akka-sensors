package akka.sensors

import io.prometheus.client.{Counter, Gauge, Histogram}

trait BasicMetricBuilders {
  def millisHistogram: Histogram.Builder
  def secondsHistogram: Histogram.Builder
  def valueHistogram(max: Int): Histogram.Builder
  def counter: Counter.Builder
  def gauge: Gauge.Builder
}

object BasicMetricBuilders {
  private final class Impl(namespace: String, subsystem: String) extends BasicMetricBuilders {
    override def millisHistogram: Histogram.Builder =
      Histogram
        .build()
        .namespace(namespace)
        .subsystem(subsystem)
        .buckets(.0005, .001, .005, .01, .05, .1, .5, 1, 5, 10, 50, 100, 500, 1000, 5000)
    override def secondsHistogram: Histogram.Builder =
      Histogram
        .build()
        .namespace(namespace)
        .subsystem(subsystem)
        .buckets(0, 1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000)
    override def valueHistogram(max: Int): Histogram.Builder =
      Histogram
        .build()
        .namespace(namespace)
        .subsystem(subsystem)
        .linearBuckets(0, 1, max)
    override def counter: Counter.Builder = Counter.build().namespace(namespace).subsystem(subsystem)
    override def gauge: Gauge.Builder     = Gauge.build().namespace(namespace).subsystem(subsystem)
  }
  def make(namespace: String, subsystem: String): BasicMetricBuilders = new Impl(namespace, subsystem)
}
