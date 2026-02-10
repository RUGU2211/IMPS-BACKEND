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
    mv target/mock-switch-*.jar app.jar

# ============================================
# Stage 2: Runtime image
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /build/app.jar app.jar

# TLS/SSL: use /app/certs (mount your certs: -v ./certs:/app/certs)
RUN mkdir -p /app/certs
ENV SOCKET_SSL_KEY_STORE=file:/app/certs/switch-keystore.jks

# Expose HTTP, TCP socket, and TLS/SSL ports (application.yml)
# HTTP: 8082 | Socket TCP: 9084 | Socket TLS: 9444
EXPOSE 8082 9084 9444

# Run the application (TCP + TLS/SSL enabled via config)
ENTRYPOINT ["java", "-jar", "app.jar"]
