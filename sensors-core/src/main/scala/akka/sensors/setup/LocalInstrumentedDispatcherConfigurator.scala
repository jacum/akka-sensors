package akka.sensors.setup

import akka.dispatch.{Dispatcher, DispatcherPrerequisites, MessageDispatcher, MessageDispatcherConfigurator}
import akka.sensors.DispatcherMetrics
import akka.sensors.dispatch.Helpers
import com.typesafe.config.Config

class LocalInstrumentedDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {
  import Helpers._

  private val instance = {
    val _metrics = LocalDispatcherSetup.setupOrThrow(prerequisites).metrics

    new Dispatcher(
      this,
      config.getString("id"),
      config.getInt("throughput"),
      config.getNanosDuration("throughput-deadline-time"),
      configureExecutor(),
      config.getMillisDuration("shutdown-timeout")
    ) with LocalInstrumentedDispatcher {
      def actorSystemName: String                       = prerequisites.mailboxes.settings.name
      protected override def metrics: DispatcherMetrics = _metrics
    }
  }

  def dispatcher(): MessageDispatcher = instance

}
