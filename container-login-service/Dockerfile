# Simple Dockerfile for container-login-service
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the pre-built JAR
COPY target/*.jar app.jar

# 12-Factor: Port Binding - Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 12-Factor: Processes - Stateless, disposable processes with k8s profile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=k8s", \
    "-jar", "/app/app.jar"]
