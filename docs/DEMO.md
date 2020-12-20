## Demo setup

We assuming you have `docker` and `docker-compose` up and running.

Prepare sample app:
```
sbt "compile; project app; docker:publishLocal"
```

Start observability stack:
```
docker-compose -f observability/docker-compose.yml up
```

Send some events:
```
for z in {1..100}; do curl -X POST http://localhost:8080/api/ping-fj/$z/100; done
for z in {101..200}; do curl -X POST http://localhost:8080/api/ping-tp/$z/100; done
for z in {3001..3300}; do curl -X POST http://localhost:8080/api/ping-persistence/$z/300 ; done
```

Open Grafana at http://localhost:3000.

Go to http://localhost:3000/plugins/sensors-prometheus-app, click *Enable*.
Sensors' bundled dashboards will be imported.

