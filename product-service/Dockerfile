# Multi-stage build for product-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY product-service/pom.xml product-service/pom.xml
RUN mvn -q -pl product-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn -q -pl product-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/product-service/target/product-service-*.jar app.jar
EXPOSE 9002
ENV JAVA_OPTS="-Xms192m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
