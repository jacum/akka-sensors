# Minimalist Akka Observability

**Non-intrusive native Prometheus collectors for Akka internals, negligible performance overhead, suitable for production use.**

- Are you running (or about to run) Akka in production, full-throttle, and want to see what happens inside?  Did your load tests produce some ask timeouts? thread starvation? threads behaving non-reactively? legacy code doing nasty blocking I/O? 

- Would be nice to use Cinnamon Telemetry, but LightBend subscription is out of reach? 

- Maybe already tried Kamon instrumentation, but is looks fragile and slows your app down - especially when running full-throttle?

- Already familiar with Prometheus/Grafana observability stack and use it for observing your applications?

If you answer 'yes' to most of the questions above, Akka Sensors may be the right choice for you. 

- It is OSS/free, as in MIT license, and uses explicit, very lightweight instrumentation - yet is a treasure trove for a busy observability engineer.

- Comprehensive feature set to make internals of your Akka visible, in any environment, including high-load production. 

- Could spare CPU costs, when running in public cloud.

- Demo/Evaluation setup included: Akka w/Cassandra persistence, with Prometheus server and Grafana dashboards.

## Features

###  Dispatcher stats
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

## Documentation

- [Usage and tips](./docs/USAGE.md)
- [Demo app with Prometheus and Grafana dashboards](./docs/DEMO.md)
