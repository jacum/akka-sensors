package akka.sensors

import java.util.concurrent.{Executors, ScheduledExecutorService}

import akka.actor.{ActorSystem, ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.sensors.actor.ClusterEventWatchActor
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.client.{CollectorRegistry, Counter, Gauge, Histogram}

object AkkaSensors {
  // single-thread dedicated executor for low-frequency (some seconds between calls) sensors' internal business
  val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
}

class AkkaSensorsExtensionImpl(system: ExtendedActorSystem) extends Extension with MetricsBuilders with LazyLogging {

  logger.info("Akka Sensors extension has been activated")

  val namespace = "akka_sensors"
  val subsystem = "actor"

  private val clusterWatcherActor = system.actorOf(Props(classOf[ClusterEventWatchActor]), s"ClusterEventWatchActor")

  val activityTime: Histogram = secondsHistogram
    .name("activity_time_seconds")
    .help(s"Seconds of activity")
    .labelNames("actor")
    .register(registry)
  val activeActors: Gauge = gauge
    .name("active_actors_total")
    .help(s"Active actors")
    .labelNames("actor")
    .register(registry)
  val unhandledMessages: Counter = counter
    .name("unhandled_messages_total")
    .help(s"Unhandled messages")
    .labelNames("actor")
    .register(registry)
  val exceptions: Counter = counter
    .name("exceptions_total")
    .help(s"Exceptions thrown by actors")
    .labelNames("actor")
    .register(registry)
  val receiveTime: Histogram = millisHistogram
    .name("receive_time_millis")
    .help(s"Millis to process receive")
    .labelNames("actor")
    .register(registry)
  val clusterEvents: Counter = counter
    .name("cluster_events_total")
    .help(s"Number of cluster events, per type")
    .labelNames("event")
    .register(registry)
  val clusterMembers: Gauge = gauge
    .name("cluster_members_total")
    .help(s"Cluster members")
    .register(registry)
  val recoveryTime: Histogram = millisHistogram
    .name("recovery_time_millis")
    .help(s"Millis to process recovery")
    .labelNames("actor")
    .register(registry)
  val persistTime: Histogram = millisHistogram
    .name("persist_time_millis")
    .help(s"Millis to process single event persist")
    .labelNames("actor")
    .register(registry)
  val recoveryEvents: Counter = counter
    .name("recovery_events_total")
    .help(s"Recovery events by actors")
    .labelNames("actor")
    .register(registry)
  val persistFailures: Counter = counter
    .name("persist_failures_total")
    .help(s"Persist failures")
    .labelNames("actor")
    .register(registry)
  val recoveryFailures: Counter = counter
    .name("recovery_failures_total")
    .help(s"Recovery failures")
    .labelNames("actor")
    .register(registry)
  val persistRejects: Counter = counter
    .name("persist_rejects_total")
    .help(s"Persist rejects")
    .labelNames("actor")
    .register(registry)
}

object AkkaSensorsExtension extends ExtensionId[AkkaSensorsExtensionImpl] with ExtensionIdProvider {
  override def lookup: ExtensionId[_ <: Extension]                               = AkkaSensorsExtension
  override def createExtension(system: ExtendedActorSystem)                      = new AkkaSensorsExtensionImpl(system)
  override def get(system: ActorSystem): AkkaSensorsExtensionImpl                = super.get(system)
  override def get(system: ClassicActorSystemProvider): AkkaSensorsExtensionImpl = super.get(system)
}

object MetricOps {

  implicit class HistogramExtensions(val histogram: Histogram) {
    def observeExecution[A](f: => A): A = {
      val timer = histogram.startTimer()
      try f
      finally timer.observeDuration()
    }
  }

  implicit class HistogramChildExtensions(val histogram: Histogram.Child) {
    def observeExecution[A](f: => A): A = {
      val timer = histogram.startTimer()
      try f
      finally timer.observeDuration()
    }
  }

}

trait MetricsBuilders {
  def namespace: String
  def subsystem: String

  val registry: CollectorRegistry = CollectorRegistry.defaultRegistry

  def millisHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(.0005, .001, .0025, .005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10, 25, 50, 75, 100)
  def secondsHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(0, 1, 2.5, 5, 10, 25, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 7500, 10000)
  def valueHistogram(max: Int): Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .linearBuckets(0, 1, max)

  def counter: Counter.Builder = Counter.build().namespace(namespace).subsystem(subsystem)
  def gauge: Gauge.Builder     = Gauge.build().namespace(namespace).subsystem(subsystem)

}
