package akka.sensors.actor

import akka.actor.{Actor, ActorLogging}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.sensors.AkkaSensorsExtension

import scala.util.control.NonFatal

trait ActorMetrics extends Actor with ActorLogging {
  _: Actor =>
  import akka.sensors.MetricOps._

  protected def actorTag: String = this.getClass.getSimpleName

  protected val metrics              = AkkaSensorsExtension(this.context.system)
  private val receiveTime            = metrics.receiveTime.labels(actorTag)
  private val activeActors           = metrics.activeActors.labels(actorTag)
  private lazy val exceptions        = metrics.exceptions.labels(actorTag)
  private lazy val unhandledMessages = metrics.unhandledMessages.labels(actorTag)

  private val activityTimer = metrics.activityTime.labels(actorTag).startTimer()

  protected[akka] override def aroundReceive(receive: Receive, msg: Any): Unit =
    try receiveTime.observeExecution(super.aroundReceive(receive, msg))
    catch {
      case NonFatal(e) =>
        exceptions.inc()
        throw e
    }

  protected[akka] override def aroundPreStart(): Unit = {
    super.aroundPreStart()
    activeActors.inc()
  }

  protected[akka] override def aroundPostStop(): Unit = {
    activeActors.dec()
    activityTimer.observeDuration()
    super.aroundPostStop()
  }

  override def unhandled(message: Any): Unit = {
    unhandledMessages.inc()
    super.unhandled(message)
  }

}

trait PersistentActorMetrics extends ActorMetrics {
  _: PersistentActor =>
  import akka.sensors.MetricOps._

  private val persistTime           = metrics.persistTime.labels(actorTag)
  private lazy val recoveryEvents   = metrics.recoveryEvents.labels(actorTag)
  private lazy val recoveryTime     = metrics.recoveryTime.labels(actorTag).startTimer()
  private lazy val recoveryFailures = metrics.recoveryFailures.labels(actorTag)
  private lazy val persistFailures  = metrics.persistFailures.labels(actorTag)
  private lazy val persistRejects   = metrics.persistRejects.labels(actorTag)

  override def receiveRecover: Receive = {
    case RecoveryCompleted =>
      recoveryTime.observeDuration()
    case e =>
      recoveryEvents.inc()
      this.receiveRecover(e)
  }

  override def persist[A](event: A)(handler: A => Unit): Unit =
    persistTime.observeExecution(
      this.internalPersist(event)(handler)
    )

  override def persistAll[A](events: Seq[A])(handler: A => Unit): Unit =
    persistTime.observeExecution(
      this.internalPersistAll(events)(handler)
    )

  override def persistAsync[A](event: A)(handler: A => Unit): Unit =
    persistTime.observeExecution(
      this.internalPersistAsync(event)(handler)
    )

  override def persistAllAsync[A](events: Seq[A])(handler: A => Unit): Unit =
    persistTime.observeExecution(
      this.internalPersistAllAsync(events)(handler)
    )

  protected override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
    log.error(cause, "Recovery failed")
    recoveryFailures.inc()
  }
  protected override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, "Persist failed")
    persistFailures.inc()
  }
  protected override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, "Persist rejected")
    persistRejects.inc()
  }
}
