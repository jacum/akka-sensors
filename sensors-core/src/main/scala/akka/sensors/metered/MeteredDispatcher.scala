package akka.sensors.metered

import akka.dispatch.Dispatcher
import akka.sensors.DispatcherMetrics

private[metered] class MeteredDispatcher(settings: MeteredDispatcherSettings)
    extends Dispatcher(
      settings._configurator,
      settings.id,
      settings.throughput,
      settings.throughputDeadlineTime,
      executorServiceFactoryProvider = settings.executorServiceFactoryProvider,
      shutdownTimeout = settings.shutdownTimeout
    )
    with MeteredDispatcherInstrumentation {
  protected override val actorSystemName: String    = settings.name
  protected override val metrics: DispatcherMetrics = settings.metrics
}
