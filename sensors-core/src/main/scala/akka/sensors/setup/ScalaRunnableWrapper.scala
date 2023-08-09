package akka.sensors.setup

import akka.dispatch.Batchable
import akka.sensors.setup.DispatcherInstrumentationWrapper.Run

import scala.PartialFunction.condOpt

private[setup] object ScalaRunnableWrapper {
  def unapply(runnable: Runnable): Option[Run => Runnable] =
    condOpt(runnable) {
      case runnable: Batchable => new OverrideBatchable(runnable, _)
    }
}

private[setup] class OverrideBatchable(self: Runnable, r: Run) extends Batchable with Runnable {
  def run(): Unit = r(() => self.run())

  def isBatchable: Boolean = true
}
