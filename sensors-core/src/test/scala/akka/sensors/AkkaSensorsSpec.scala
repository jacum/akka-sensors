package akka.sensors

import java.io.CharArrayWriter
import akka.actor.{Actor, ActorRef, ActorSystem, NoSerializationVerificationNeeded, PoisonPill, Props, ReceiveTimeout}
import akka.pattern.ask
import akka.sensors.actor.{ActorMetrics, PersistentActorMetrics}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Millis, Seconds, Span}

import scala.Console.println
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Random

class AkkaSensorsSpec extends AnyFreeSpec with LazyLogging with Eventually with BeforeAndAfterAll {

  import InstrumentedActors._
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val system: ActorSystem = ActorSystem("instrumented")
  private lazy val probeActor = system.actorOf(Props(classOf[InstrumentedProbe]), s"probe")
  private lazy val persistentActor = system.actorOf(Props(classOf[PersistentInstrumentedProbe]), s"persistent")
  private lazy val persistentActor2 = system.actorOf(Props(classOf[AnotherPersistentInstrumentedProbe]), s"another-persistent")
  implicit val registry: CollectorRegistry = AkkaSensors.prometheusRegistry

  "Launch akka app, and ensure it works" - {

    "starts actor system and pings the bootstrap actor" in {
      pingActor
    }

    "ensure prometheus JMX scraping is working" in {

      for (_ <- 1 to 5) probeActor ! KnownError
      for (_ <- 1 to 100) probeActor ! UnknownMessage
      probeActor ! BlockTooLong
      for (_ <- 1 to 1000) {
        pingActor
      }

      probeActor ! PoisonPill

      for (_ <- 1 to 100) sendEventAck(persistentActor)

      for (_ <- 1 to 100) sendEventAck(persistentActor2)

      persistentActor ! PoisonPill
      persistentActor2 ! PoisonPill

      Thread.sleep(100)

      val blockingIo = system.dispatchers.lookup("akka.actor.default-blocking-io-dispatcher")
      blockingIo.execute(() => { Thread.sleep(100)})

      system.actorOf(Props(classOf[PersistentInstrumentedProbe]), s"persistent")
      system.actorOf(Props(classOf[AnotherPersistentInstrumentedProbe]), s"another-persistent")

      val content = metrics.split("\n")
      List("akka_sensors_actor", "akka_sensors_dispatchers").foreach(s =>
        assert(content.exists(_.startsWith(s)), s"starts with $s")
      )
    }

    "ensure MANY actors are created and stopped, all accounted for" in {
      val actors = 200000
      val refs = (1 to actors).map(v =>
        system.actorOf(Props(classOf[MassProbe]), s"mass-$v")
      )

      implicit val patienceConfig: PatienceConfig = PatienceConfig(10 seconds, 100 milliseconds)
      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassProbe\""),
          _.endsWith(s" $actors.0")
        )
      }
      eventually {
        refs.foreach(r => assert(!r.isTerminated))
      }
      refs.foreach(_ ! Ping("1"))
      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_receive_time_millis_count{actor=\"MassProbe\""),
          _.endsWith(s" $actors.0")
        )
      }
      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassProbe\""),
          _.endsWith(s" 0.0")
        )
        assertMetrics(
          _.startsWith("akka_sensors_actor_receive_timeouts_total{actor=\"MassProbe\""),
          _.endsWith(s" $actors.0")
        )
      }
      assertMetrics(
        _.startsWith("akka_sensors_actor_activity_time_seconds_bucket{actor=\"MassProbe\",le=\"10.0\""),
        _.endsWith(s" $actors.0")
      )
    }

    "ensure many persistent are created and stopped, all accounted for" in {
      val actors = 1000
      val commands = 10
      val refs = (1 to actors).map(v =>
        system.actorOf(Props(classOf[MassPersistentProbe]), s"mass-persistent-$v")
      )

      implicit val patienceConfig: PatienceConfig = PatienceConfig(10 seconds, 100 milliseconds)
      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" $actors.0")
        )
      }
      eventually {
        refs.foreach(r => assert(!r.isTerminated))
      }
      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" $actors.0")
        )
      }

      for (e <- 1 to commands) refs.foreach(a => {
        a ! ValidCommand(e.toString)
      })

      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_persist_time_millis_count{actor=\"MassPersistentProbe\""),
          _.endsWith(s" ${actors*commands}.0")
        )
      }

      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" 0.0")
        )
        assertMetrics(
          _.startsWith("akka_sensors_actor_receive_timeouts_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" $actors.0")
        )
      }

      assertMetrics(
        _.startsWith("akka_sensors_actor_activity_time_seconds_bucket{actor=\"MassPersistentProbe\",le=\"10.0\""),
        _.endsWith(s" $actors.0")
      )

      assertMetrics(
        _.startsWith("akka_sensors_actor_persist_time_millis_count{actor=\"MassPersistentProbe\",event=\"ValidEvent\""),
        _.endsWith(s" ${actors*commands}.0")
      )

      val refRecovered = (1 to actors).map(v =>
        system.actorOf(Props(classOf[MassPersistentProbe]), s"mass-persistent-$v")
      )

      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_active_actors_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" $actors.0")
        )
      }

      for (e <- commands + 1 to commands + 2) refs.foreach(_ ! ValidCommand(e.toString))

      eventually {
        assertMetrics(
          _.startsWith("akka_sensors_actor_recoveries_total{actor=\"MassPersistentProbe\""),
          _.endsWith(s" ${actors*2}.0")
        )
      }

      assertMetrics(
        _.startsWith("akka_sensors_actor_recovery_events_total{actor=\"MassPersistentProbe\","),
        _.endsWith(s" ${actors*commands}.0")
      )

    }
  }

  private def assertMetrics(filter: String => Boolean, assertion: String => Boolean) = {
    metrics
      .split("\n")
      .find(filter)
      .map(m =>
        assert(assertion(m), s"assertion failed for $m")
      ).getOrElse(
      fail("No metric found")
    )
  }

  private def metrics(implicit registry: CollectorRegistry): String = {
    val writer = new CharArrayWriter(16 * 1024)
    TextFormat.write004(writer, registry.metricFamilySamples)
    writer.toString
  }

  private def pingActor = {
    val r = Await.result(
      probeActor.ask(Ping("1"))(Timeout.durationToTimeout(10 seconds)), 15 seconds)
    assert(r.toString == "Pong(1)")
  }

  private def sendEventAck(actor: ActorRef) = {
    val r = Await.result(
      actor.ask(ValidCommand("1"))(Timeout.durationToTimeout(10 seconds)), 15 seconds)
    assert(r.toString == "Pong(1)")
  }
  override protected def afterAll(): Unit = {
    system.terminate()
  }
}

object InstrumentedActors {

  case class Ping(id: String) extends NoSerializationVerificationNeeded
  case object KnownError  extends NoSerializationVerificationNeeded
  case object UnknownMessage  extends NoSerializationVerificationNeeded
  case object BlockTooLong extends NoSerializationVerificationNeeded
  case class Pong(id: String) extends NoSerializationVerificationNeeded
  case class ValidCommand(id: String) extends NoSerializationVerificationNeeded
  case class ValidEvent(id: String) extends NoSerializationVerificationNeeded

  class MassProbe extends Actor with ActorMetrics {
    context.setReceiveTimeout(2 seconds)
    def receive: Receive = {
      case Ping(x) =>
        sender() ! Pong(x)
      case ReceiveTimeout =>
        context.stop(self)
    }
  }

  class MassPersistentProbe extends PersistentActor with PersistentActorMetrics {
    context.setReceiveTimeout(2 seconds)
    var counter = 0

    def receiveRecover: Receive = {
      case ValidEvent(x) =>
        counter = x.toInt
      case RecoveryCompleted =>
    }

    def receiveCommand: Receive = {
      case ValidCommand(x) =>
        persist(ValidEvent(x)) { _ =>
          counter +=1
        }

      case ReceiveTimeout =>
        context.stop(self)

    }

    def persistenceId: String = context.self.actorRef.path.name

  }

  class InstrumentedProbe extends Actor with ActorMetrics {
    def receive: Receive = {
            case Ping(x) =>
              Thread.sleep(Random.nextInt(3))
              sender() ! Pong(x)
            case KnownError =>
              throw new Exception("known")
            case BlockTooLong =>
              Thread.sleep(6000)
    }
  }

  class PersistentInstrumentedProbe extends PersistentActor with PersistentActorMetrics {
    var counter = 0

    def receiveRecover: Receive = {
      case _ =>
    }

    def receiveCommand: Receive = {
      case ValidCommand(x) =>
        val replyTo = sender()
        persist(ValidEvent(counter.toString)) { _ =>
          replyTo ! Pong(x)
          counter +=1
        }
      case x => println(x)
    }

    def persistenceId: String = context.self.actorRef.path.name
  }

  class AnotherPersistentInstrumentedProbe extends PersistentInstrumentedProbe

}