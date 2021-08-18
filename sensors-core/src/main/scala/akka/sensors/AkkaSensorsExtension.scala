package akka.sensors

import akka.Done
import akka.actor.{ActorSystem, ClassicActorSystemProvider, CoordinatedShutdown, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.sensors.actor.ClusterEventWatchActor
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.client.{CollectorRegistry, Counter, Gauge, Histogram}

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

object AkkaSensors extends LazyLogging {

  private val config                    = ConfigFactory.load().getConfig("akka.sensors")
  private val defaultPollInterval: Long = Try(config.getDuration("thread-state-snapshot-period", TimeUnit.SECONDS)).getOrElse(1L)
  val ClusterWatchEnabled: Boolean      = Try(config.getBoolean("cluster-watch-enabled")).getOrElse(false)

  // single-thread dedicated executor for low-frequency (some seconds between calls) sensors' internal business
  private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  private val periodicPolls                      = new TrieMap[String, Runnable]

  def schedule(id: String, poll: Runnable, interval: Duration = Duration(defaultPollInterval, TimeUnit.SECONDS)): Unit =
    periodicPolls.getOrElseUpdate(
      id, {
        executor.scheduleWithFixedDelay(poll, interval.length, interval.length, interval.unit)
        logger.info(s"Scheduled activity: $id")
        poll
      }
    )

  def prometheusRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry // todo how to parametrise/hook to other metric exports?

  /**
   * Safer than Class obj's getSimpleName which may throw Malformed class name error in scala.
   * This method mimics scalatest's getSimpleNameOfAnObjectsClass.
   */
  def getSimpleName(cls: Class[_]): String =
    try cls.getSimpleName
    catch {
      // TODO: the value returned here isn't even quite right; it returns simple names
      // like UtilsSuite$MalformedClassObject$MalformedClass instead of MalformedClass
      // The exact value may not matter much as it's used in log statements
      case _: InternalError =>
        stripDollars(stripPackages(cls.getName))
    }

  /**
   * Remove the packages from full qualified class name
   */
  private def stripPackages(fullyQualifiedName: String): String =
    fullyQualifiedName.split("\\.").takeRight(1)(0)

  /**
   * Remove trailing dollar signs from qualified class name,
   * and return the trailing part after the last dollar sign in the middle
   */
  private def stripDollars(s: String): String = {
    val lastDollarIndex = s.lastIndexOf('$')
    if (lastDollarIndex < s.length - 1)
      // The last char is not a dollar sign
      if (lastDollarIndex == -1 || !s.contains("$iw"))
        // The name does not have dollar sign or is not an interpreter
        // generated class, so we should return the full string
        s
      else
        // The class name is interpreter generated,
        // return the part after the last dollar sign
        // This is the same behavior as getClass.getSimpleName
        s.substring(lastDollarIndex + 1)
    else {
      // The last char is a dollar sign
      // Find last non-dollar char
      val lastNonDollarChar = s.findLast(_ != '$')
      lastNonDollarChar match {
        case None => s
        case Some(c) =>
          val lastNonDollarIndex = s.lastIndexOf(c)
          if (lastNonDollarIndex == -1)
            s
          else
            // Strip the trailing dollar signs
            // Invoke stripDollars again to get the simple name
            stripDollars(s.substring(0, lastNonDollarIndex + 1))
      }
    }
  }
}

/**
 * For overrides, make a subclass and put it's name in 'akka.sensors.extension-class' config value
 */
class AkkaSensorsExtension(system: ExtendedActorSystem) extends Extension with MetricsBuilders with LazyLogging {

  logger.info(s"Akka Sensors extension has been activated: ${this.getClass.getName}")

  val namespace = "akka_sensors"
  val subsystem = "actor"

  if (AkkaSensors.ClusterWatchEnabled)
    system.actorOf(Props(classOf[ClusterEventWatchActor]), s"ClusterEventWatchActor")

  CoordinatedShutdown(system)
    .addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "clearPrometheusRegistry") { () =>
      AkkaSensors.prometheusRegistry.clear()
      logger.info("Cleared metrics")
      Future.successful(Done)
    }

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
    .labelNames("actor", "message")
    .register(registry)
  val exceptions: Counter = counter
    .name("exceptions_total")
    .help(s"Exceptions thrown by actors")
    .labelNames("actor")
    .register(registry)
  val receiveTime: Histogram = millisHistogram
    .name("receive_time_millis")
    .help(s"Millis to process receive")
    .labelNames("actor", "message")
    .register(registry)
  val receiveTimeouts: Counter = counter
    .name("receive_timeouts_total")
    .help("Number of receive timeouts")
    .labelNames("actor")
    .register(registry)
  val clusterEvents: Counter = counter
    .name("cluster_events_total")
    .help(s"Number of cluster events, per type")
    .labelNames("event", "member")
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
    .labelNames("actor", "event")
    .register(registry)
  val recoveries: Counter = counter
    .name("recoveries_total")
    .help(s"Recoveries by actors")
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

object AkkaSensorsExtension extends ExtensionId[AkkaSensorsExtension] with ExtensionIdProvider {
  override def lookup: ExtensionId[_ <: Extension]                           = AkkaSensorsExtension
  override def createExtension(system: ExtendedActorSystem)                  = {
    val extensionClass = ConfigFactory.load().getString("akka.sensors.extension-class")
      Class.forName(extensionClass).getDeclaredConstructor(classOf[ExtendedActorSystem]).newInstance(system) match {
        case w: AkkaSensorsExtension => w
        case _ => throw new IllegalArgumentException(s"Class $extensionClass must extend com.ing.bakery.baker.Watcher")
      }
  }
  override def get(system: ActorSystem): AkkaSensorsExtension                = super.get(system)
  override def get(system: ClassicActorSystemProvider): AkkaSensorsExtension = super.get(system)
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

  val registry: CollectorRegistry = AkkaSensors.prometheusRegistry

  def millisHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(.0005, .001, .005, .01, .05, .1, .5, 1, 5, 10, 50, 100, 500, 1000, 5000)
  def secondsHistogram: Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .buckets(0, 1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000)
  def valueHistogram(max: Int): Histogram.Builder =
    Histogram
      .build()
      .namespace(namespace)
      .subsystem(subsystem)
      .linearBuckets(0, 1, max)

  def counter: Counter.Builder = Counter.build().namespace(namespace).subsystem(subsystem)
  def gauge: Gauge.Builder     = Gauge.build().namespace(namespace).subsystem(subsystem)

}
