# Black Monitoring Watcher

Black Monitoring Watcher is a distributed, reactive monitoring system designed to
execute API/TCP scenarios, measure latency metrics (DNS, connection, communication time),
and push observability data into Grafana Mimir for centralized analysis.

The project is built as a **multi-module Spring Boot (WebFlux) application** with:
- Cassandra as scenario storage
- Zookeeper for distributed coordination
- Mimir + Grafana for metrics storage and dashboarding
- Batch workers + simulators executing scenarios in parallel

## Features

### Distributed Scenario Execution
The system executes two types of monitoring scenarios:
- **API Scenarios** — triggers HTTP requests using WebClient
- **TCP Scenarios** — measures DNS lookup, TCP connect, and communication times

---

## 📬 Email Alerting on Scenario Failure

Whenever any monitoring scenario fails due to:

- DNS lookup failure
- HTTP timeout
- HTTP 4xx / 5xx
- TCP connection timeout
- Connection refused
- Unexpected exception

the watcher automatically sends an alert to the **api-server** at:

```
POST /api/v1/alert
```

The **api-server** performs:

1. Looks up the service owner’s email from the Cassandra `service` table (`email` column).
2. Sends an alert email containing:
    - service name
    - scenario name
    - failure message

This enables real‑time failure visibility and rapid operational response.

---

## Docker Infrastructure

The repository includes a full monitoring stack via **docker-compose**:

### **1. Cassandra**
Stores:
- service definitions
- API scenarios
- TCP scenarios  
  Runs schema initialization via `init.cql`.

### **2. Zookeeper 3.x**
Provides:
- coordination
- distributed locks
- scenario partitioning between multiple batch workers

### **3. Grafana Mimir**
Stores all metrics pushed from simulators.  
UI: **http://localhost:10100**

### **4. Grafana**
Dashboard visualization.  
UI: **http://localhost:3000**

---

## How to Run the System

### **1. Start the Monitoring Stack**

```bash
docker compose up -d
```

### **2. Start Spring Boot Applications**

```bash
./gradlew :api-server:bootRun
./gradlew :api-watcher:bootRun
./gradlew :tcp-watcher:bootRun
```

Modules run on:

- api-server → **7080**
- api-watcher → **7010**
- tcp-watcher → **7020**

---

## Email Alert Workflow Summary

1. watcher detects a failure
2. sends alert → `/api/v1/alert`
3. api-server retrieves service info from Cassandra
4. sends email to the service owner automatically

This ensures immediate notification for degraded or failing scenarios.

---

