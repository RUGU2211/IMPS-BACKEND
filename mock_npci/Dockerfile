# ============================================
# Stage 1: Build the JAR with Maven
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy pom and download dependencies (cached unless pom changes)
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# Copy source and build JAR (skip tests for smaller/faster image)
COPY src ./src
RUN mvn package -DskipTests -B && \
    mv target/mock-npci-*.jar app.jar

# ============================================
# Stage 2: Runtime image
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /build/app.jar app.jar

# TLS/SSL: mount certs at /app/certs: docker run -v ./certs:/app/certs ...
# Uses spring.profiles.active=docker for /app/certs paths
RUN mkdir -p /app/certs

# Expose HTTP, TCP socket, TLS socket, and HTTPS (application.yml)
# HTTP: 8083 | Socket TCP: 9085 | Socket TLS: 9445 | HTTPS (ssl profile): 8445
EXPOSE 8083 9085 9445 8445

# Run with docker profile (cert paths /app/certs). Add ssl profile for HTTPS: -Dspring.profiles.active=docker,ssl
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
