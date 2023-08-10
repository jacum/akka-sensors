package akka.sensors.metered

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

trait SetupNotFound extends NoStackTrace

object SetupNotFound {
  private final case class Impl(msg: String) extends RuntimeException(msg) with SetupNotFound
  private def errorMsg[T: ClassTag]: String = {
    val className = implicitly[ClassTag[T]].runtimeClass.getName
    s"Can't find dispatcher setup for '$className'." +
      s" Please check if you have `$className` defined for your ActorSystem." +
      s" Check ActorSystemSetup for more info."
  }
  def apply[T: ClassTag]: SetupNotFound = Impl(errorMsg[T])
}
