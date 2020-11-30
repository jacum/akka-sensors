package akka.dispatch

import akka.sensors.dispatch.DispatcherInstrumentationWrapper.Run

import scala.PartialFunction.condOpt
import scala.concurrent.OnCompleteRunnable

object ScalaRunnableWrapper {
  def unapply(runnable: Runnable): Option[Run => Runnable] =
    condOpt(runnable) {
      case runnable: OnCompleteRunnable => new OverrideOnComplete(runnable, _)
    }

  class OverrideOnComplete(self: Runnable, r: Run) extends OnCompleteRunnable with Runnable {
    def run(): Unit = r(() => self.run())
  }
}
