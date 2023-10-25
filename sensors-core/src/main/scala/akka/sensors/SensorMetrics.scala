package akka.sensors

import io.prometheus.client._

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

object SensorMetrics {
  def makeAndRegister(metricBuilders: BasicMetricBuilders, cr: CollectorRegistry): SensorMetrics = {
    val metrics = make(metricBuilders)
    register(metrics, cr)
    metrics
  }

  def make(metricBuilders: BasicMetricBuilders): SensorMetrics = {
    import metricBuilders._
    SensorMetrics(
      activityTime = secondsHistogram
        .name("activity_time_seconds")
        .help(s"Seconds of activity")
        .labelNames("actor")
        .create(),
      activeActors = gauge
        .name("active_actors_total")
        .help(s"Active actors")
        .labelNames("actor")
        .create(),
      unhandledMessages = counter
        .name("unhandled_messages_total")
        .help(s"Unhandled messages")
        .labelNames("actor", "message")
        .create(),
      exceptions = counter
        .name("exceptions_total")
        .help(s"Exceptions thrown by actors")
        .labelNames("actor")
        .create(),
      receiveTime = millisHistogram
        .name("receive_time_millis")
        .help(s"Millis to process receive")
        .labelNames("actor", "message")
        .create(),
      receiveTimeouts = counter
        .name("receive_timeouts_total")
        .help("Number of receive timeouts")
        .labelNames("actor")
        .create(),
      clusterEvents = counter
        .name("cluster_events_total")
        .help(s"Number of cluster events, per type")
        .labelNames("event", "member")
        .create(),
      clusterMembers = gauge
        .name("cluster_members_total")
        .help(s"Cluster members")
        .create(),
      recoveryTime = millisHistogram
        .name("recovery_time_millis")
        .help(s"Millis to process recovery")
        .labelNames("actor")
        .create(),
      persistTime = millisHistogram
        .name("persist_time_millis")
        .help(s"Millis to process single event persist")
        .labelNames("actor", "event")
        .create(),
      recoveries = counter
        .name("recoveries_total")
        .help(s"Recoveries by actors")
        .labelNames("actor")
        .create(),
      recoveryEvents = counter
        .name("recovery_events_total")
        .help(s"Recovery events by actors")
        .labelNames("actor")
        .create(),
      persistFailures = counter
        .name("persist_failures_total")
        .help(s"Persist failures")
        .labelNames("actor")
        .create(),
      recoveryFailures = counter
        .name("recovery_failures_total")
        .help(s"Recovery failures")
        .labelNames("actor")
        .create(),
      persistRejects = counter
        .name("persist_rejects_total")
        .help(s"Persist rejects")
        .labelNames("actor")
        .create()
    )
  }

  def register(metrics: SensorMetrics, cr: CollectorRegistry): Unit =
    metrics.allCollectors.foreach(cr.register)
}
