# Scalable E-Commerce System

A production-grade, highly scalable distributed E-Commerce platform built using modern cloud-native architectures, Spring Boot, MongoDB, Redis, Docker, and distributed system design patterns.

---

## Current Status: Phase 1 Completed

**Phase 1: Project Structure and Backend Initialization** is currently implemented.

This phase establishes the foundational Spring Boot backend service, verified automated testing setup, containerization configuration, and initial health-check REST API.

> **Note on Future Phases**: Database connectivity (MongoDB), caching (Redis), distributed rate limiting, authentication, shopping carts, order pipelines, and other system design components are scheduled for implementation in subsequent phases (Phases 2 through 20). No database or cache dependencies are configured in Phase 1.

---

## Technology Stack

* **Programming Language**: Java 21 (LTS)
* **Backend Framework**: Spring Boot 3.3.5
* **Build Tool**: Apache Maven (3.9+)
* **API Architecture**: RESTful API
* **Containerization**: Docker (multi-stage Java 21 build)
* **Testing Framework**: JUnit 5, Spring Boot Test, MockMvc

---

## Prerequisites

Before running the project locally, ensure the following tools are installed:

1. **Java Development Kit (JDK) 21**:
   Verify installation:
   ```powershell
   java -version
   ```
2. **Apache Maven 3.9+**:
   Verify installation:
   ```powershell
   mvn -version
   ```
3. **Docker (Optional for container deployment)**:
   Verify installation:
   ```powershell
   docker --version
   ```

---

## Backend Directory Structure

```text
backend/
├── pom.xml                                      # Maven project configuration and dependencies
├── Dockerfile                                   # Multi-stage Docker container specification
├── .dockerignore                                # Build context exclusions for Docker
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ecommerce/
    │   │           ├── EcommerceApplication.java # Spring Boot main entry point
    │   │           └── health/
    │   │               ├── HealthController.java # Health check REST controller
    │   │               └── HealthResponse.java   # Health check response DTO
    │   └── resources/
    │       └── application.yml                  # Application configuration & logging settings
    └── test/
        └── java/
            └── com/
                └── ecommerce/
                    ├── EcommerceApplicationTests.java # Context load test
                    └── health/
                        └── HealthControllerTest.java  # MockMvc health endpoint test
```

---

## Health-Check Endpoint

The backend exposes a public health-check endpoint to verify application availability without authentication:

* **Method**: `GET`
* **URL**: `http://localhost:8080/api/health`
* **Response Status**: `200 OK`
* **Response Content-Type**: `application/json`

### Expected JSON Response

```json
{
  "status": "UP",
  "application": "ecommerce-backend"
}
```

---

## Running the Application Locally

### Using Maven (from the `backend/` directory)

Open Windows PowerShell or your preferred terminal:

```powershell
# Navigate to the backend directory
cd backend

# Run the Spring Boot application
mvn spring-boot:run
```

To run with a custom port via the `PORT` environment variable:

```powershell
# In PowerShell:
$env:PORT="9090"; mvn spring-boot:run
```

Once started, test the health check endpoint in a browser or via PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
```

or using `curl`:

```bash
curl http://localhost:8080/api/health
```

---

## Running Automated Tests

To execute the test suite (verifying context bootstrap and the health-check REST endpoint):

```powershell
cd backend
mvn test
```

---

## Building the Executable JAR

To package the backend into a standalone executable JAR:

```powershell
cd backend
mvn clean package
```

The compiled JAR will be located at `backend/target/ecommerce-backend-0.0.1-SNAPSHOT.jar`.

To run the packaged JAR:

```powershell
java -jar target/ecommerce-backend-0.0.1-SNAPSHOT.jar
```

---

## Building and Running with Docker

The project includes a multi-stage Dockerfile that builds the application using Maven and packages it into an optimized Java 21 runtime image running as a non-root user.

### 1. Build the Docker Image

The build context is the `backend/` directory:

```powershell
cd backend
docker build -t ecommerce-backend:latest .
```

### 2. Run the Docker Container

```powershell
docker run -d --name ecommerce-backend -p 8080:8080 ecommerce-backend:latest
```

### 3. Verify Container Health

```powershell
curl http://localhost:8080/api/health
```

### 4. Stop the Container

```powershell
docker stop ecommerce-backend
docker rm ecommerce-backend
```

