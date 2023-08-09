package akka.sensors.setup

import akka.dispatch.{Batchable, Mailbox}
import akka.sensors.setup.DispatcherInstrumentationWrapper.Run

import java.util.concurrent.ForkJoinTask
import scala.PartialFunction.condOpt

private[setup] object AkkaRunnableWrapper {
  def unapply(runnable: Runnable): Option[Run => Runnable] =
    condOpt(runnable) {
      case runnable: Batchable => new BatchableWrapper(runnable, _)
      case runnable: Mailbox   => new MailboxWrapper(runnable, _)
    }
}

private[setup] class MailboxWrapper(self: Mailbox, r: Run) extends ForkJoinTask[Unit] with Runnable {
  def getRawResult: Unit = self.getRawResult()

  def setRawResult(v: Unit): Unit = self.setRawResult(v)

  def exec(): Boolean = r(() => self.exec())

  def run(): Unit = {
    exec(); ()
  }
}

private[setup] class BatchableWrapper(self: Batchable, r: Run) extends Batchable {
  def run(): Unit = r(() => self.run())

  def isBatchable: Boolean = self.isBatchable
}
