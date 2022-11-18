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
    val slf4jversion = "2.0.4"
    val slf4jApi     = "org.slf4j"                   % "slf4j-api"     % slf4jversion
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
    val deps         = Seq(slf4jApi, scalaLogging)
  }

  object Akka {
    val akkaVersion                     = "2.6.20"
    val akkaManagementVersion           = "1.0.9"
    val akkaPersistenceCassandraVersion = "1.0.5"
    val akkaHttpVersion                 = "10.2.1"

    val actor            = "com.typesafe.akka" %% "akka-actor"             % akkaVersion
    val typed            = "com.typesafe.akka" %% "akka-actor-typed"       % akkaVersion
    val persistence      = "com.typesafe.akka" %% "akka-persistence"       % akkaVersion
    val persistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
    val persistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion excludeAll (
            ExclusionRule("com.datastax.oss"),
            ExclusionRule("com.fasterxml.jackson.core")
    )

    val cluster      = "com.typesafe.akka" %% "akka-cluster"       % akkaVersion
    val clusterTyped = "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
    val slf4j        = "com.typesafe.akka" %% "akka-slf4j"         % akkaVersion

    val deps = Seq(actor, typed, persistence, persistenceTyped, persistenceQuery, persistenceCassandra, cluster, clusterTyped, clusterTools, slf4j) ++ Logging.deps
  }

  object Prometheus {
    val hotspot   = "io.prometheus"     % "simpleclient_hotspot" % "0.16.0"
    val common    = "io.prometheus"     % "simpleclient_common"  % "0.16.0"
    val jmx       = "io.prometheus.jmx" % "collector"            % "0.17.1" exclude ("org.yaml", "snakeyaml")
    val snakeYaml = "org.yaml"          % "snakeyaml"            % "1.32"

    val deps = Seq(hotspot, common, jmx, snakeYaml)
  }

  object Http4s {
    // unfortunately, http4s modules' versions not synced anymore
    val http4sVersion02312  = "0.23.12"
    val http4sVersion02314  = "0.23.16"
    val http4sVersion0241   = "0.24.1"
    val server              = "org.http4s"       %% "http4s-blaze-server"       % http4sVersion02312
    val client              = "org.http4s"       %% "http4s-blaze-client"       % http4sVersion02312
    val jdkClient           = "org.http4s"       %% "http4s-jdk-http-client"    % "0.7.0"
    val xml                 = "org.http4s"       %% "http4s-scala-xml"          % http4sVersion02312
    val circe               = "org.http4s"       %% "http4s-circe"              % http4sVersion02314
    val dsl                 = "org.http4s"       %% "http4s-dsl"                % http4sVersion02314
    val metrics             = "org.http4s"       %% "http4s-prometheus-metrics" % http4sVersion0241
    val prometheusJmx       = "io.prometheus.jmx" % "collector"                 % "0.17.2"
    val deps: Seq[ModuleID] = Seq(server, client, circe, xml, dsl, metrics, prometheusJmx)
  }

  object App {

    val deps = Seq(
        Cassandra.cassandraUnit
      ) ++ Http4s.deps ++ Akka.deps ++ Cassandra.deps ++ Logging.deps
  }

  object Cassandra {
    val akkaPersistenceCassandraVersion = "1.0.6"
    val cassandraDriverVersion          = "4.14.1"

    val cassandraDriverCore         = "com.datastax.oss"      % "java-driver-core"           % cassandraDriverVersion
    val cassandraDriverQueryBuilder = "com.datastax.oss"      % "java-driver-query-builder"  % cassandraDriverVersion
    val cassandraDriverMetrics      = "io.dropwizard.metrics" % "metrics-jmx"                % "4.2.12"
    val akkaPersistenceCassandra    = "com.typesafe.akka"    %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion
    val cassandraUnit               = "org.cassandraunit"     % "cassandra-unit"             % "4.3.1.0"

    val deps = Seq(akkaPersistenceCassandra, cassandraDriverCore, cassandraDriverQueryBuilder, cassandraDriverMetrics)
  }

  object TestTools {
    val log       = "ch.qos.logback" % "logback-classic" % "1.4.1"
    val scalaTest = "org.scalatest" %% "scalatest"       % "3.2.13"
    val deps      = Logging.deps ++ testDeps(scalaTest, akkaInmemoryJournal, log)
  }

  def scopeDeps(scope: String, modules: Seq[ModuleID]) = modules.map(m => m % scope)
  def testDeps(modules: ModuleID*)                     = scopeDeps("test", modules)

}
