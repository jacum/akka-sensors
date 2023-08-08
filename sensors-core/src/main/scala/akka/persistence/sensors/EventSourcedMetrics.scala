package akka.persistence.sensors

import akka.actor.typed.internal.BehaviorImpl.DeferredBehavior
import akka.actor.typed.internal.InterceptorImpl
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.{ReceiveImpl, ReceiveMessageImpl}
import akka.actor.typed.{Behavior, BehaviorInterceptor, ExtensibleBehavior, TypedActorContext}
import akka.persistence.typed.internal.{CompositeEffect, EventSourcedBehaviorImpl, Persist, PersistAll}
import akka.persistence.typed.scaladsl.{EffectBuilder, EventSourcedBehavior}
import akka.persistence.{JournalProtocol => P}
import akka.sensors.MetricOps._
import akka.sensors.{AkkaSensorsExtension, ClassNameUtil, SensorMetrics}
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.reflect.ClassTag

final case class EventSourcedMetrics[C, E, S](
  actorLabel: String,
  metrics: SensorMetrics
) extends LazyLogging {

  private lazy val recoveries       = metrics.recoveries.labels(actorLabel)
  private lazy val recoveryEvents   = metrics.recoveryEvents.labels(actorLabel)
  private lazy val recoveryTime     = metrics.recoveryTime.labels(actorLabel).startTimer()
  private lazy val recoveryFailures = metrics.recoveryFailures.labels(actorLabel)
  private lazy val persistFailures  = metrics.persistFailures.labels(actorLabel)
  private lazy val persistRejects   = metrics.persistRejects.labels(actorLabel)

  def messageLabel(value: Any): Option[String] =
    Some(ClassNameUtil.simpleName(value.getClass))

  def apply(behaviorToObserve: Behavior[C])(implicit ct: ClassTag[C]): Behavior[C] = {

    val interceptor = () =>
      new BehaviorInterceptor[Any, Any] {
        def aroundReceive(
          ctx: TypedActorContext[Any],
          msg: Any,
          target: BehaviorInterceptor.ReceiveTarget[Any]
        ): Behavior[Any] = {
          msg match {
            case res: P.Response =>
              res match {
                case _: P.ReplayedMessage       => recoveryEvents.inc()
                case _: P.ReplayMessagesFailure => recoveryFailures.inc()
                case _: P.WriteMessageRejected  => persistRejects.inc()
                case _: P.WriteMessageFailure   => persistFailures.inc()
                case _: P.RecoverySuccess =>
                  recoveries.inc()
                  recoveryTime.observeDuration()

                case _ =>
              }

            case _ =>
          }

          target(ctx, msg)
        }
      }

    Behaviors.intercept(interceptor)(observedBehavior(behaviorToObserve).unsafeCast[Any]).narrow
  }

  /**
   * recursively inspects subsequent chain of behaviors in behaviorToObserve to find a [[EventSourcedBehaviorImpl]]
   * then the function overrides behavior's command handler to start a [[metrics.persistTime]] timer when
   * [[Persist]] / [[PersistAll]] / [[CompositeEffect]] effects being produced.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private def observedBehavior(behaviorToObserve: Behavior[C]): Behavior[C] =
    behaviorToObserve match {

      case eventSourced: EventSourcedBehaviorImpl[C @unchecked, E @unchecked, S @unchecked] =>
        val observedCommandHandler: EventSourcedBehavior.CommandHandler[C, E, S] = (state: S, command: C) => {
          eventSourced.commandHandler(state, command) match {
            case eff: EffectBuilder[E, S] => observeEffect(eff)
            case other                    => other
          }
        }

        eventSourced.copy(commandHandler = observedCommandHandler)

      case deferred: DeferredBehavior[C] =>
        Behaviors.setup(ctx => observedBehavior(deferred(ctx)))

      case receive: ReceiveImpl[C] =>
        Behaviors.receive((ctx, msg) => observedBehavior(receive.onMessage(ctx, msg)))

      case receive: ReceiveMessageImpl[C @unchecked] =>
        Behaviors.receiveMessage(msg => observedBehavior(receive.onMessage(msg)))

      case interceptor: InterceptorImpl[_, C @unchecked] =>
        new InterceptorImpl(
          interceptor = interceptor.interceptor.asInstanceOf[BehaviorInterceptor[Any, C]],
          nestedBehavior = observedBehavior(interceptor.nestedBehavior)
        ).asInstanceOf[ExtensibleBehavior[C]]

      case other => other
    }

  private def observeEffect(effect: EffectBuilder[E, S]): EffectBuilder[E, S] = {

    def foldComposites(
      e: EffectBuilder[E, S],
      composites: List[CompositeEffect[E, S]]
    ): EffectBuilder[E, S] =
      composites.foldLeft(e)((e, c) => c.copy(persistingEffect = e))

    @tailrec
    def loop(
      e: EffectBuilder[E, S],
      composites: List[CompositeEffect[E, S]]
    ): EffectBuilder[E, S] =
      e match {

        case eff: Persist[E, S] =>
          val withMetrics = messageLabel(eff.event).map { label =>
            metrics.persistTime
              .labels(actorLabel, label)
              .observeEffect(eff)
          }
            .getOrElse(eff)
          foldComposites(withMetrics, composites)

        case eff: PersistAll[E, S] =>
          val withMetrics = metrics.persistTime
            .labels(actorLabel, "_all")
            .observeEffect(eff)
          foldComposites(withMetrics, composites)

        case eff: CompositeEffect[E, S] =>
          loop(eff.persistingEffect, eff :: composites)

        case other => foldComposites(other, composites)
      }

    loop(effect, Nil)
  }
}
