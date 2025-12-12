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
    val slf4jApi     = "org.slf4j"                   % "slf4j-api"       % slf4jversion
    val logback      = "ch.qos.logback"              % "logback-classic" % "1.5.22"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.6"
    val deps         = Seq(slf4jApi, scalaLogging, logback)
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
    val hotspot        = "io.prometheus" % "prometheus-metrics-instrumentation-jvm"    % "1.4.1"
    val common         = "io.prometheus" % "prometheus-metrics-core"                   % "1.4.1"
    val exposition     = "io.prometheus" % "prometheus-metrics-exposition-textformats" % "1.4.1"
    val exporterCommon = "io.prometheus" % "prometheus-metrics-exporter-common"        % "1.4.1"

    val jmx       = "io.prometheus.jmx" % "collector" % "1.5.0"
    val snakeYaml = "org.yaml"          % "snakeyaml" % "2.5"

    val deps = Seq(hotspot, common, exporterCommon, jmx, exposition, snakeYaml)
  }

  object TestTools {
    val log       = "ch.qos.logback"                                                % "logback-classic" % "1.5.22"
    val scalaTest = "org.scalatest"                                                %% "scalatest"       % "3.2.19"
    val deps      = Logging.deps ++ Seq(scalaTest, akkaInmemoryJournal, log) map (_ % Test)
  }

}
