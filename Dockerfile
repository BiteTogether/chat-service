FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

ARG COMMON_VERSION=0.0.7-SNAPSHOT
COPY libs/common-service-${COMMON_VERSION}.jar /tmp/common-service.jar

RUN mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.0:install-file \
    -Dfile=/tmp/common-service.jar \
    -DgroupId=io.github.bitetogether \
    -DartifactId=common-service \
    -Dversion=${COMMON_VERSION} \
    -Dpackaging=jar

RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Copy SSL certificates for Kafka (Aiven Cloud)
RUN mkdir -p /app/certs
COPY --from=builder /app/src/main/resources/client.keystore.p12 /app/certs/client.keystore.p12
COPY --from=builder /app/src/main/resources/client.truststore.jks /app/certs/client.truststore.jks

# Create directories for runtime credential overrides
RUN mkdir -p /app/config && chown -R spring:spring /app && chown -R spring:spring /app/certs
USER spring

EXPOSE 8083 7000
ENTRYPOINT ["java", "-jar", "app.jar"]
