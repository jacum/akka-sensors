package akka.sensors

import io.prometheus.metrics.core.metrics._
import io.prometheus.metrics.model.registry.{Collector, PrometheusRegistry}

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
        .classicExponentialUpperBounds(60, 2, 15)
        .name("akka_sensors_actor_activity_time_seconds")
        .help(s"Seconds of activity")
        .labelNames("actor")
        .build(),
      activeActors = Gauge
        .builder()
        .name("akka_sensors_actor_active_actors")
        .help(s"Active actors")
        .labelNames("actor")
        .build(),
      unhandledMessages = Counter
        .builder()
        .name("akka_sensors_actor_unhandled_messages")
        .help(s"Unhandled messages")
        .labelNames("actor", "message")
        .build(),
      exceptions = Counter
        .builder()
        .name("akka_sensors_actor_exceptions")
        .help(s"Exceptions thrown by actors")
        .labelNames("actor")
        .build(),
      receiveTime = Histogram
        .builder()
        .classicExponentialUpperBounds(0.5, 2, 14)
        .name("akka_sensors_actor_receive_time_millis")
        .help(s"Millis to process receive")
        .labelNames("actor", "message")
        .build(),
      receiveTimeouts = Counter
        .builder()
        .name("akka_sensors_actor_receive_timeouts")
        .help("Number of receive timeouts")
        .labelNames("actor")
        .build(),
      clusterEvents = Counter
        .builder()
        .name("akka_sensors_actor_cluster_events")
        .help(s"Number of cluster events, per type")
        .labelNames("event", "member")
        .build(),
      clusterMembers = Gauge
        .builder()
        .name("akka_sensors_actor_cluster_members")
        .help(s"Cluster members")
        .build(),
      recoveryTime = Histogram
        .builder()
        .classicExponentialUpperBounds(0.5, 2, 14)
        .name("akka_sensors_actor_recovery_time_millis")
        .help(s"Millis to process recovery")
        .labelNames("actor")
        .build(),
      recoveryToFirstEventTime = Histogram
        .builder()
        .classicExponentialUpperBounds(0.5, 2, 14)
        .name("akka_sensors_actor_recovery_to_first_event_time_millis")
        .help(s"Millis to process recovery before first event is applied")
        .labelNames("actor")
        .build(),
      persistTime = Histogram
        .builder()
        .classicExponentialUpperBounds(0.5, 2, 14)
        .name("akka_sensors_actor_persist_time_millis")
        .help(s"Millis to process single event persist")
        .labelNames("actor", "event")
        .build(),
      recoveries = Counter
        .builder()
        .name("akka_sensors_actor_recoveries")
        .help(s"Recoveries by actors")
        .labelNames("actor")
        .build(),
      recoveryEvents = Counter
        .builder()
        .name("akka_sensors_actor_recovery_events")
        .help(s"Recovery events by actors")
        .labelNames("actor")
        .build(),
      persistFailures = Counter
        .builder()
        .name("akka_sensors_actor_persist_failures")
        .help(s"Persist failures")
        .labelNames("actor")
        .build(),
      recoveryFailures = Counter
        .builder()
        .name("akka_sensors_actor_recovery_failures")
        .help(s"Recovery failures")
        .labelNames("actor")
        .build(),
      persistRejects = Counter
        .builder()
        .name("akka_sensors_actor_persist_rejects")
        .help(s"Persist rejects")
        .labelNames("actor")
        .build(),
      waitingForRecovery = Gauge
        .builder()
        .name("akka_sensors_actor_waiting_for_recovery_permit_actors")
        .help(s"Actors waiting for recovery permit")
        .labelNames("actor")
        .build(),
      waitingForRecoveryTime = Histogram
        .builder()
        .classicExponentialUpperBounds(0.5, 2, 14)
        .name("akka_sensors_actor_waiting_for_recovery_permit_time_millis")
        .help(s"Millis from actor creation to recovery permit being granted")
        .labelNames("actor")
        .build()
    )

  def makeAndRegister(cr: PrometheusRegistry): SensorMetrics = {
    val metrics = make()
    metrics.allCollectors.foreach(c => cr.register(c))
    metrics
  }

}
