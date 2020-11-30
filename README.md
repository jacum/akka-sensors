# Minimalist Akka Observability

**Non-intrusive native Prometheus collectors for Akka internals, negligible performance overhead, suitable for production use.**

Are you planning to run Akka in production full-throttle, and want to see what happens inside? 

Would be nice to use Cinnamon, but LightBend subscription is out of reach? 

Maybe already tried Kamon instrumentation, but is looks fragile and slows your app down, especially when running full-throttle?

Then Akka Sensors may be the right choice for you: Free as in MIT license, and no heavy bytecode instrumentation either, yet a treasure trove for a busy observability engineer.

- Comprehensive feature set to make internals of your Akka visible, for performance tuning
- Sensible metrics in native Prometheus collectors
- Example pre-configured Akka cluster node with Cassandra persistence
- Some Grafana dashboards included

## Features

###  Dispatchers 
 - time of runnable waiting in queue (histogram) 
 - time of runnable run (histogram)
 - implementation-specific ForkJoinPool and ThreadPool stats (gauges)
 - thread states, as seen from JMX ThreadInfo (histogram, updated once in X seconds) 
 - active worker threads (histogram, updated on each runnable)

### Thread watcher
 - thread watcher, keeping eye on threads running suspiciously long, and reporting their stacktraces - to help you find blocking code quickly 

### Basic actor stats
 - number of actors (gauge)
 - time of actor 'receive' run (histogram)
 - actor activity time (histogram)
 - unhandled messages (count)
 - exceptions (count)
 
### Persistent actor stats
 - recovery time (histogram)
 - number of recovery events (histogram)
 - persist time (histogram)
 - recovery failures (counter)
 - persist failures (counter)

### Cluster
 - cluster events, per type (counter)

### Cassandra
Instrumented Cassandra session provider, leveraging Cassandra client metrics collection.

## Usage

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

    # some other dispatcher

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

## Notes

### Best practices for extensions
 - For anything additional to measure in actors, extend `*ActorMetrics` in your own traits.
 - Override `actorTag` for your own actor labelling schema. Just make sure you keep cardinality sane, make sure it's below 100.

