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
COPY .env .env
RUN chown -R spring:spring /app
USER spring

EXPOSE 8083
ENTRYPOINT ["/bin/sh", "-c", "export $(grep -v '^#' .env | xargs) && java -jar app.jar"]
