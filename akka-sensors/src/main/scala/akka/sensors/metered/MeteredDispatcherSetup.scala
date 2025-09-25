package akka.sensors.metered

import akka.actor.setup.Setup
import akka.dispatch.DispatcherPrerequisites
import akka.sensors.DispatcherMetrics

final case class MeteredDispatcherSetup(metrics: DispatcherMetrics) extends Setup

object MeteredDispatcherSetup {

  /** Extract LocalDispatcherSetup out from DispatcherPrerequisites or throw an exception */
  def setupOrThrow(prereq: DispatcherPrerequisites): MeteredDispatcherSetup =
    prereq.settings.setup
      .get[MeteredDispatcherSetup]
      .getOrElse(throw SetupNotFound[MeteredDispatcherSetup])
}
