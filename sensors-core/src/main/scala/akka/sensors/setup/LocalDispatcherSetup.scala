package akka.sensors.setup

import akka.actor.setup.Setup
import akka.dispatch.DispatcherPrerequisites
import akka.sensors.DispatcherMetrics

final case class LocalDispatcherSetup(metrics: DispatcherMetrics) extends Setup

object LocalDispatcherSetup {
  def setupOrThrow(prereq: DispatcherPrerequisites): LocalDispatcherSetup =
    prereq.settings.setup
      .get[LocalDispatcherSetup]
      .getOrElse(throw SetupNotFound[LocalDispatcherSetup])
}
