package akka.sensors

import akka.sensors.MetricsTestUtils.{asMetricName, builder}
import io.prometheus.client.CollectorRegistry
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class DispatcherMetricsSpec extends AnyFreeSpec with Matchers {
  "DispatcherMetrics" - {
    "registers all metrics" in {
      val cr = new CollectorRegistry(true)
      DispatcherMetrics.makeAndRegister(builder, cr)
      val samples = cr.metricFamilySamples().asIterator().asScala.toList
      val names   = samples.map(_.name)

      // Pay attention that counter metrics does not have `_total` suffixes
      // Check io.prometheus.client.Counter.Builder.create(..) to see more
      names should contain(asMetricName("queue_time_millis"))
      names should contain(asMetricName("run_time_millis"))
      names should contain(asMetricName("active_threads"))
      names should contain(asMetricName("thread_states"))
      names should contain(asMetricName("threads_total"))
      names should contain(asMetricName("executor_value"))
    }
  }
}
