# Black Monitoring Watcher

Black Monitoring Watcher is a distributed, reactive monitoring system designed to
execute API/TCP scenarios, measure latency metrics (DNS, connection, communication time),
and push observability data into Grafana Mimir for centralized analysis.

The project is built as a **multi-module Spring Boot (WebFlux) application** with:
- Cassandra as scenario storage
- Zookeeper for distributed coordination
- Mimir + Grafana for metrics storage and dashboarding
- Batch workers + simulators executing scenarios in parallel

---

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Kotlin | 1.9.25 |
| **Runtime** | Java | 17 |
| **Framework** | Spring Boot | 3.3.4 |
| **Reactive** | Spring WebFlux | 3.3.4 |
| **Database** | Apache Cassandra | 4.1 |
| **Coordination** | Apache Zookeeper | 3.9 |
| **Zookeeper Client** | Apache Curator | 5.6.0 |
| **Metrics Storage** | Grafana Mimir | 2.11.0 |
| **Visualization** | Grafana | 8.0.2 |
| **Web Automation** | Selenium WebDriver | 4.18.1 |
| **HTTP Client** | Spring WebClient / OpenFeign | - |
| **Build Tool** | Gradle (Kotlin DSL) | 8.x |

---

## Project Structure

```
black-monitoring-watcher/
├── api-server/                 # Central management & alert server
│   └── src/main/kotlin/com/monitor/api/
│       ├── controller/         # REST API endpoints
│       ├── domain/             # Domain entities
│       ├── repository/         # Cassandra repositories
│       ├── service/            # Business logic
│       ├── mimir/              # Mimir metric pusher
│       └── client/             # Alert client
│
├── api-watcher/                # HTTP API scenario executor
│   └── src/main/kotlin/com/monitor/apibatch/
│       ├── config/             # Zookeeper, WebClient config
│       ├── simulator/          # API scenario simulator
│       └── worker/             # Batch worker with partitioning
│
├── tcp-watcher/                # TCP scenario executor
│   └── src/main/kotlin/com/monitor/tcpbatch/
│       ├── config/             # Zookeeper config
│       ├── simulator/          # TCP scenario simulator
│       └── worker/             # Batch worker with partitioning
│
├── web-watcher/                # Selenium web scenario executor
│   └── src/main/kotlin/com/monitor/webbatch/
│       ├── config/             # Selenium, Zookeeper config
│       ├── simulator/          # Web scenario simulator
│       └── worker/             # Batch worker with partitioning
│
├── kube/                       # Kubernetes deployment manifests
├── docker-compose.yml          # Docker infrastructure stack
├── init.cql                    # Cassandra schema initialization
├── settings.gradle.kts         # Gradle multi-module settings
└── build.gradle.kts            # Root build configuration
```

---

## Features

### Distributed Scenario Execution
The system executes three types of monitoring scenarios:
- **API Scenarios** — triggers HTTP requests using WebClient
- **TCP Scenarios** — measures DNS lookup, TCP connect, and communication times
- **Web Scenarios** — uses Selenium headless Chrome to load web pages, execute JavaScript, and perform login scripts

### Metrics Collection
Each scenario collects detailed metrics:

| Scenario Type | Metrics |
|---------------|---------|
| **API** | DNS Time, Request Time, HTTP Status Code |
| **TCP** | DNS Time, Connect Time, Communication Time |
| **Web** | Page Load Time, JavaScript Execution Time |

### Distributed Coordination
- **Zookeeper-based partitioning**: Scenarios are partitioned across worker instances using consistent hashing
- **Automatic rebalancing**: When instances join/leave, scenarios are redistributed
- **Horizontal scaling**: Add more watcher instances to handle increased load

### Real-time Alerting
- Automatic email notifications on scenario failures
- Configurable alert recipients per service

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
- Web scenarios

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
./gradlew :web-watcher:bootRun
```

Modules run on:

- api-server → **7080**
- api-watcher → **7010**
- tcp-watcher → **7020**
- web-watcher → **7030**

---

### **3. Build Docker images**

```bash
docker build -t black-monitoring-api-server:1.0.0 api-server
docker build -t black-monitoring-api-watcher:1.0.0 api-watcher
docker build -t black-monitoring-tcp-watcher:1.0.0 tcp-watcher
docker build -t black-monitoring-web-watcher:1.0.0 web-watcher
```

> **Note:** The web-watcher image includes Google Chrome for Selenium headless browser testing.

---

### **4. Kubernetes Deployment**

Deploy to Kubernetes using the manifests in `kube/` directory:

```bash
kubectl apply -f kube/
```

The Kubernetes deployment includes:
- ConfigMaps for application configuration
- Deployments for each watcher module
- Services for internal communication
- Ingress for external access (optional)

---

## Metrics & Monitoring

### Prometheus Metrics Format

Metrics are pushed to Mimir using Prometheus remote write protocol:

```
# API Scenario Metrics
api_scenario_dns_time_ms{service="<uuid>", scenario="<name>"} <value>
api_scenario_request_time_ms{service="<uuid>", scenario="<name>"} <value>
api_scenario_status_code{service="<uuid>", scenario="<name>"} <value>

# TCP Scenario Metrics
tcp_scenario_dns_time_ms{service="<uuid>", scenario="<name>"} <value>
tcp_scenario_connect_time_ms{service="<uuid>", scenario="<name>"} <value>
tcp_scenario_comm_time_ms{service="<uuid>", scenario="<name>"} <value>

# Web Scenario Metrics
web_scenario_load_time_ms{service="<uuid>", scenario="<name>"} <value>
```

### Grafana Dashboard

Access Grafana at `http://localhost:3000` to visualize:
- Response time trends
- Success/failure rates
- DNS resolution times
- TCP connection health
- Web page load performance

---

## Email Alert Workflow Summary

1. watcher detects a failure
2. sends alert → `/api/v1/alert`
3. api-server retrieves service info from Cassandra
4. sends email to the service owner automatically

This ensures immediate notification for degraded or failing scenarios.

---

## API Endpoints

### api-server (Port 7080)

#### Service Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/service` | List all services |
| GET | `/api/v1/service/{uuid}` | Get service by UUID |
| POST | `/api/v1/service` | Create new service |
| PUT | `/api/v1/service/{uuid}` | Update service |
| DELETE | `/api/v1/service/{uuid}` | Delete service |

#### API Scenario Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/api-scenario` | List all API scenarios |
| GET | `/api/v1/api-scenario/{serviceUuid}/{scenarioUuid}` | Get specific scenario |
| POST | `/api/v1/api-scenario` | Create new API scenario |
| PUT | `/api/v1/api-scenario` | Update API scenario |
| DELETE | `/api/v1/api-scenario/{serviceUuid}/{scenarioUuid}` | Delete API scenario |

#### TCP Scenario Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/tcp-scenario` | List all TCP scenarios |
| POST | `/api/v1/tcp-scenario` | Create new TCP scenario |
| PUT | `/api/v1/tcp-scenario` | Update TCP scenario |
| DELETE | `/api/v1/tcp-scenario/{serviceUuid}/{scenarioUuid}` | Delete TCP scenario |

#### Web Scenario Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/web-scenario` | List all Web scenarios |
| POST | `/api/v1/web-scenario` | Create new Web scenario |
| PUT | `/api/v1/web-scenario` | Update Web scenario |
| DELETE | `/api/v1/web-scenario/{serviceUuid}/{scenarioUuid}` | Delete Web scenario |

#### Alert
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/alert` | Receive failure alerts from watchers |

#### Metrics
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/metrics/push` | Push metrics to Mimir |

---

## Cassandra Schema

The system uses the following tables in the `monitoring` keyspace:

```sql
-- Service metadata
CREATE TABLE service (
    uuid UUID PRIMARY KEY,
    service_name TEXT,
    email TEXT,
    description TEXT
);

-- API monitoring scenarios
CREATE TABLE api_scenario (
    service_uuid UUID,
    scenario_uuid UUID,
    scenario_name TEXT,
    url TEXT,
    method TEXT,
    headers MAP<TEXT, TEXT>,
    body TEXT,
    PRIMARY KEY (service_uuid, scenario_uuid)
);

-- TCP monitoring scenarios
CREATE TABLE tcp_scenario (
    service_uuid UUID,
    scenario_uuid UUID,
    scenario_name TEXT,
    host TEXT,
    port INT,
    PRIMARY KEY (service_uuid, scenario_uuid)
);

-- Web monitoring scenarios (Selenium)
CREATE TABLE web_scenario (
    service_uuid UUID,
    scenario_uuid UUID,
    scenario_name TEXT,
    url TEXT,
    javascript TEXT,
    login_script TEXT,
    PRIMARY KEY (service_uuid, scenario_uuid)
);
```

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CASSANDRA_HOST` | localhost | Cassandra contact point |
| `CASSANDRA_PORT` | 9042 | Cassandra native port |
| `ZOOKEEPER_CONNECT` | localhost:2181 | Zookeeper connection string |
| `MIMIR_URL` | http://localhost:10100 | Grafana Mimir endpoint |
| `MAIL_HOST` | localhost | SMTP server host |
| `MAIL_PORT` | 1025 | SMTP server port |

### Module Ports

| Module | Port | Description |
|--------|------|-------------|
| api-server | 7080 | Management & Alert API |
| api-watcher | 7010 | HTTP scenario executor |
| tcp-watcher | 7020 | TCP scenario executor |
| web-watcher | 7030 | Web scenario executor |

---

## Development

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Gradle 8.x (or use included Gradle wrapper)

### Local Development Setup

1. **Start infrastructure:**
   ```bash
   docker compose up -d cassandra zookeeper mimir grafana mailpit
   ```

2. **Wait for Cassandra to be ready:**
   ```bash
   docker exec -it cassandra cqlsh -e "DESCRIBE KEYSPACES"
   ```

3. **Initialize schema:**
   ```bash
   docker exec -i cassandra cqlsh < init.cql
   ```

4. **Run applications in separate terminals:**
   ```bash
   ./gradlew :api-server:bootRun
   ./gradlew :api-watcher:bootRun
   ./gradlew :tcp-watcher:bootRun
   ./gradlew :web-watcher:bootRun
   ```

### Running Tests

```bash
./gradlew test
```

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Cassandra connection refused | Ensure Cassandra is running and healthy |
| Zookeeper session expired | Check Zookeeper connectivity and restart watchers |
| Mimir push failed | Verify Mimir is running on port 10100 |
| Email not sent | Check Mailpit is running (SMTP: 1025, UI: 8025) |
| Selenium WebDriver error | Ensure Chrome is installed for web-watcher |

### Health Check Endpoints

Each module exposes Spring Boot Actuator endpoints:

```bash
# Health check
curl http://localhost:<port>/actuator/health

# Application info
curl http://localhost:<port>/actuator/info
```

---

## License

This project is for educational and practice purposes.