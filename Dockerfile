FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY src src
COPY pom.xml pom.xml
COPY .env .env
COPY settings.xml /root/.m2/settings.xml
RUN export $(grep -v '^#' .env | xargs) && mvn clean package -s /root/.m2/settings.xml -DskipTests


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk update && apk add --no-cache ca-certificates bind-tools && update-ca-certificates

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/.env .env
EXPOSE 8084

ENTRYPOINT ["/bin/sh", "-c", "export $(grep -v '^#' .env | xargs) && java -jar app.jar"]

