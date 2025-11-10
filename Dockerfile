FROM maven:3.9-eclipse-temurin-21-alpine as builder

WORKDIR /app

# Copy the parent pom.xml first
COPY pom.xml .

# Copy all module pom.xml files first
COPY genesis-api/pom.xml genesis-api/
COPY genesis-common/pom.xml genesis-common/
COPY genesis-user/pom.xml genesis-user/
COPY genesis-workspace/pom.xml genesis-workspace/
COPY genesis-coref/pom.xml genesis-coref/
COPY genesis-import-export/pom.xml genesis-import-export/
COPY genesis-infra/pom.xml genesis-infra/

# Create source directories (to avoid Maven errors)
RUN mkdir -p genesis-api/src/main/java \
    genesis-common/src/main/java \
    genesis-user/src/main/java \
    genesis-workspace/src/main/java \
    genesis-coref/src/main/java \
    genesis-import-export/src/main/java \
    genesis-infra/src/main/java

# Install local dependencies first
RUN mvn -B install -N

# Copy all source code
COPY . .

# Package all modules
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built artifacts from the builder stage
COPY --from=builder /app/genesis-api/target/genesis-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]