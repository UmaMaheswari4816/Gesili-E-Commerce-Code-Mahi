# Multi-stage build for order-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -q -pl order-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY order-service/src order-service/src
RUN mvn -q -pl order-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service-*.jar app.jar
EXPOSE 9005
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
