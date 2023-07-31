package akka.sensors

import io.prometheus.client._

final case class Metrics(
  activityTime: Histogram,
  activeActors: Gauge,
  unhandledMessages: Counter,
  exceptions: Counter,
  receiveTime: Histogram,
  receiveTimeouts: Counter,
  clusterEvents: Counter,
  clusterMembers: Gauge,
  recoveryTime: Histogram,
  persistTime: Histogram,
  recoveries: Counter,
  recoveryEvents: Counter,
  persistFailures: Counter,
  recoveryFailures: Counter,
  persistRejects: Counter
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
    persistTime,
    recoveries,
    recoveryEvents,
    persistFailures,
    recoveryFailures,
    persistRejects
  )
}

object Metrics {
  def makeAndRegister(metricBuilders: BasicMetricBuilders, cr: CollectorRegistry): Metrics = {
    import metricBuilders._
    Metrics(
      activityTime = secondsHistogram
        .name("activity_time_seconds")
        .help(s"Seconds of activity")
        .labelNames("actor")
        .register(cr),
      activeActors = gauge
        .name("active_actors_total")
        .help(s"Active actors")
        .labelNames("actor")
        .register(cr),
      unhandledMessages = counter
        .name("unhandled_messages_total")
        .help(s"Unhandled messages")
        .labelNames("actor", "message")
        .register(cr),
      exceptions = counter
        .name("exceptions_total")
        .help(s"Exceptions thrown by actors")
        .labelNames("actor")
        .register(cr),
      receiveTime = millisHistogram
        .name("receive_time_millis")
        .help(s"Millis to process receive")
        .labelNames("actor", "message")
        .register(cr),
      receiveTimeouts = counter
        .name("receive_timeouts_total")
        .help("Number of receive timeouts")
        .labelNames("actor")
        .register(cr),
      clusterEvents = counter
        .name("cluster_events_total")
        .help(s"Number of cluster events, per type")
        .labelNames("event", "member")
        .register(cr),
      clusterMembers = gauge
        .name("cluster_members_total")
        .help(s"Cluster members")
        .register(cr),
      recoveryTime = millisHistogram
        .name("recovery_time_millis")
        .help(s"Millis to process recovery")
        .labelNames("actor")
        .register(cr),
      persistTime = millisHistogram
        .name("persist_time_millis")
        .help(s"Millis to process single event persist")
        .labelNames("actor", "event")
        .register(cr),
      recoveries = counter
        .name("recoveries_total")
        .help(s"Recoveries by actors")
        .labelNames("actor")
        .register(cr),
      recoveryEvents = counter
        .name("recovery_events_total")
        .help(s"Recovery events by actors")
        .labelNames("actor")
        .register(cr),
      persistFailures = counter
        .name("persist_failures_total")
        .help(s"Persist failures")
        .labelNames("actor")
        .register(cr),
      recoveryFailures = counter
        .name("recovery_failures_total")
        .help(s"Recovery failures")
        .labelNames("actor")
        .register(cr),
      persistRejects = counter
        .name("persist_rejects_total")
        .help(s"Persist rejects")
        .labelNames("actor")
        .register(cr)
    )
  }
}
