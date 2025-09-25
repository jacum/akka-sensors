package akka.sensors

import io.prometheus.metrics.core.datapoints.{CounterDataPoint, DistributionDataPoint, GaugeDataPoint}
import io.prometheus.metrics.core.metrics.{Counter, Gauge, Histogram}

object PrometheusCompat {
  implicit class CounterBuilderCompat(private val b: Counter.Builder) extends AnyVal {
    def create(): Counter = b.build()
  }
  implicit class GaugeBuilderCompat(private val b: Gauge.Builder) extends AnyVal {
    def create(): Gauge = b.build()
  }
  implicit class HistogramBuilderCompat(private val b: Histogram.Builder) extends AnyVal {
    def create(): Histogram = b.build()
  }

  // Compatibility aliases: old simpleclient used `.labels(...)`, new API uses `.labelValues(...)`
  implicit class CounterLabelsCompat(private val c: Counter) extends AnyVal {
    def labels(values: String*): CounterDataPoint = c.labelValues(values: _*)
  }
  implicit class GaugeLabelsCompat(private val g: Gauge) extends AnyVal {
    def labels(values: String*): GaugeDataPoint = g.labelValues(values: _*)
  }
  implicit class HistogramLabelsCompat(private val h: Histogram) extends AnyVal {
    def labels(values: String*): DistributionDataPoint = h.labelValues(values: _*)
  }
}
