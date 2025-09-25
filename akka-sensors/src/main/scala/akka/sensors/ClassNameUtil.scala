package akka.sensors

object ClassNameUtil {

  /**
   * Safer than Class obj's getSimpleName which may throw Malformed class name error in scala.
   * This method mimics scalatest's getSimpleNameOfAnObjectsClass.
   */
  def simpleName(cls: Class[_]): String =
    try stripDollars(cls.getSimpleName)
    catch {
      // TODO: the value returned here isn't even quite right; it returns simple names
      // like UtilsSuite$MalformedClassObject$MalformedClass instead of MalformedClass
      // The exact value may not matter much as it's used in log statements
      case _: InternalError =>
        stripDollars(stripPackages(cls.getName))
    }

  /**
   * Remove the packages from full qualified class name
   */
  private def stripPackages(fullyQualifiedName: String): String =
    fullyQualifiedName.split("\\.").takeRight(1)(0)

  /**
   * Remove trailing dollar signs from qualified class name,
   * and return the trailing part after the last dollar sign in the middle
   */
  private def stripDollars(s: String): String = {
    val lastDollarIndex = s.lastIndexOf('$')
    if (lastDollarIndex < s.length - 1)
      // The last char is not a dollar sign
      if (lastDollarIndex == -1 || !s.contains("$iw"))
        // The name does not have dollar sign or is not an interpreter
        // generated class, so we should return the full string
        s
      else
        // The class name is interpreter generated,
        // return the part after the last dollar sign
        // This is the same behavior as getClass.getSimpleName
        s.substring(lastDollarIndex + 1)
    else {
      // The last char is a dollar sign
      // Find last non-dollar char
      val lastNonDollarChar = s.reverse.find(_ != '$')
      lastNonDollarChar match {
        case None => s
        case Some(c) =>
          val lastNonDollarIndex = s.lastIndexOf(c)
          if (lastNonDollarIndex == -1)
            s
          else
            // Strip the trailing dollar signs
            // Invoke stripDollars again to get the simple name
            stripDollars(s.substring(0, lastNonDollarIndex + 1))
      }
    }
  }
}
