package akka.sensors.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.sensors.EventSourcedMetrics
import akka.sensors.{AkkaSensorsExtension, ClassNameUtil, Metrics}

import scala.reflect.ClassTag

object BehaviorMetrics {

  private type CreateBehaviorMetrics[C] = (Metrics, Behavior[C]) => Behavior[C]
  private val defaultMessageLabel: Any => Option[String] = msg => Some(ClassNameUtil.simpleName(msg.getClass))

  def apply[C: ClassTag](actorLabel: String, getLabel: C => Option[String] = defaultMessageLabel): BehaviorMetricsBuilder[C] = {
    val defaultMetrics = (metrics: Metrics, behavior: Behavior[C]) => BasicActorMetrics[C](actorLabel, metrics, getLabel)(behavior)
    new BehaviorMetricsBuilder(actorLabel, defaultMetrics :: Nil)
  }

  class BehaviorMetricsBuilder[C: ClassTag](
    actorLabel: String,
    createMetrics: List[CreateBehaviorMetrics[C]]
  ) { self =>

    def setup(factory: ActorContext[C] => Behavior[C]): Behavior[C] =
      Behaviors.setup { actorContext =>
        val behavior = factory(actorContext)
        val metrics  = AkkaSensorsExtension(actorContext.asScala.system).metrics
        createMetrics.foldLeft(behavior)((b, createMetrics) => createMetrics(metrics, b))
      }

    def withReceiveTimeoutMetrics(timeoutCmd: C): BehaviorMetricsBuilder[C] = {
      val receiveTimeoutMetrics = (metrics: Metrics, behavior: Behavior[C]) => ReceiveTimeoutMetrics[C](actorLabel, metrics, timeoutCmd)(behavior)
      new BehaviorMetricsBuilder[C](self.actorLabel, receiveTimeoutMetrics :: self.createMetrics)
    }

    def withPersistenceMetrics: BehaviorMetricsBuilder[C] = {
      val eventSourcedMetrics = (metrics: Metrics, behaviorToObserve: Behavior[C]) => EventSourcedMetrics(actorLabel, metrics)(behaviorToObserve)
      new BehaviorMetricsBuilder[C](actorLabel, eventSourcedMetrics :: self.createMetrics)
    }
  }
}
