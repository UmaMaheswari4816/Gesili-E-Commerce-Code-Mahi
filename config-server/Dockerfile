# Multi-stage build for the Config Server.
# Stage 1: build the whole monorepo context enough to package this module.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the parent POM and the modules this one needs, then the source.
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY config-server/pom.xml config-server/pom.xml
# Warm the dependency cache (best-effort; ignores the fact sources aren't present yet).
RUN mvn -q -pl config-server -am -DskipTests dependency:go-offline || true

COPY common-lib/src common-lib/src
COPY config-server/src config-server/src
RUN mvn -q -pl config-server -am -DskipTests clean package

# Stage 2: slim runtime image.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# config-repo is mounted at /config-repo by docker-compose (read-only).
COPY --from=build /workspace/config-server/target/config-server-*.jar app.jar
EXPOSE 8888
# -Xmx capped for limited-RAM machines; override via JAVA_OPTS.
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
