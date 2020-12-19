# Minimalist Akka Observability

[![Build Status](https://dev.azure.com/pragmasoftnl/akka-sensors/_apis/build/status/jacum.akka-sensors?branchName=featurea%2Fcurrent)](https://dev.azure.com/pragmasoftnl/akka-sensors/_build/latest?definitionId=30&branchName=featurea%2Fcurrent)
[![codecov.io](http://codecov.io/github/jacum/akka-sensors/coverage.svg?branch=master)](https://codecov.io/gh/jacum/akka-sensors?branch=master)
[![Maven Central](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/nl/pragmasoft/akka-sensors/maven-metadata.xml.svg)](https://maven-badges.herokuapp.com/maven-central/nl.pragmasoft/akka-sensors_2.12)

**Non-intrusive native Prometheus collectors for Akka internals, negligible performance overhead, suitable for production use.**

- Are you running (or about to run) Akka in production, full-throttle, and want to see what happens inside?  Did your load tests produce some ask timeouts? thread starvation? threads behaving non-reactively? legacy code doing nasty blocking I/O? 

- Would be nice to use Cinnamon Telemetry, but LightBend subscription is out of reach? 

- Maybe already tried Kamon instrumentation, but is looks fragile and slows your app down - especially when running full-throttle?

- Already familiar with Prometheus/Grafana observability stack and use it for observing your applications?

If you answer 'yes' to most of the questions above, Akka Sensors may be the right choice for you:

- It is OSS/free, as in MIT license, and uses explicit, very lightweight instrumentation - yet is a treasure trove for a busy observability engineer.

- Comprehensive feature set to make internals of your Akka visible, in any environment, including high-load production. 

- Won't affect CPU costs, when running in public cloud.

- Demo/Evaluation setup included: Akka w/Cassandra persistence, with Prometheus server and Grafana dashboards.

## Features

### Thread watcher
 - thread watcher, keeping eye on threads running suspiciously long, and reporting their stacktraces - to help you find blocking code quickly 

###  Dispatcher stats
 - time of runnable waiting in queue (histogram) 
 - time of runnable run (histogram)
 - implementation-specific ForkJoinPool and ThreadPool stats (gauges)
 - thread states, as seen from JMX ThreadInfo (histogram, updated once in X seconds) 
 - active worker threads (histogram, updated on each runnable)

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
Instrumented Cassandra session provider, exposing Cassandra client metrics collection.

## Documentation

- [Usage and tips](./docs/USAGE.md)
- [Demo app with Prometheus and Grafana dashboards](./docs/DEMO.md)
