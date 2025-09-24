package akka.sensors

import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class DispatcherMetricsSpec extends AnyFreeSpec with Matchers {
  "DispatcherMetrics" - {
    "registers all metrics" in {
      val cr = new PrometheusRegistry()
      DispatcherMetrics.makeAndRegister(cr)
      val samples = cr.scrape().iterator().asScala.toList
      val names   = samples.map(_.getMetadata.getName)

      names should contain("akka_sensors_dispatchers_queue_time_millis")
      names should contain("akka_sensors_dispatchers_run_time_millis")
      names should contain("akka_sensors_dispatchers_active_threads")
      names should contain("akka_sensors_dispatchers_thread_states")
      names should contain("akka_sensors_dispatchers_threads")
      names should contain("akka_sensors_dispatchers_executor_value")
    }
  }
}
