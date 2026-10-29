# Multi-stage build for payment-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
RUN mvn -q -pl payment-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY payment-service/src payment-service/src
RUN mvn -q -pl payment-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar
EXPOSE 9006
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
