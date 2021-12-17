import Dependencies._
import Keys._
import sbt.file

val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "nl.pragmasoft.sensors",
  crossScalaVersions := Seq("2.13.7", "2.12.15"),
  scalaVersion :=  "2.13.6",
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
  Test / parallelExecution := false,
  Test / fork := true,
  javacOptions := Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions := Seq(
    s"-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-encoding", "utf8",
    "-Xfatal-warnings"
  ),
  Compile / packageBin / packageOptions
    +=
    Package.ManifestAttributes(
      "Build-Time" -> new java.util.Date().toString,
      "Build-Commit" -> git.gitHeadCommit.value.getOrElse("No Git Revision Found")
    ),
  doc / sources := Seq.empty,
  packageSrc / publishArtifact  := false,
  packageDoc / publishArtifact  := true,
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven")
) ++ Publish.settings

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val `sensors-core` = project.in(file("sensors-core"))
  .settings(commonSettings)
  .settings(
    moduleName := "sensors-core",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )

lazy val `sensors-cassandra` = project.in(file("sensors-cassandra"))
  .settings(commonSettings)
  .settings(
    moduleName := "sensors-cassandra",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++
      (Cassandra.deps :+ Cassandra.cassandraUnit % Test) ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )
  .dependsOn(`sensors-core`)

lazy val `app` = project.in(file("examples/app"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(commonSettings ++ noPublishSettings)
  .settings(
    moduleName := "app",
    Compile / mainClass  := Some("nl.pragmasoft.app.Main"),
    Docker / version  := Keys.version.value,
    dockerUpdateLatest := true,
    libraryDependencies ++= App.deps :+ Cassandra.cassandraUnit,
    dependencyOverrides ++= Akka.deps
  ).dependsOn(`sensors-core`,`sensors-cassandra`)

lazy val `root` =  project.in(file("."))
  .aggregate(app, `sensors-core`, `sensors-cassandra`)
  .settings(commonSettings ++ noPublishSettings)
  .settings(name := "Akka Sensors")
