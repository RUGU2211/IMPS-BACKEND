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
    mv target/Imps-backend-*.jar app.jar

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

# Expose HTTP, TCP socket, and TLS/SSL ports (application.yml)
# HTTP: 8081 | NPCI socket TCP: 9083, TLS: 9443 | Switch socket TCP: 9086, TLS: 9446 | HTTPS (ssl profile): 8443
EXPOSE 8081 9083 9443 9086 9446 8443

# Run the application (docker profile = /app/certs paths)
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
