### Usage

## SBT dependency

```
libraryDependencies ++= 
  Seq(
     "nl.pragmasoft.sensors" %% "sensors-core" % "0.0.2",
     "nl.pragmasoft.sensors" %% "sensors-cassandra" % "0.0.2"
  )
```

## Prometheus exporter

If you already have Prometheus exporter in your application, `CollectorRegistry.defaultRegistry` will be used by Sensors and the metrics should appear automatically.

For an example of HTTP exporter service, check `MetricService` implementation in example application (`app`) module. 

## Application configuration

Override `type` and `executor` with Sensors' instrumented executors.
Add `akka.sensors.AkkaSensorsExtension` to extensions.

```
akka {

  actor {

    # main/global/default dispatcher

    default-dispatcher {
      type = "akka.sensors.dispatch.InstrumentedDispatcherConfigurator"
      executor = "akka.sensors.dispatch.InstrumentedExecutor"

      instrumented-executor {
        delegate = "fork-join-executor" 
        measure-runs = true
        watch-long-runs = true
        watch-check-interval = 1s
        watch-too-long-run = 3s
      }
    }

    # some other dispatcher used in your app

    default-blocking-io-dispatcher {
      type = "akka.sensors.dispatch.InstrumentedDispatcherConfigurator"
      executor = "akka.sensors.dispatch.InstrumentedExecutor"

      instrumented-executor {
        delegate = "thread-pool-executor"
        measure-runs = true
        watch-long-runs = false
      }
    }
  }

  extensions = [
    akka.sensors.AkkaSensorsExtension
  ]
}

```

## Actors

```
 # Non-persistent actors
 class MyImportantActor extends Actor with ActorMetrics {

    # This becomes label 'actor', default is simple class name
    # but you may segment it further
    # Just make sure the cardinality is sane (<100)
    override protected def actorTag: String = ... 

      ... # your implementation
  }

 # Persistent actors
 class MyImportantPersistentActor extends Actor with PersistentActorMetrics {
  ...


```

## Additional metrics

For anything additional to measure in actors, extend `*ActorMetrics` in your own trait.

```
trait CustomActorMetrics extends ActorMetrics  with MetricsBuilders {

  val importantEvents: Counter = counter
    .name("important_events_total")
    .help(s"Important events")
    .labelNames("actor")
    .register(metrics.registry)

}

```

## Why codahale is used alongside Prometheus?

We would prefer 100% Prometheus, however Cassandra Datastax OSS driver doesn't support Prometheus collectors.
Prometheus is our preferred main metrics engine, hence we brigde metrics from Codahale via JMX.
This won't be needed anymore if Prometheus would be supported natively by Datastax driver.
