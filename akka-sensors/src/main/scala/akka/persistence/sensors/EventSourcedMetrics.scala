package akka.persistence.sensors

import akka.actor.typed.internal.BehaviorImpl.DeferredBehavior
import akka.actor.typed.internal.InterceptorImpl
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.{ReceiveImpl, ReceiveMessageImpl}
import akka.actor.typed.{Behavior, BehaviorInterceptor, ExtensibleBehavior, TypedActorContext}
import akka.persistence.typed.internal.{CompositeEffect, EffectImpl, EventSourcedBehaviorImpl, Persist, PersistAll}
import akka.persistence.typed.scaladsl.{EffectBuilder, EventSourcedBehavior}
import akka.persistence.{RecoveryPermitter, JournalProtocol => P}
import akka.sensors.MetricOps._
import akka.sensors.{ClassNameUtil, SensorMetrics}
import com.typesafe.scalalogging.LazyLogging
import akka.persistence.typed.scaladsl.Effect

import scala.annotation.tailrec

final case class EventSourcedMetrics[C, E, S](
  actorLabel: String,
  metrics: SensorMetrics
) extends LazyLogging {

  private val recoveries                = metrics.recoveries.labelValues(actorLabel)
  private val recoveryEvents            = metrics.recoveryEvents.labelValues(actorLabel)
  private var firstEventPassed: Boolean = false
  private val recoveryTime              = metrics.recoveryTime.labelValues(actorLabel).startTimer()
  private val recoveryToFirstEventTime  = metrics.recoveryToFirstEventTime.labelValues(actorLabel).startTimer()
  private val recoveryFailures          = metrics.recoveryFailures.labelValues(actorLabel)
  private val persistFailures           = metrics.persistFailures.labelValues(actorLabel)
  private val persistRejects            = metrics.persistRejects.labelValues(actorLabel)
  private val waitingForRecoveryGauge   = metrics.waitingForRecovery.labelValues(actorLabel)
  private val waitingForRecoveryTime    = metrics.waitingForRecoveryTime.labelValues(actorLabel).startTimer()

  waitingForRecoveryGauge.inc()

  def messageLabel(value: Any): Option[String] =
    Some(ClassNameUtil.simpleName(value.getClass))

  def apply(behaviorToObserve: Behavior[C]): Behavior[C] = {

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
                case _: P.ReplayedMessage =>
                  if (!firstEventPassed) {
                    recoveryToFirstEventTime.observeDuration()
                    firstEventPassed = true
                  }
                  recoveryEvents.inc()
                case _: P.ReplayMessagesFailure => recoveryFailures.inc()
                case _: P.WriteMessageRejected  => persistRejects.inc()
                case _: P.WriteMessageFailure   => persistFailures.inc()
                case _: P.RecoverySuccess =>
                  recoveries.inc()
                  recoveryTime.observeDuration()

                case _ =>
              }

            case RecoveryPermitter.RecoveryPermitGranted =>
              waitingForRecoveryGauge.dec()
              waitingForRecoveryTime.observeDuration()

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
            case eff: EffectImpl[E, S] => observeEffect(eff)
            // This case should never happen as `EffectImpl` is a parent for all real cases of `Effect`
            case other => other
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

  private def observeEffect(effect: EffectImpl[E, S]): Effect[E, S] = {
    def foldComposites[E1, S1](
      e: EffectBuilder[E1, S1],
      composites: List[CompositeEffect[E1, S1]]
    ): Effect[E1, S1] =
      composites.foldLeft(e)((e, c) => c.copy(persistingEffect = e))

    @tailrec
    def loop[E1, S1](
      e: EffectBuilder[E1, S1],
      composites: List[CompositeEffect[E1, S1]]
    ): Effect[E1, S1] =
      e match {
        case eff @ Persist(_) =>
          val withMetrics = messageLabel(eff.event).map { label =>
            metrics.persistTime
              .labelValues(actorLabel, label)
              .observeEffect(eff)
          }
            .getOrElse(eff)
          foldComposites(withMetrics, composites)

        case eff @ PersistAll(_) =>
          val withMetrics = metrics.persistTime
            .labelValues(actorLabel, "_all")
            .observeEffect(eff)
          foldComposites(withMetrics, composites)

        case CompositeEffect(pe, effs) =>
          loop(pe, CompositeEffect(pe, effs) :: composites)

        case other => foldComposites(other, composites)
      }

    loop(effect, Nil)
  }
}
