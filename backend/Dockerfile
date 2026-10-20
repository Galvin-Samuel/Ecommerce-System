# ==========================================
# Stage 1: Build stage using Maven and Java 21
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven POM file and resolve dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source tree and compile/package the Spring Boot application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Runtime stage with minimal Java 21 JRE
# ==========================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as non-privileged user for container security
RUN groupadd --system --gid 1001 ecommerce && \
    useradd --system --uid 1001 --gid ecommerce --no-create-home ecommerce

# Copy the packaged executable JAR from the builder stage
COPY --from=build --chown=ecommerce:ecommerce /workspace/target/ecommerce-backend-*.jar app.jar

USER ecommerce

# Expose default HTTP port
EXPOSE 8080

# Environment variables
ENV PORT=8080

# Run the executable JAR
ENTRYPOINT ["java", "-jar", "app.jar"]

