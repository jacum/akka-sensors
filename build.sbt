import Dependencies._
import Keys._
import sbt.file

val scala2 = "2.13.16"
ThisBuild / versionScheme := Some("early-semver")

val commonSettings = Defaults.coreDefaultSettings ++ Seq(
        organization := "nl.pragmasoft.sensors",
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

lazy val `sensors-core` = project
  .in(file("sensors-core"))
  .settings(commonSettings)
  .settings(
    moduleName := "sensors-core",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )

lazy val `sensors-cassandra` = project
  .in(file("sensors-cassandra"))
  .settings(commonSettings)
  .settings(
    moduleName := "sensors-cassandra",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++
            (Cassandra.deps :+ Cassandra.cassandraUnit % Test) ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )
  .dependsOn(`sensors-core`)

lazy val `app` = project
  .in(file("examples/app"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(commonSettings ++ noPublishSettings)
  .settings(
    moduleName := "app",
    Compile / mainClass := Some("nl.pragmasoft.app.Main"),
    Docker / version := Keys.version.value,
    dockerUpdateLatest := true,
    libraryDependencies ++= App.deps ++ Logging.deps :+ Cassandra.cassandraUnit,
    dependencyOverrides ++= Akka.deps
  )
  .dependsOn(`sensors-core`, `sensors-cassandra`)

lazy val `root` = project
  .in(file("."))
  .aggregate(app, `sensors-core`, `sensors-cassandra`)
  .settings(commonSettings ++ noPublishSettings)
  .settings(name := "Akka Sensors")
