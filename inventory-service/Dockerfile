# Multi-stage build for inventory-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
RUN mvn -q -pl inventory-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY inventory-service/src inventory-service/src
RUN mvn -q -pl inventory-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 9003
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
