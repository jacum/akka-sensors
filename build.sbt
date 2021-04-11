import Dependencies._
import Keys._
import sbt.file

val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "nl.pragmasoft.sensors",
  crossScalaVersions := Seq("2.13.5", "2.12.13"),
  scalaVersion :=  "2.13.5",
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
  parallelExecution in Test := false,
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
  packageOptions in(Compile, packageBin) +=
    Package.ManifestAttributes(
      "Build-Time" -> new java.util.Date().toString,
      "Build-Commit" -> git.gitHeadCommit.value.getOrElse("No Git Revision Found")
    ),
  sources in doc := Seq.empty,
  publishArtifact in packageDoc := false,
  publishArtifact in packageDoc := false,
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven")
) ++ Publish.settings

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(new URL("http://github.com/jacum/akka-sensors")),
  startYear := Some(2020),
  organizationName := "PragmaSoft",
  organizationHomepage := Some(url("http://pragmasoft.nl")),
  organization := "nl.pragmasoft",
  licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html"))
) ++ Publish.settings

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
    mainClass in Compile := Some("nl.pragmasoft.app.Main"),
    version in Docker := Keys.version.value,
    dockerUpdateLatest := true,
    libraryDependencies ++= App.deps :+ Cassandra.cassandraUnit,
    dependencyOverrides ++= Akka.deps
  ).dependsOn(`sensors-core`,`sensors-cassandra`)

lazy val `root` =  project.in(file("."))
  .aggregate(app, `sensors-core`, `sensors-cassandra`)
  .settings(commonSettings ++ noPublishSettings)
  .settings(name := "Akka Sensors")
