package nl.pragmasoft

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.cluster.Cluster
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.http4s.server.Server

import scala.concurrent.{ExecutionContext, Future}

object Main extends IOApp with LazyLogging {
//  val cassandra = startEmbeddedCassandra("cassandra-server.yaml")

  override def run(args: List[String]): IO[ExitCode] = {
    val config                                      = ConfigFactory.load()
    implicit val system: ActorSystem                = ActorSystem("app", config)
    implicit val executionContext: ExecutionContext = system.dispatcher

    val mainResource: Resource[IO, Server[IO]] =
      for {
        _ <- Resource.liftF(IO.async[Unit] { callback =>
          Cluster(system).registerOnMemberUp {
            logger.info("Akka cluster is now up")
            callback(Right(()))
          }
        })
        _ <- MetricService.resource(
          InetSocketAddress.createUnresolved("0.0.0.0", 8081)
        )
        apiService <- ApiService.resource(
          InetSocketAddress.createUnresolved("0.0.0.0", 8080),
          system
        )
      } yield apiService

    mainResource.use { s =>
      logger.info(s"App started at ${s.address}/${s.baseUri}, enabling the readiness in Akka management")
      ReadinessCheck.enable()
      IO.never
    }
      .as(ExitCode.Success)
  }
}

object ReadinessCheck {
  var ready: Boolean = false
  def enable(): Unit = ReadinessCheck.ready = true
}

class ReadinessCheck extends (() => Future[Boolean]) {
  override def apply(): Future[Boolean] = Future.successful(ReadinessCheck.ready)
}
