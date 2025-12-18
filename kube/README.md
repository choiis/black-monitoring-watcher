# Black Monitoring Platform (Kubernetes + Minikube)

This repository deploys the **Black Monitoring Platform** on Kubernetes using **Minikube**.

It consists of:
- API Server
- API Watcher
- TCP Watcher
- Cassandra
- Zookeeper
- Grafana Mimir
- Grafana

All components run inside the `black-monitoring` namespace.

---

## Architecture Overview

```
[ api-watcher ] ----+
                    |--> [ api-server ] --> Cassandra
[ tcp-watcher ] ----+            |
                                   --> Zookeeper
                                   --> Mimir Gateway --> Mimir --> Grafana
```

---

## Prerequisites

- Docker
- kubectl
- minikube
- kubens (optional)

```bash
brew install minikube kubectl kubectx
```

---

## Start Minikube

```bash
minikube start
minikube tunnel
```

---

## Load Images into Minikube

```bash
minikube image load black-monitoring-api-server:1.0.0
minikube image load black-monitoring-api-watcher:1.0.0
minikube image load black-monitoring-tcp-watcher:1.0.0
```

---

## Namespace Setup

```bash
kubectl create namespace black-monitoring
kubens black-monitoring
```

---

## Deploy Infrastructure

```bash
kubectl apply -f infra.yaml
```

Check status:

```bash
kubectl get pods
kubectl get svc
```

---

## Deploy Applications

```bash
kubectl apply -f apps.yaml
```

---

## Port Forwarding

### Cassandra

```bash
kubectl -n black-monitoring port-forward svc/cassandra 9042:9042
cqlsh 127.0.0.1 9042
```

### Mimir Gateway

```bash
kubectl -n black-monitoring port-forward svc/mimir-gateway 10100:10100
```

### Grafana

```bash
kubectl -n black-monitoring port-forward svc/grafana 3000:3000
```

Access:
```
http://localhost:3000
ID: admin
PW: admin
```

---

## Environment Variables

Injected via ConfigMap:

```
CASSANDRA_CONTACT_POINTS=cassandra.black-monitoring.svc.cluster.local
CASSANDRA_PORT=9042
ZOOKEEPER_CONNECT=zookeeper.black-monitoring.svc.cluster.local:2181
MIMIR_URL=http://mimir-gateway.black-monitoring.svc.cluster.local:10100
API_SERVER_BASE_URL=http://api-server.black-monitoring.svc.cluster.local:7080
```

---

## Cleanup

```bash
kubectl delete namespace black-monitoring
minikube stop
```

---

## Status

✔ Local Kubernetes stack ready  
✔ Cassandra schema auto-initialized  
✔ Metrics flowing to Mimir  
✔ Grafana ready
