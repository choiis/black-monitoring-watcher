```bash
./gradlew :api-server:bootJar :api-watcher:bootJar :tcp-watcher:bootJar

docker build -t <YOUR_REGISTRY>/black-monitoring-api-server:1.0.0 api-server
docker build -t <YOUR_REGISTRY>/black-monitoring-api-watcher:1.0.0 api-watcher
docker build -t <YOUR_REGISTRY>/black-monitoring-tcp-watcher:1.0.0 tcp-watcher

docker push <YOUR_REGISTRY>/black-monitoring-api-server:1.0.0
docker push <YOUR_REGISTRY>/black-monitoring-api-watcher:1.0.0
docker push <YOUR_REGISTRY>/black-monitoring-tcp-watcher:1.0.0
```