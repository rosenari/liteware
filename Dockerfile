# Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Download dependencies (using gradle from the image)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN gradle bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Create necessary directories
RUN mkdir -p logs uploads

# Copy the jar file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN groupadd -g 1000 spring && \
    useradd -u 1000 -g spring spring && \
    chown -R spring:spring /app

USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]