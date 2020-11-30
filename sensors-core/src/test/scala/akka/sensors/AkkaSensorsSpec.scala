package akka.sensors

import java.io.CharArrayWriter

import akka.actor.{Actor, ActorSystem, NoSerializationVerificationNeeded, PoisonPill, Props}
import akka.pattern.ask
import akka.sensors.actor.{ActorMetrics, PersistentActorMetrics}
import akka.persistence.PersistentActor
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
  private val probeActor = system.actorOf(Props(classOf[InstrumentedProbe]), s"probe")
  private val persistentActor = system.actorOf(Props(classOf[PersistentInstrumentedProbe]), s"persistent")
  implicit val registry: CollectorRegistry = CollectorRegistry.defaultRegistry

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

      for (_ <- 1 to 100) sendEventAck

      persistentActor ! PoisonPill

      Thread.sleep(100) // todo better condition?

      val blockingIo = system.dispatchers.lookup("akka.actor.default-blocking-io-dispatcher")
      blockingIo.execute(() => { Thread.sleep(100)})

      println(metrics)

      // todo assertions per feature
//      assert(content.split("\n").exists(_.startsWith("cassandra_cql")))
    }
  }

  def metrics(implicit registry: CollectorRegistry) = {
    val writer = new CharArrayWriter(16 * 1024)
    TextFormat.write004(writer, registry.metricFamilySamples)
    writer.toString
  }

  private def pingActor = {
    val r = Await.result(
      probeActor.ask(Ping)(Timeout.durationToTimeout(10 seconds)), 15 seconds)
    assert(r.toString == "Pong")
  }

  private def sendEventAck = {
    val r = Await.result(
      persistentActor.ask(ValidCommand)(Timeout.durationToTimeout(10 seconds)), 15 seconds)
    assert(r.toString == "Pong")
  }
  override protected def afterAll(): Unit = {
    system.terminate()
  }
}

object InstrumentedActors {

  case object Ping extends NoSerializationVerificationNeeded
  case object KnownError  extends NoSerializationVerificationNeeded
  case object UnknownMessage  extends NoSerializationVerificationNeeded
  case object BlockTooLong extends NoSerializationVerificationNeeded
  case object Pong extends NoSerializationVerificationNeeded
  case object ValidCommand extends NoSerializationVerificationNeeded
  case class ValidEvent(id: String) extends NoSerializationVerificationNeeded

  class InstrumentedProbe extends Actor with ActorMetrics {
    def receive: Receive = {
            case Ping =>
              Thread.sleep(Random.nextInt(3))
              sender() ! Pong
            case KnownError =>
              throw new Exception("known")
            case BlockTooLong =>
              Thread.sleep(6000)
    }
  }

  class PersistentInstrumentedProbe extends PersistentActor with PersistentActorMetrics {
    var counter = 0

    def receiveCommand: Receive = {
      case ValidCommand =>
        val replyTo = sender()
        persist(ValidEvent(counter.toString)) { _ =>
          counter +=1
          replyTo ! Pong
        }
      case x => println(x)
    }

    def persistenceId: String = context.self.actorRef.path.name
  }


}