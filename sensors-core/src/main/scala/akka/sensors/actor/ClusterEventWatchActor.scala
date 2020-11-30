package akka.sensors.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.sensors.{AkkaSensorsExtension, AkkaSensorsExtensionImpl}

class ClusterEventWatchActor extends Actor with ActorLogging {

  private val cluster                           = Cluster(context.system)
  private val metrics: AkkaSensorsExtensionImpl = AkkaSensorsExtension(this.context.system)
  private val clusterEvents                     = metrics.clusterEvents

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[ClusterDomainEvent])
    log.info("Starting cluster event watch")
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  private def registerEvent(e: ClusterDomainEvent): Unit = {
    val eventName = e.getClass.getSimpleName
    clusterEvents.labels(eventName).inc()
  }

  def receive: Receive = {
    case e @ MemberUp(member) =>
      registerEvent(e)
      log.info("Member is Up: {}", member.address)
    case e @ UnreachableMember(member) =>
      registerEvent(e)
      log.info("Member detected as unreachable: {}", member)
    case e @ MemberRemoved(member, previousStatus) =>
      registerEvent(e)
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case e @ MemberDowned(member) =>
      registerEvent(e)
      log.info("Member is Down: {}", member.address)
    case e: ClusterDomainEvent =>
      registerEvent(e)
      log.info(s"Cluster domain event: $e")
  }

}
