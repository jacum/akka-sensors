package akka.sensors.cassandra

import java.io.CharArrayWriter
import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.{ActorLogging, ActorSystem, NoSerializationVerificationNeeded, Props}
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.QueryBuilder._
import com.datastax.oss.driver.api.querybuilder.term.Term
import com.typesafe.scalalogging.LazyLogging
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.jmx.JmxCollector
import org.cassandraunit.utils.EmbeddedCassandraServerHelper._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Millis, Seconds, Span}

import scala.Console.println
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.jdk.CollectionConverters._

class AkkaCassandraJmxMetricsSpec extends AnyFreeSpec with LazyLogging with Eventually with BeforeAndAfterAll {

  implicit val prometheusRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
  DefaultExports.register(prometheusRegistry)
  prometheusRegistry.register(new JmxCollector(Source.fromResource("prometheus-jmx-collector.yaml").mkString))

  import TestActors._
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

  lazy val cassandra = {
    try startEmbeddedCassandra("cassandra-server.yaml")
    catch {
      case e: Exception =>
    }
  }
  implicit val ec: ExecutionContext    = ExecutionContext.global
  private lazy val system: ActorSystem = ActorSystem("instrumented")
  private val persistenceInitActor     = system.actorOf(Props(classOf[AwaitPersistenceInit]), s"persistenceInit-${UUID.randomUUID().toString}")

  def createSession =
    CqlSession
      .builder()
      .withLocalDatacenter("datacenter1")
      .addContactPoints(List(InetSocketAddress.createUnresolved(getHost, getNativeTransportPort)).asJavaCollection)
      .build()

  // todo embedded cassandra is broken for JDK >8
  "Launch cassandra and akka app, and ensure it works" ignore {

    "starts actor system and pings the bootstrap actor" in {
      pingActor
    }

    "after some activity exports some jmx metrics" in {
      val session = createSession
      session.execute("create table akka.test (id int, a text, b text, primary key(id))")
      for (i <- 1 to 100)
        session
          .execute(
            insertInto("akka", "test")
              .values(Map[String, Term]("id" -> literal(i), "a" -> literal(s"A${i}"), "b" -> literal(s"B$i")).asJava)
              .build()
          )

      import java.lang.management.ManagementFactory
      val mbs = ManagementFactory.getPlatformMBeanServer
      mbs.queryMBeans(null, null).asScala.map(_.getObjectName).filter(_.toString.startsWith("akka")).foreach(println)
      val datastaxMbeans = mbs.queryMBeans(null, null).asScala.filter(_.getObjectName.getDomain == "com.datastax.oss.driver")
      assert(datastaxMbeans.nonEmpty)
    }

    "ensure prometheus JMX scraping is working" in {

      for (_ <- 1 to 100000)
        pingActor

      val content = metrics.split("\n")
      List("cassandra_cql_requests", "cassandra_cql_client_timeout", "cassandra_bytes").foreach(s => assert(content.exists(_.startsWith(s)), s"starts with $s"))
    }

  }

  private def metrics(implicit registry: CollectorRegistry) = {
    val writer = new CharArrayWriter(16 * 1024)
    TextFormat.write004(writer, registry.metricFamilySamples)
    writer.toString
  }

  private def pingActor = {
    val r = Await.result(persistenceInitActor.ask(Ping)(Timeout.durationToTimeout(30 seconds)), 40 seconds)
    assert(r.toString == "Pong")
  }
}

object TestActors {

  case object Ping extends NoSerializationVerificationNeeded

  case object Pong extends NoSerializationVerificationNeeded

  class AwaitPersistenceInit extends PersistentActor with ActorLogging {

    override val persistenceId: String = s"persistenceInit-${UUID.randomUUID()}"

    log.info("Starting PersistenceInit actor with id: {}", persistenceId)

    // intentionally left empty
    def receiveRecover: Receive = Map.empty

    // intentionally left empty
    def receiveCommand: Receive = {
      case Ping =>
        sender() ! Pong
    }
  }
}
