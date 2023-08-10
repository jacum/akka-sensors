package akka.sensors.metered

import akka.dispatch.MessageDispatcherConfigurator

import scala.concurrent.duration._
import akka.dispatch.ExecutorServiceFactoryProvider
import akka.sensors.DispatcherMetrics

private[metered] case class MeteredDispatcherSettings(
  name: String,
  metrics: DispatcherMetrics,
  _configurator: MessageDispatcherConfigurator,
  id: String,
  throughput: Int,
  throughputDeadlineTime: Duration,
  executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
  shutdownTimeout: FiniteDuration
)
