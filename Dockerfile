# Build stage
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Create necessary directories
RUN mkdir -p logs uploads

# Copy the jar file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1000 -S spring && \
    adduser -u 1000 -S spring -G spring && \
    chown -R spring:spring /app

USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]