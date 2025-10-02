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
    val slf4jversion = "2.0.17"
    val slf4jApi     = "org.slf4j"                   % "slf4j-api"     % slf4jversion
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6"
    val deps         = Seq(slf4jApi, scalaLogging)
  }

  object Akka {
    val akkaVersion                     = "2.6.21"
    val akkaManagementVersion           = "1.0.9"
    val akkaPersistenceCassandraVersion = "1.0.5"
    val akkaHttpVersion                 = "10.2.1"

    val actor            = "com.typesafe.akka" %% "akka-actor"             % akkaVersion
    val typed            = "com.typesafe.akka" %% "akka-actor-typed"       % akkaVersion
    val persistence      = "com.typesafe.akka" %% "akka-persistence"       % akkaVersion
    val persistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion

    val cluster      = "com.typesafe.akka" %% "akka-cluster"       % akkaVersion
    val clusterTyped = "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
    val slf4j        = "com.typesafe.akka" %% "akka-slf4j"         % akkaVersion

    val deps = Seq(actor, typed, persistence, persistenceTyped, persistenceQuery, cluster, clusterTyped, clusterTools, slf4j) ++ Logging.deps
  }

  object Prometheus {
    val hotspot   = "io.prometheus"     % "simpleclient_hotspot" % "0.16.0"
    val common    = "io.prometheus"     % "simpleclient_common"  % "0.16.0"
    val jmx       = "io.prometheus.jmx" % "collector"            % "0.20.0" exclude ("org.yaml", "snakeyaml")
    val snakeYaml = "org.yaml"          % "snakeyaml"            % "2.5"

    val deps = Seq(hotspot, common, jmx, snakeYaml)
  }

  object TestTools {
    val log       = "ch.qos.logback" % "logback-classic" % "1.5.18"
    val scalaTest = "org.scalatest" %% "scalatest"       % "3.2.19"
    val deps      = Logging.deps ++ testDeps(scalaTest, akkaInmemoryJournal, log)
  }

  def scopeDeps(scope: String, modules: Seq[ModuleID]) = modules.map(m => m % scope)
  def testDeps(modules: ModuleID*)                     = scopeDeps("test", modules)

}
