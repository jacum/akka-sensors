import sbt.addSbtPlugin
addSbtPlugin("com.github.sbt"   % "sbt-git"              % "2.0.1")
addSbtPlugin("dev.guardrail"    % "sbt-guardrail"        % "0.75.2")
addSbtPlugin("org.wartremover"  % "sbt-wartremover"      % "3.1.8")
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")
addSbtPlugin("com.github.sbt"   % "sbt-native-packager"  % "1.10.4")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"          % "0.6.4")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"         % "2.5.4")
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"         % "3.11.2")
addSbtPlugin("com.jsuereth"     % "sbt-pgp"              % "2.1.1")
addSbtPlugin("no.arktekk.sbt"   % "aether-deploy"        % "0.29.1")
addSbtPlugin("com.github.sbt"   % "sbt-release"          % "1.4.0")
addSbtPlugin("org.scoverage"    % "sbt-scoverage"        % "2.0.12")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "com.lihaoyi"            %% "geny"      % VersionScheme.Always
)
