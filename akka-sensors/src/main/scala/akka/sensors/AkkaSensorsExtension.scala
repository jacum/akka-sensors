package akka.sensors

import akka.Done
import akka.actor.{ActorSystem, ClassicActorSystemProvider, CoordinatedShutdown, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.persistence.typed.scaladsl.EffectBuilder
import akka.sensors.actor.ClusterEventWatchActor
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.metrics.core.datapoints.DistributionDataPoint
import io.prometheus.metrics.core.metrics.{Counter, Gauge, Histogram}
import io.prometheus.metrics.model.registry.{Collector, PrometheusRegistry}

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.annotation.tailrec
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

  val prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry // todo how to parametrise/hook to other metric exports?

  /**
   * Safer than Class obj's getSimpleName, which may throw Malformed class name error in scala.
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
  @tailrec
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
class AkkaSensorsExtension(system: ExtendedActorSystem) extends Extension with LazyLogging {

  val registry = PrometheusRegistry.defaultRegistry

  logger.info(s"Akka Sensors extension has been activated: ${this.getClass.getName}")
  if (AkkaSensors.ClusterWatchEnabled)
    system.actorOf(Props(classOf[ClusterEventWatchActor]), s"ClusterEventWatchActor")

  CoordinatedShutdown(system)
    .addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "clearPrometheusRegistry") { () =>
      allCollectors.foreach(c => this.registry.unregister(c))
      logger.info("Cleared metrics")
      Future.successful(Done)
    }

  val metrics: SensorMetrics = SensorMetrics.makeAndRegister(this.registry)

  def activityTime: Histogram           = metrics.activityTime
  def activeActors: Gauge               = metrics.activeActors
  def unhandledMessages: Counter        = metrics.unhandledMessages
  def exceptions: Counter               = metrics.exceptions
  def receiveTime: Histogram            = metrics.receiveTime
  def receiveTimeouts: Counter          = metrics.receiveTimeouts
  def clusterEvents: Counter            = metrics.clusterEvents
  def clusterMembers: Gauge             = metrics.clusterMembers
  def recoveryTime: Histogram           = metrics.recoveryTime
  def persistTime: Histogram            = metrics.persistTime
  def recoveries: Counter               = metrics.recoveries
  def recoveryEvents: Counter           = metrics.recoveryEvents
  def persistFailures: Counter          = metrics.persistFailures
  def recoveryFailures: Counter         = metrics.recoveryFailures
  def persistRejects: Counter           = metrics.persistRejects
  def waitingForRecovery: Gauge         = metrics.waitingForRecovery
  def waitingForRecoveryTime: Histogram = metrics.waitingForRecoveryTime

  def allCollectors: List[Collector] = metrics.allCollectors
}

object AkkaSensorsExtension extends ExtensionId[AkkaSensorsExtension] with ExtensionIdProvider {
  override def lookup: ExtensionId[_ <: Extension] = AkkaSensorsExtension
  override def createExtension(system: ExtendedActorSystem): AkkaSensorsExtension = {
    val extensionClass = ConfigFactory.load().getString("akka.sensors.extension-class")
    Class.forName(extensionClass).getDeclaredConstructor(classOf[ExtendedActorSystem]).newInstance(system) match {
      case w: AkkaSensorsExtension => w
      case _                       => throw new IllegalArgumentException(s"Class $extensionClass must extend com.ing.bakery.baker.Watcher")
    }
  }
  override def get(system: ActorSystem): AkkaSensorsExtension                = super.get(system)
  override def get(system: ClassicActorSystemProvider): AkkaSensorsExtension = super.get(system)
}

object MetricOps {

  implicit class HistogramExtensions(val histogram: DistributionDataPoint) {
    def observeExecution[A](f: => A): A = {
      val timer = histogram.startTimer()
      try f
      finally timer.observeDuration()
    }

    def observeEffect[E, S](eff: EffectBuilder[E, S]): EffectBuilder[E, S] = {
      val timer = histogram.startTimer()
      eff.thenRun(_ => timer.observeDuration())
    }
  }

}
