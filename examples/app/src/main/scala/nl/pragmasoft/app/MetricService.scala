package nl.pragmasoft.app

import akka.sensors.AkkaSensors

import java.io.CharArrayWriter
import java.net.InetSocketAddress
import cats.effect.{ContextShift, IO, Resource, Timer}
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot._
import io.prometheus.client.{Collector, CollectorRegistry}
import io.prometheus.jmx.JmxCollector
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, _}

import scala.concurrent.ExecutionContext
import scala.io.Source

object MetricService extends LazyLogging {

  val registry: CollectorRegistry = AkkaSensors.prometheusRegistry
  DefaultExports.register(registry)

  try registry.register(new JmxCollector(Source.fromResource("prometheus-jmx-collector.yaml").mkString))
  catch {
    case e: Exception =>
      logger.info(s"No prometheus-jmx-collector.yaml found in classpath, JMX export is not enabled: ${e.getMessage}")
  }

  def register(collector: Collector): Unit = registry.register(collector)

  def resource(socketAddress: InetSocketAddress)(implicit cs: ContextShift[IO], timer: Timer[IO], ec: ExecutionContext): Resource[IO, Server[IO]] = {
    val encoder = EntityEncoder.stringEncoder

    def exportMetrics: String = {
      val writer = new CharArrayWriter(16 * 1024)
      TextFormat.write004(writer, registry.metricFamilySamples)
      writer.toString
    }

    for {
      server <- BlazeServerBuilder[IO](ec)
        .bindSocketAddress(socketAddress)
        .withHttpApp(
          Router(
            "/metrics" ->
                HttpRoutes.of[IO] {
                  case GET -> Root =>
                    IO(Response(status = Ok, body = encoder.toEntity(exportMetrics).body, headers = Headers.of(Header("Content-Type", TextFormat.CONTENT_TYPE_004))))
                }
          ) orNotFound
        )
        .resource
    } yield server
  }

}
