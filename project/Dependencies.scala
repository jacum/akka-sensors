import sbt._

//noinspection TypeAnnotation
object Dependencies {

  val akkaInmemoryJournal = ("com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2")
    .exclude("com.typesafe.akka", "akka-actor")
    .exclude("com.typesafe.akka", "akka-persistence")
    .exclude("com.typesafe.akka", "akka-persistence-query")
    .exclude("com.typesafe.akka", "akka-stream")
    .exclude("com.typesafe.akka", "akka-protobuf")

  object Logging {
    val logbackVersion = "1.2.3"
    val slf4jversion = "1.7.30"
    val log = "ch.qos.logback" % "logback-classic" % logbackVersion
//    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jversion
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val deps = Seq(log, scalaLogging)
  }

  object Akka {
    val akkaVersion = "2.6.10"
    val akkaManagementVersion = "1.0.9"
    val akkaPersistenceCassandraVersion = "1.0.3"
    val akkaHttpVersion = "10.2.1"


    val actor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
    val persistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
    val persistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion
    val cluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

    val deps = Seq(actor, persistence, persistenceQuery, persistenceCassandra, cluster, clusterTools, slf4j) ++ Logging.deps
  }

  object Prometheus {
    val hotspot = "io.prometheus" % "simpleclient_hotspot" % "0.9.0"
    val common = "io.prometheus" % "simpleclient_common" % "0.9.0"
    val jmx = "io.prometheus.jmx" % "collector" % "0.14.0"

    val deps = Seq(hotspot, common, jmx)
  }

  object App {
    val http4sVersion = "0.21.8"
    val circeVersion = "0.13.0"
    val http4s = "org.http4s" %% "http4s-core" % http4sVersion
    val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
    val http4sServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
    val http4sClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
    val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion
    val http4sPrometheus = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion
    val circe = "io.circe" %% "circe-core" % circeVersion
    val circeParser = "io.circe" %% "circe-parser" % circeVersion
    val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion

    val deps = Seq(http4s, http4sDsl, http4sServer, http4sClient, http4sCirce, http4sPrometheus,
      circe, circeParser, circeGeneric, circeGenericExtras) ++ Akka.deps ++ Logging.deps
  }

  object TestTools {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
    val deps = Logging.deps ++ testDeps(scalaTest, akkaInmemoryJournal)
  }

  def scopeDeps(scope: String, modules: Seq[ModuleID]) = modules.map(m => m % scope)
  def testDeps(modules: ModuleID*) = scopeDeps("test", modules)

}
