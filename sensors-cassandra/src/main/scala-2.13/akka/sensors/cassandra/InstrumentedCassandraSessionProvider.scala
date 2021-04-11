package akka.sensors.cassandra

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.alpakka.cassandra.{CqlSessionProvider, DefaultSessionProvider, DriverConfigLoaderFromConfig}
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jmx.JmxReporter
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.metadata.{Node, NodeStateListener}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._


class InstrumentedCassandraSessionProvider(system: ActorSystem, config: Config) extends DefaultSessionProvider(system, config) with LazyLogging {

  private val instanceId = UUID.randomUUID()

  private val metricRegistry = {
    val registry = new MetricRegistry()
    JmxReporter
      .forRegistry(registry)
      .inDomain("com.datastax.oss.driver")
      .build
      .start()
    logger.info("JMX exporter started")
    registry
  }

  override def connect()(implicit ec: ExecutionContext): Future[CqlSession] = {

    val nodeStateListener: NodeStateListener = new NodeStateListener {
      private def info(node: Node) = s"(host id: ${node.getHostId}, address ${node.getEndPoint.resolve()}, dc ${node.getDatacenter}, cassandra version ${node.getCassandraVersion})"

      override def onAdd(node: Node): Unit = logger.info(s"Node added ${info(node)}")

      override def onUp(node: Node): Unit = logger.info(s"Node up ${info(node)}")

      override def onDown(node: Node): Unit = logger.info(s"Node down ${info(node)}")

      override def onRemove(node: Node): Unit = logger.info(s"Node remove ${info(node)}")

      override def close(): Unit = logger.info(s"Listener closed")
    }

    val driverConfig       = CqlSessionProvider.driverConfig(system, config)
    val driverConfigLoader = DriverConfigLoaderFromConfig.fromConfig(driverConfig)
    logger.info("Creating new Cassandra connection")
    toScala(
      CqlSession
        .builder()
        .withMetricRegistry(metricRegistry)
        .withConfigLoader(driverConfigLoader)
        .withClientId(instanceId)
        .withNodeStateListener(nodeStateListener)
        .buildAsync())
  }
}
