FROM maven:3.9-eclipse-temurin-21-alpine as builder

WORKDIR /app

# Copy the parent pom.xml first
COPY pom.xml .

# Copy all module pom.xml files first (keep this list in sync with the
# top-level pom.xml <modules> block — used purely for layer caching of
# Maven dependency resolution).
COPY genesis-api/pom.xml genesis-api/
COPY genesis-common/pom.xml genesis-common/
COPY genesis-user/pom.xml genesis-user/
COPY genesis-workspace/pom.xml genesis-workspace/
COPY genesis-coref/pom.xml genesis-coref/
COPY genesis-import-export/pom.xml genesis-import-export/
COPY genesis-infra/pom.xml genesis-infra/
COPY genesis-editor/pom.xml genesis-editor/
COPY genesis-notification/pom.xml genesis-notification/
COPY genesis-pos/pom.xml genesis-pos/
COPY genesis-logging/pom.xml genesis-logging/
COPY genesis-wsd/pom.xml genesis-wsd/
COPY genesis-recommend/pom.xml genesis-recommend/
COPY genesis-ner/pom.xml genesis-ner/

# Create source directories (to avoid Maven errors)
RUN mkdir -p genesis-api/src/main/java \
    genesis-common/src/main/java \
    genesis-user/src/main/java \
    genesis-workspace/src/main/java \
    genesis-coref/src/main/java \
    genesis-import-export/src/main/java \
    genesis-infra/src/main/java \
    genesis-editor/src/main/java \
    genesis-notification/src/main/java \
    genesis-pos/src/main/java \
    genesis-logging/src/main/java \
    genesis-wsd/src/main/java \
    genesis-recommend/src/main/java \
    genesis-ner/src/main/java

# Install local dependencies first
RUN mvn -B install -N

# Copy all source code
COPY . .

# Package all modules
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# curl is required by docker-compose.yml healthcheck. Without it the
# healthcheck exec-fails ("curl: not found") every interval and the
# container is permanently reported as `unhealthy` even though the
# Spring Boot app is serving — orchestrators (compose, k8s) then treat
# it as never-ready and refuse to route traffic.
RUN apk add --no-cache curl

# Copy the built artifacts from the builder stage
COPY --from=builder /app/genesis-api/target/genesis-api-*.jar app.jar

EXPOSE 8080

# Use exec form for proper signal handling
ENTRYPOINT ["java", "-jar", "app.jar"]