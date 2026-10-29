# Multi-stage build for the Eureka Service Registry.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY service-registry/pom.xml service-registry/pom.xml
RUN mvn -q -pl service-registry -am -DskipTests dependency:go-offline || true

COPY common-lib/src common-lib/src
COPY service-registry/src service-registry/src
RUN mvn -q -pl service-registry -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/service-registry/target/service-registry-*.jar app.jar
EXPOSE 8761
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
