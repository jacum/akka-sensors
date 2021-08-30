import sbt.addSbtPlugin
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.1")
addSbtPlugin("com.twilio" % "sbt-guardrail" % "0.64.5")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.16")
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "3.1.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.10")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.1.1") // 2.1.2 is released but unavailable??
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.27.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")
