package akka.sensors

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._

import scala.jdk.CollectionConverters.IteratorHasAsScala
import io.prometheus.metrics.model.registry.PrometheusRegistry

class SensorMetricsSpec extends AnyFreeSpec {
  "SensorMetrics" - {
    "registers all metrics" in {
      val cr      = new PrometheusRegistry()
      val result  = SensorMetrics.makeAndRegister(cr)
      val samples = cr.scrape().iterator().asScala.toList
      val names   = samples.map(_.getMetadata.getName)

      names should contain("akka_sensors_actor_activity_time_seconds")
      names should contain("akka_sensors_actor_active_actors")
      names should contain("akka_sensors_actor_unhandled_messages")
      names should contain("akka_sensors_actor_exceptions")
      names should contain("akka_sensors_actor_receive_time_millis")
      names should contain("akka_sensors_actor_receive_timeouts")
      names should contain("akka_sensors_actor_cluster_events")
      names should contain("akka_sensors_actor_cluster_members")
      names should contain("akka_sensors_actor_recovery_time_millis")
      names should contain("akka_sensors_actor_persist_time_millis")
      names should contain("akka_sensors_actor_recoveries")
      names should contain("akka_sensors_actor_recovery_events")
      names should contain("akka_sensors_actor_persist_failures")
      names should contain("akka_sensors_actor_recovery_failures")
      names should contain("akka_sensors_actor_persist_rejects")
      names should contain("akka_sensors_actor_waiting_for_recovery_permit_actors")
      names should contain("akka_sensors_actor_waiting_for_recovery_permit_time_millis")
    }
  }
}
