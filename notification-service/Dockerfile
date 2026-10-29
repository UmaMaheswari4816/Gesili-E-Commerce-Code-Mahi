# Multi-stage build for notification-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -q -pl notification-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY notification-service/src notification-service/src
RUN mvn -q -pl notification-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service-*.jar app.jar
EXPOSE 9007
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
