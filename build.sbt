import Dependencies._
import Keys._
import sbt.file

val scala2 = "2.13.18"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / libraryDependencySchemes += "org.http4s" % "*" % VersionScheme.Always

val commonSettings = Defaults.coreDefaultSettings ++ Seq(
        organization := "nl.pragmasoft",
        scalaVersion := scala2,
        testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
        Test / parallelExecution := false,
        Test / fork := true,
        scalacOptions := Seq(
              s"-unchecked",
              "-deprecation",
              "-feature",
              "-language:higherKinds",
              "-language:existentials",
              "-language:implicitConversions",
              "-language:postfixOps",
              "-encoding",
              "utf8",
              "-Xfatal-warnings"
            ),
        Compile / packageBin / packageOptions +=
            Package.ManifestAttributes(
              "Build-Time"   -> new java.util.Date().toString,
              "Build-Commit" -> git.gitHeadCommit.value.getOrElse("No Git Revision Found")
            ),
        doc / sources := Seq.empty,
        packageSrc / publishArtifact := false,
        packageDoc / publishArtifact := true
      ) ++ Publish.settings

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val `akka-sensors` = project
  .in(file("akka-sensors"))
  .settings(commonSettings)
  .settings(
    moduleName := "akka-sensors",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )

lazy val `root` = project
  .in(file("."))
  .aggregate(`akka-sensors`)
  .settings(commonSettings ++ noPublishSettings)
  .settings(name := "Akka Sensors")
