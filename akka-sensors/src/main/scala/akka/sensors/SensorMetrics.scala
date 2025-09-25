package akka.sensors

import io.prometheus.metrics.core.metrics._
import io.prometheus.metrics.model.registry.{Collector, PrometheusRegistry}
import akka.sensors.PrometheusCompat._

final case class SensorMetrics(
  activityTime: Histogram,
  activeActors: Gauge,
  unhandledMessages: Counter,
  exceptions: Counter,
  receiveTime: Histogram,
  receiveTimeouts: Counter,
  clusterEvents: Counter,
  clusterMembers: Gauge,
  recoveryTime: Histogram,
  recoveryToFirstEventTime: Histogram,
  persistTime: Histogram,
  recoveries: Counter,
  recoveryEvents: Counter,
  persistFailures: Counter,
  recoveryFailures: Counter,
  persistRejects: Counter,
  waitingForRecovery: Gauge,
  waitingForRecoveryTime: Histogram
) {
  val allCollectors: List[Collector] = List(
    activityTime,
    activeActors,
    unhandledMessages,
    exceptions,
    receiveTime,
    receiveTimeouts,
    clusterEvents,
    clusterMembers,
    recoveryTime,
    recoveryToFirstEventTime,
    persistTime,
    recoveries,
    recoveryEvents,
    persistFailures,
    recoveryFailures,
    persistRejects,
    waitingForRecovery,
    waitingForRecoveryTime
  )
}

object SensorMetrics {

  def make(): SensorMetrics =
    SensorMetrics(
      activityTime = Histogram
        .builder()
        .classicUpperBounds(10)
        .name("akka_sensors_actor_activity_time_seconds")
        .help(s"Seconds of activity")
        .labelNames("actor")
        .create(),
      activeActors = Gauge
        .builder()
        .name("akka_sensors_actor_active_actors")
        .help(s"Active actors")
        .labelNames("actor")
        .create(),
      unhandledMessages = Counter
        .builder()
        .name("akka_sensors_actor_unhandled_messages")
        .help(s"Unhandled messages")
        .labelNames("actor", "message")
        .create(),
      exceptions = Counter
        .builder()
        .name("akka_sensors_actor_exceptions")
        .help(s"Exceptions thrown by actors")
        .labelNames("actor")
        .create(),
      receiveTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_actor_receive_time_millis")
        .help(s"Millis to process receive")
        .labelNames("actor", "message")
        .create(),
      receiveTimeouts = Counter
        .builder()
        .name("akka_sensors_actor_receive_timeouts")
        .help("Number of receive timeouts")
        .labelNames("actor")
        .create(),
      clusterEvents = Counter
        .builder()
        .name("akka_sensors_actor_cluster_events")
        .help(s"Number of cluster events, per type")
        .labelNames("event", "member")
        .create(),
      clusterMembers = Gauge
        .builder()
        .name("akka_sensors_actor_cluster_members")
        .help(s"Cluster members")
        .create(),
      recoveryTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_actor_recovery_time_millis")
        .help(s"Millis to process recovery")
        .labelNames("actor")
        .create(),
      recoveryToFirstEventTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_actor_recovery_to_first_event_time_millis")
        .help(s"Millis to process recovery before first event is applied")
        .labelNames("actor")
        .create(),
      persistTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_actor_persist_time_millis")
        .help(s"Millis to process single event persist")
        .labelNames("actor", "event")
        .create(),
      recoveries = Counter
        .builder()
        .name("akka_sensors_actor_recoveries")
        .help(s"Recoveries by actors")
        .labelNames("actor")
        .create(),
      recoveryEvents = Counter
        .builder()
        .name("akka_sensors_actor_recovery_events")
        .help(s"Recovery events by actors")
        .labelNames("actor")
        .create(),
      persistFailures = Counter
        .builder()
        .name("akka_sensors_actor_persist_failures")
        .help(s"Persist failures")
        .labelNames("actor")
        .create(),
      recoveryFailures = Counter
        .builder()
        .name("akka_sensors_actor_recovery_failures")
        .help(s"Recovery failures")
        .labelNames("actor")
        .create(),
      persistRejects = Counter
        .builder()
        .name("akka_sensors_actor_persist_rejects")
        .help(s"Persist rejects")
        .labelNames("actor")
        .create(),
      waitingForRecovery = Gauge
        .builder()
        .name("akka_sensors_actor_waiting_for_recovery_permit_actors")
        .help(s"Actors waiting for recovery permit")
        .labelNames("actor")
        .create(),
      waitingForRecoveryTime = Histogram
        .builder()
        .classicUpperBounds(10000)
        .name("akka_sensors_actor_waiting_for_recovery_permit_time_millis")
        .help(s"Millis from actor creation to recovery permit being granted")
        .labelNames("actor")
        .create()
    )

  def makeAndRegister(cr: PrometheusRegistry): SensorMetrics = {
    val metrics = make()
    metrics.allCollectors.foreach(c => cr.register(c))
    metrics
  }

}
