import Dependencies._
import Keys._
import sbt.file

val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "nl.pragmasoft",
  crossScalaVersions := Seq("2.13.3", "2.12.12"),
  scalaVersion := crossScalaVersions.value.head,
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
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
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven")
)

lazy val publishSettings = Seq(
  homepage := Some(new URL("http://github.com/jacum/akka-sensors")),
  startYear := Some(2020),
  organizationName := "PragmaSoft",
  organizationHomepage := Some(url("http://pragmasoft.nl")),
  organization := "nl.pragmasoft",
  licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html"))
)

lazy val `sensors-core` = project.in(file("sensors-core"))
  .settings(commonSettings ++ publishSettings)
  .settings(
    moduleName := "core-sensors",
    libraryDependencies ++= Akka.deps ++ Prometheus.deps ++ Logging.deps ++ TestTools.deps,
    dependencyOverrides ++= Akka.deps
  )

lazy val `example-app` = project.in(file("example-app"))
  .settings(commonSettings)
  .settings(
    moduleName := "example-app",
    libraryDependencies ++= App.deps,
    dependencyOverrides ++= Akka.deps
  ).dependsOn(`sensors-core`)
