package akka.sensors

import io.prometheus.client.CollectorRegistry
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._

import scala.jdk.CollectionConverters.IteratorHasAsScala
import SensorMetricsSpec._

class SensorMetricsSpec extends AnyFreeSpec {
  "SensorMetrics" - {
    "registers all metrics" in {
      val cr      = new CollectorRegistry(true)
      val builder = BasicMetricBuilders.make(TestNameSpace, TestSubSystem)
      val result  = SensorMetrics.makeAndRegister(builder, cr)

      val samples = cr.metricFamilySamples().asIterator().asScala.toList

      val names = samples.map(_.name)

      // Pay attention that counter metrics does not have `_total` suffixes
      // Check io.prometheus.client.Counter.Builder.create(..) to see more
      names should contain(asMetricName("activity_time_seconds"))
      names should contain(asMetricName("active_actors_total"))
      names should contain(asMetricName("unhandled_messages"))
      names should contain(asMetricName("exceptions"))
      names should contain(asMetricName("receive_time_millis"))
      names should contain(asMetricName("receive_timeouts"))
      names should contain(asMetricName("cluster_events"))
      names should contain(asMetricName("cluster_members_total"))
      names should contain(asMetricName("recovery_time_millis"))
      names should contain(asMetricName("persist_time_millis"))
      names should contain(asMetricName("recoveries"))
      names should contain(asMetricName("recovery_events"))
      names should contain(asMetricName("persist_failures"))
      names should contain(asMetricName("recovery_failures"))
      names should contain(asMetricName("persist_rejects"))
    }
  }
}

object SensorMetricsSpec {
  private val TestNameSpace = "test_namespace"
  private val TestSubSystem = "test_subsystem"
  private def asMetricName(in: String): String =
    s"${TestNameSpace}_${TestSubSystem}_$in"
}
