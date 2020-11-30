addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.26.0")
addSbtPlugin("com.twilio" % "sbt-guardrail" % "0.60.0")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.10")
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "1.3.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.3")
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.26.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.30"