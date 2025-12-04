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

- ## Docker Infrastructure

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

### **3. Mimir (Grafana Mimir)**
Stores all metrics pushed from simulators.
Dashboard UI available at:  http://localhost:10100

### **4. Grafana**
Dashboard UI available at:  http://localhost:3000

## How to Run the System

### **1. Start the Monitoring Stack**

At the project root:

```bash
docker compose up -d
```

### **2. Start Spring Boot Applications**

Each module can be started individually:
```bash
./gradlew :api-server:bootRun
./gradlew :api-watcher:bootRun
./gradlew :tcp-watcher:bootRun
```

Modules run on:

api-server → 7080

api-watcher      → 7010

tcp-watcher      → 7020
