# Multi-stage build for cart-service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
RUN mvn -q -pl cart-service -am -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY cart-service/src cart-service/src
RUN mvn -q -pl cart-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/cart-service/target/cart-service-*.jar app.jar
EXPOSE 9004
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
