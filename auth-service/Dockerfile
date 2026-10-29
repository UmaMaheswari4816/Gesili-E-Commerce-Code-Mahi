# Multi-stage build for auth-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
RUN mvn -q -pl auth-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY auth-service/src auth-service/src
RUN mvn -q -pl auth-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service-*.jar app.jar
EXPOSE 9001
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
