import sbt.addSbtPlugin
addSbtPlugin("com.github.sbt"    % "sbt-git"              % "2.0.0")
addSbtPlugin("dev.guardrail"     % "sbt-guardrail"        % "0.75.0")
addSbtPlugin("org.wartremover"   % "sbt-wartremover"      % "3.0.6")
addSbtPlugin("net.vonbuchholtz"  % "sbt-dependency-check" % "4.1.0")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager"  % "1.9.11")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"          % "0.6.3")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.4.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"         % "3.9.16")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"              % "2.1.1")
addSbtPlugin("no.arktekk.sbt"    % "aether-deploy"        % "0.27.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"          % "1.0.13")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"        % "2.0.4")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
