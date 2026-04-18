FROM eclipse-temurin:21-jre-alpine

ARG VERSION=0.0.1-SNAPSHOT

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Copy pre-built JAR from local target directory
COPY target/chat-service-${VERSION}.jar app.jar

# Copy SSL certificates for Kafka (Aiven Cloud)
COPY src/main/resources/client.keystore.p12 /app/certs/client.keystore.p12
COPY src/main/resources/client.truststore.jks /app/certs/client.truststore.jks

# Create directories and set permissions
RUN mkdir -p /app/config /app/certs && \
    chown -R spring:spring /app && \
    chmod 600 /app/certs/client.keystore.p12 /app/certs/client.truststore.jks

USER spring

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
