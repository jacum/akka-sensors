package akka.sensors.setup

import scala.reflect.ClassTag

object SetupNotFound {
  private final case class SetupNotFound(msg: String) extends RuntimeException(msg)
  private def errorMsg[T: ClassTag]: String = {
    val className = implicitly[ClassTag[T]].runtimeClass.getName
    s"Can't find dispatcher setup for '$className'"
  }
  def apply[T: ClassTag]: Exception = new SetupNotFound(errorMsg[T])
}
