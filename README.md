# Scalable E-Commerce System

A production-grade, highly scalable distributed E-Commerce platform built using modern cloud-native architectures, Java 21, Spring Boot 3, MongoDB, Redis, Docker Compose, and distributed system design patterns.

---

## Current Status: Phase 2 Completed

* **Phase 1: Project Structure and Backend Initialization** — Completed
* **Phase 2: Database and Cache Infrastructure Setup** — Completed

Phase 2 establishes the persistence and caching infrastructure using **MongoDB 7.0**, **Redis 7.4**, and **Docker Compose** multi-container orchestration. It introduces dependency connectivity checks, graceful degradation handling, health endpoints, automated testing, and safe secrets management.

> **Note on Future Phases**: User registration, authentication/JWT, product catalog, shopping carts, order pipelines, rate limiting, and other distributed system capabilities are scheduled for Phases 3 through 20.

---

## 20-Phase Implementation Roadmap

1. [x] **Phase 1**: Project Structure and Backend Initialization
2. [x] **Phase 2**: Database and Cache Infrastructure Setup
3. [ ] **Phase 3**: User Management & Authentication Service
4. [ ] **Phase 4**: Product Catalog & Inventory Service
5. [ ] **Phase 5**: High-Performance Product Caching (Redis Cache-Aside)
6. [ ] **Phase 6**: Shopping Cart Service & Distributed Session Store
7. [ ] **Phase 7**: Order Processing Pipeline & Saga Pattern
8. [ ] **Phase 8**: Distributed Unique ID Generator (Snowflake / UUIDv7)
9. [ ] **Phase 9**: Distributed Rate Limiting (Token Bucket / Sliding Window via Redis)
10. [ ] **Phase 10**: Search Service & Full-Text Search Engine
11. [ ] **Phase 11**: Real-Time Search Autocomplete (Trie / Redis Sorted Sets)
12. [ ] **Phase 12**: URL Shortener Service (Base62 Encoding & Bloom Filter)
13. [ ] **Phase 13**: Distributed Caching Invalidation & Cache Stampede Protection
14. [ ] **Phase 14**: Database Sharding & Partitioning Strategy
15. [ ] **Phase 15**: Distributed Lock Manager (Redlock Algorithm)
16. [ ] **Phase 16**: Asynchronous Messaging & Event-Driven Architecture (Kafka / RabbitMQ)
17. [ ] **Phase 17**: Web Crawler & Competitor Price Monitoring
18. [ ] **Phase 18**: API Gateway, Routing & SSL Termination
19. [ ] **Phase 19**: Observability, Distributed Tracing & Centralized Logging
20. [ ] **Phase 20**: End-to-End Performance Testing, Benchmarking & Production Hardening

---

## Technology Stack

* **Programming Language**: Java 21 (LTS - Eclipse Temurin)
* **Backend Framework**: Spring Boot 3.3.5
* **Build Tool**: Apache Maven (3.9+)
* **Primary Database**: MongoDB 7.0 (Document Store)
* **Distributed Cache**: Redis 7.4 Alpine (In-Memory Key-Value)
* **Containerization & Orchestration**: Docker & Docker Compose
* **Testing Suite**: JUnit 5, Spring Boot Test, Mockito, MockMvc

---

## Prerequisites

Before running the application or its containers, ensure the following tools are installed:

1. **Java Development Kit (JDK) 21**:
   ```powershell
   java -version
   ```
2. **Apache Maven 3.9+**:
   ```powershell
   mvn -version
   ```
3. **Docker Desktop & Docker Compose**:
   ```powershell
   docker --version
   docker compose version
   ```

---

## Repository Structure

```text
ECommerce-System/
├── backend/
│   ├── pom.xml                                      # Maven project configuration and dependencies
│   ├── Dockerfile                                   # Multi-stage Java 21 container specification
│   ├── .dockerignore                                # Build context exclusions for Docker
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── ecommerce/
│       │   │           ├── EcommerceApplication.java # Spring Boot entry point
│       │   │           └── health/
│       │   │               ├── HealthController.java        # REST controller for liveness & readiness
│       │   │               ├── HealthResponse.java          # Liveness DTO
│       │   │               ├── DependencyHealthResponse.java# Readiness DTO (MongoDB & Redis)
│       │   │               └── DependencyHealthService.java # Connectivity & ping health service
│       │   └── resources/
│       │       └── application.yml                  # Config properties & logging settings
│       └── test/
│           └── java/
│               └── com/
│                   └── ecommerce/
│                       ├── EcommerceApplicationTests.java          # Context bootstrap test
│                       └── health/
│                           ├── HealthControllerTest.java           # MockMvc endpoint tests
│                           ├── DependencyHealthServiceTest.java    # Unit tests for ping logic
│                           └── DependencyHealthIntegrationTest.java# Live container integration test
├── docker/
│   ├── docker-compose.yml                           # Compose orchestration (backend, mongodb, redis)
│   └── .env.example                                 # Template environment file
├── frontend/
├── docs/
├── testing/
├── .gitignore
└── README.md
```

---

## Database and Cache Infrastructure

### 1. MongoDB (Persistent Document Store)
* **Role**: Primary database for operational e-commerce data (user records, products, shopping carts, orders).
* **Docker Image**: `mongo:7.0`
* **Internal Port**: `27017`
* **Host Mapping**: `27017:27017`
* **Service Name**: `mongodb`
* **Persistence Mechanism**: Data persists in the named Docker volume `ecommerce_mongodb_data` (mapped to `/data/db`). Even if containers are stopped or recreated (`docker compose down`), MongoDB data remains intact.
* **Health Check**: Executes `mongosh --quiet --eval "db.adminCommand('ping').ok"` every 10s.

### 2. Redis (High-Performance In-Memory Cache)
* **Role**: Ephemeral storage for fast-access data, including cache-aside product caches, rate limiting counters, and session state.
* **Docker Image**: `redis:7.4-alpine`
* **Internal Port**: `6379`
* **Host Mapping**: `6379:6379`
* **Service Name**: `redis`
* **Cache Semantics & Data Loss Impact**: Redis is treated as an ephemeral cache without persistent volume storage. If Redis restarts or crashes, cached items are lost; however, this **does not result in permanent data loss** because all ground-truth data resides in MongoDB. When cache misses occur, the application reads from MongoDB and warms the cache transparently (cache-aside pattern).
* **Health Check**: Executes `redis-cli ping` every 10s.

### 3. Dedicated Application Network
* Services communicate over a dedicated bridge network (`ecommerce-network`). The backend communicates with `mongodb` and `redis` via service hostnames rather than hardcoded IPs or `localhost`.

---

## Environment Variables & Configuration

Configuration follows the 12-factor application methodology, reading from environment variables with sensible local defaults:

| Variable | Description | Default in Docker Compose | Default for Local Dev |
|---|---|---|---|
| `PORT` | Backend HTTP listening port | `8080` | `8080` |
| `MONGODB_URI` | MongoDB connection URI | `mongodb://mongodb:27017/ecommerce_db` | `mongodb://mongodb:27017/ecommerce_db?serverSelectionTimeoutMS=3000` |
| `REDIS_HOST` | Redis service host | `redis` | `redis` (override with `localhost`) |
| `REDIS_PORT` | Redis service port | `6379` | `6379` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` | `default` |

### Safe Secrets Management
* Non-secret template values are provided in `docker/.env.example`.
* To customize environment variables locally:
  ```powershell
  Copy-Item docker\.env.example docker\.env
  ```
* All `.env` files are ignored by Git (`.gitignore`) to prevent accidental commits of local secrets.

---

## Health-Check Endpoints

### 1. Process Liveness: `GET /api/health`
Verifies that the Spring Boot application JVM process is running. This check is intentionally independent of external database availability.

* **URL**: `http://localhost:8080/api/health`
* **Status**: `200 OK`
* **Response**:
  ```json
  {
    "status": "UP",
    "application": "ecommerce-backend"
  }
  ```

### 2. Dependency Readiness: `GET /api/health/dependencies`
Verifies live connectivity to MongoDB (via lightweight `db.adminCommand('ping')`) and Redis (via `ping() -> PONG`).
* **Design & HTTP Status Decision**:
  * **All dependencies healthy**: Returns `HTTP 200 OK`.
  * **Any dependency unavailable**: Returns `HTTP 503 SERVICE_UNAVAILABLE` with individual statuses (`"DOWN"`), without crashing the application. This aligns with standard Kubernetes readiness probes and load-balancer health checks.

#### Sample Response (All UP - HTTP 200)
```json
{
  "application": "ecommerce-backend",
  "mongodb": "UP",
  "redis": "UP"
}
```

#### Sample Response (Degraded - HTTP 503)
```json
{
  "application": "ecommerce-backend",
  "mongodb": "DOWN",
  "redis": "DOWN"
}
```

---

## Running the Application

### Option A: Complete Multi-Container Stack (Docker Compose)

From the project root (`C:\ECommerce-System`):

1. **Start all services** (MongoDB, Redis, and Backend):
   ```powershell
   docker compose -f docker/docker-compose.yml up -d --build
   ```
2. **Inspect container status**:
   ```powershell
   docker compose -f docker/docker-compose.yml ps
   ```
3. **Inspect application and database logs**:
   ```powershell
   docker compose -f docker/docker-compose.yml logs -f backend
   ```
4. **Verify health endpoints**:
   ```powershell
   curl.exe -i http://localhost:8080/api/health
   curl.exe -i http://localhost:8080/api/health/dependencies
   ```
5. **Stop services** (preserves MongoDB data volume):
   ```powershell
   docker compose -f docker/docker-compose.yml down
   ```
6. **Stop services and remove data volume** (resets database):
   ```powershell
   docker compose -f docker/docker-compose.yml down -v
   ```

---

### Option B: Local Standalone Development (outside Docker)

If running MongoDB and Redis in Docker or locally, but running the backend via Maven:

1. **Start only the database and cache containers**:
   ```powershell
   docker compose -f docker/docker-compose.yml up -d mongodb redis
   ```
2. **Run the backend using Maven** (pointing to `localhost`):
   ```powershell
   cd backend
   $env:MONGODB_URI="mongodb://localhost:27017/ecommerce_db"
   $env:REDIS_HOST="localhost"
   mvn spring-boot:run
   ```
3. **Package and run standalone JAR**:
   ```powershell
   cd backend
   mvn clean package
   java -jar target/ecommerce-backend-0.0.1-SNAPSHOT.jar
   ```

---

## Running Automated Tests

From the `backend/` directory:

### Run Unit and Controller Tests
Unit tests use mocks for external dependencies and **do not require Docker or active database servers**:

```powershell
cd backend
mvn test
```

### Run Full Verification Build
```powershell
cd backend
mvn clean verify
```

### Run Live Integration Tests against Containers
When Docker Compose services are running, enable integration tests via the `RUN_INTEGRATION_TESTS` environment variable:

```powershell
cd backend
$env:RUN_INTEGRATION_TESTS="true"
mvn test -Dtest=DependencyHealthIntegrationTest
```

---

## Troubleshooting

### Docker Desktop Daemon Not Running
* **Symptom**: `failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine` or `Docker Desktop Service is stopped`.
* **Resolution**:
  1. Open the Start Menu and launch **Docker Desktop**.
  2. Wait until the Docker status icon turns solid green / reports "Engine running".
  3. Verify with `docker info` in PowerShell.
  4. If required on Windows, start the `com.docker.service` service from an elevated PowerShell terminal (`Start-Service com.docker.service`).

### Port Conflicts
* If port `8080`, `27017`, or `6379` is already in use by a local process, configure alternate ports in `docker/.env`:
  ```dotenv
  PORT=8081
  MONGODB_PORT=27018
  REDIS_PORT=6380
  ```
