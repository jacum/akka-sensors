package akka.sensors.behavior

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.sensors.MetricOps._
import akka.sensors.{AkkaSensorsExtension, ClassNameUtil}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

final case class BasicActorMetrics[C](
    actorLabel: String,
    metrics: AkkaSensorsExtension
) {

  private lazy val exceptions = metrics.exceptions.labels(actorLabel)
  private val activeActors    = metrics.activeActors.labels(actorLabel)
  private val activityTimer   = metrics.activityTime.labels(actorLabel).startTimer()

  private def messageLabel(value: Any): Option[String] = Some(ClassNameUtil.simpleName(value.getClass))

  def apply(behavior: Behavior[C])(implicit ct: ClassTag[C]): Behavior[C] = {

    val interceptor = () =>
      new BehaviorInterceptor[C, C] {
        override def aroundSignal(
            ctx: TypedActorContext[C],
            signal: Signal,
            target: BehaviorInterceptor.SignalTarget[C]
        ): Behavior[C] = {
          signal match {
            case PostStop =>
              activeActors.dec()
              activityTimer.observeDuration()

            case _ =>
          }

          target(ctx, signal)
        }

        override def aroundStart(
            ctx: TypedActorContext[C],
            target: BehaviorInterceptor.PreStartTarget[C]
        ): Behavior[C] = {
          activeActors.inc()
          target.start(ctx)
        }

        @SuppressWarnings(Array("org.wartremover.warts.Throw"))
        override def aroundReceive(
            ctx: TypedActorContext[C],
            msg: C,
            target: BehaviorInterceptor.ReceiveTarget[C]
        ): Behavior[C] = {
          try {
            val next = messageLabel(msg)
              .map {
                metrics.receiveTime
                  .labels(actorLabel, _)
                  .observeExecution(target(ctx, msg))
              }
              .getOrElse(target(ctx, msg))

            if (Behavior.isUnhandled(next))
              messageLabel(msg)
                .foreach(metrics.unhandledMessages.labels(actorLabel, _).inc())
            next
          } catch {
            case NonFatal(e) =>
              exceptions.inc()
              throw e
          }
        }
      }

    Behaviors.intercept(interceptor)(behavior)
  }
}
