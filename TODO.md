### before first release
- prometheus/grafana compose setup
- grafana dashboard
- app w/embedded cassandra
- update dashboards for cassandra
- assertions unit tests
- thread state snapshots period - make configurable
- representative load test suite
- oss signature/secrets
- azure build + oss publishing
- article/release 0.3

### next version
- Cluster events - translate to cluster member/state numbers, how many up, how many unreachable etc.
- Cluster sharding visibility/stats
- akka remoting
- typed akka
- Add 'scope' to Runnables, to make 'labels' for runnables of certain kind to Prometheus; stoplist for some of labels, also execution watcher on/off
