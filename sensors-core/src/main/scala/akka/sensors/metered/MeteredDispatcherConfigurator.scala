package akka.sensors.metered

import akka.dispatch.{Dispatcher, DispatcherPrerequisites, MessageDispatcher, MessageDispatcherConfigurator}
import akka.sensors.DispatcherMetrics
import akka.sensors.dispatch.Helpers
import com.typesafe.config.Config

class MeteredDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {
  import Helpers._

  private val instance: MessageDispatcher = {
    val _metrics = MeteredDispatcherSetup.setupOrThrow(prerequisites).metrics
    val settings = MeteredDispatcherSettings(
      name = prerequisites.mailboxes.settings.name,
      metrics = _metrics,
      _configurator = this,
      id = config.getString("id"),
      throughput = config.getInt("throughput"),
      throughputDeadlineTime = config.getNanosDuration("throughput-deadline-time"),
      executorServiceFactoryProvider = configureExecutor(),
      shutdownTimeout = config.getMillisDuration("shutdown-timeout")
    )

    new MeteredDispatcher(settings)
  }

  def dispatcher(): MessageDispatcher = instance
}
