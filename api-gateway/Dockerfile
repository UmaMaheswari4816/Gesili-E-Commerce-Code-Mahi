# Multi-stage build for api-gateway.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -q -pl api-gateway -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn -q -pl api-gateway -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
