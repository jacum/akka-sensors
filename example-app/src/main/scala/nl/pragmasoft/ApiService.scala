package nl.pragmasoft

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, NoSerializationVerificationNeeded, Props}
import akka.sensors.actor.ActorMetrics
import cats.effect.{ContextShift, IO, Resource, Timer}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}

import scala.concurrent.ExecutionContext
import scala.util.Random
import ResponderActor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

object ApiService extends LazyLogging {

  def resource(socketAddress: InetSocketAddress, system: ActorSystem)(implicit cs: ContextShift[IO], timer: Timer[IO], ec: ExecutionContext): Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO](ec)
      .bindSocketAddress(socketAddress)
      .withHttpApp(
        Router("/api" -> HttpRoutes.of[IO] {
              case GET -> Root / "health" => Ok()

              case POST -> Root / "event" / actorId =>
                val actor = system.actorOf(Props(classOf[ResponderActor]), s"responder-$actorId")
                val actorResponse = actor.ask(Ping)(Timeout.durationToTimeout(10 seconds))
                IO.fromFuture(IO(actorResponse)).attempt.flatMap {
                  case Left(e: Exception) => InternalServerError(e.getMessage)
                  case _ => Ok()
                }

            }) orNotFound
      )
      .resource
}

object ResponderActor {

  case object Ping extends NoSerializationVerificationNeeded
  case object KnownError  extends NoSerializationVerificationNeeded
  case object UnknownMessage  extends NoSerializationVerificationNeeded
  case object BlockTooLong extends NoSerializationVerificationNeeded
  case object Pong extends NoSerializationVerificationNeeded
  case object ValidCommand extends NoSerializationVerificationNeeded
  case class ValidEvent(id: String) extends NoSerializationVerificationNeeded

}


class ResponderActor extends Actor with ActorMetrics {

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