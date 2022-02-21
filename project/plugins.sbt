import sbt.addSbtPlugin
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"              % "1.0.2")
addSbtPlugin("com.twilio"        % "sbt-guardrail"        % "0.65.0")
addSbtPlugin("org.wartremover"   % "sbt-wartremover"      % "2.4.18")
addSbtPlugin("net.vonbuchholtz"  % "sbt-dependency-check" % "3.4.0")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager"  % "1.9.8")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"          % "0.6.2")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.4.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"         % "3.9.12")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"              % "2.1.1") // 2.1.2 is released but unavailable??
addSbtPlugin("no.arktekk.sbt"    % "aether-deploy"        % "0.27.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"          % "1.0.13")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"        % "1.9.3")
