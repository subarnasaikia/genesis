# 🤖 Agent Context — Genesis Project Overview

This document provides **complete context** about the `Genesis` project for AI coding assistants (e.g., GitHub Copilot, Claude Code, Gemini CLI, ChatGPT Code Interpreter).
It includes architecture design, module responsibilities, coding conventions, file organization, and Docker setup — enabling the AI to understand, extend, and write coherent code for the project.

---

## 🧠 Project Summary

**Genesis** is a **modular monolithic NLP annotation platform**, built with **Spring Boot**, **PostgreSQL**, and **Next.js** (frontend).
The backend enables annotation workflows such as **coreference resolution**, with future support for POS, NER, and more.

Architecture follows **Clean Architecture** and **SOLID principles**, making each module self-contained and extensible.

---

## ⚙️ Tech Stack

| Layer                  | Technology                         |
| ---------------------- | ---------------------------------- |
| **Backend**            | Java 17+, Spring Boot 3            |
| **Database**           | PostgreSQL 15                      |
| **ORM**                | Spring Data JPA                    |
| **Build Tool**         | Maven (multi-module)               |
| **Containerization**   | Docker + Docker Compose            |
| **Authentication**     | OAuth2 / JWT                       |
| **Frontend**           | Next.js (not part of backend repo) |
| **Migration Tool**     | Flyway (planned)                   |
| **Formatting / Style** | Spotless + Google Java Format      |
| **CI/CD**              | (Planned) GitHub Actions / Jenkins |

---

## 🧱 Architecture Overview

Genesis uses a **modular monolith** architecture:

```
genesis/
├── genesis-api/             # Main entry point (Spring Boot)
├── genesis-common/          # Shared kernel, base models, utilities
├── genesis-user/            # Auth and user management
├── genesis-workspace/       # Project and file management
├── genesis-coref/           # Coreference annotation logic
├── genesis-import-export/   # File import/export (TXT, CONLL, XMI)
├── genesis-infra/           # Infrastructure and adapters (DB, file storage)
└── pom.xml                  # Parent Maven config (packaging=pom)
```

### 🧩 Each module has:

```
module-name/
├── api/             # REST controllers, DTOs
├── application/     # Services (business logic)
├── domain/          # Entities, value objects, interfaces
├── infrastructure/  # Repositories, adapters, mappers
└── exceptions/      # Custom exceptions
```

---

## 📦 Parent POM (Root)

Parent `pom.xml` controls dependency versions and module inclusion.

```xml
<project>
  <groupId>com.genesis</groupId>
  <artifactId>genesis</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>genesis-api</module>
    <module>genesis-common</module>
    <module>genesis-user</module>
    <module>genesis-workspace</module>
    <module>genesis-coref</module>
    <module>genesis-import-export</module>
    <module>genesis-infra</module>
  </modules>

  <properties>
    <java.version>17</java.version>
    <spring.boot.version>3.3.3</spring.boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

---

## 🧩 Module Summaries

### 🧱 **genesis-api**

* Spring Boot application entry point.
* `GenesisApplication.java` runs the app.
* Handles routing, REST controllers, and configuration.
* Depends on all other modules.

Example:

```java
@SpringBootApplication(scanBasePackages = "com.genesis")
public class GenesisApplication {
    public static void main(String[] args) {
        SpringApplication.run(GenesisApplication.class, args);
    }
}
```

---

### 🧱 **genesis-common**

* Shared kernel of reusable components.
* Contains text-processing interfaces and domain value objects.

Example classes:

```java
public class Token {
    private final String text;
    private final int startOffset;
    private final int endOffset;
}

public interface TextProcessor {
    DocumentText process(String rawText);
}
```

---

### 🧱 **genesis-user**

* User authentication and role management.
* OAuth2 + JWT integration.
* Tables: `users`, `roles`.
* Exposes REST endpoints for auth.

Example endpoint:

```java
@PostMapping("/login")
public ResponseEntity<UserDto> login(@RequestBody LoginRequest req) {
    return ResponseEntity.ok(userService.authenticate(req));
}
```

---

### 🧱 **genesis-workspace**

* Manages projects, workspaces, and file metadata.
* Handles user assignments and upload logic.

Workflow example:

```
Workspace.upload(file)
  → FileStorageService.upload()
  → DocumentUploadService.createDocument()
  → ImportService.prepareForAnnotation()
```

---

### 🧱 **genesis-coref**

* Coreference annotation logic (mentions, clusters, history).
* CRUD APIs for annotation.
* Interacts with Import/Export module for data initialization and export.

Example entity:

```java
@Entity
public class Mention {
    @Id @GeneratedValue
    private UUID id;
    private UUID documentId;
    private int startOffset;
    private int endOffset;
}
```

---

### 🧱 **genesis-import-export**

* Handles format conversions and parsing.
* Supported: TXT, CoNLL-2012 (future: XMI).
* Uses `TextProcessor` from common module.

Example:

```java
@Service
public class ImportServiceImpl implements ImportService {
    public void prepareForAnnotation(UUID docId) {
        String text = fileStorage.download(docId);
        DocumentText processed = textProcessor.process(text);
        corefPort.saveDocumentStructure(docId, processed);
    }
}
```

---

### 🧱 **genesis-infra**

* Manages infrastructure services.
* Provides adapters for PostgreSQL, File Storage, Kafka, etc.

Example adapter:

```java
@Service
public class LocalDiskStorageAdapter implements FileStorageService {
    public String upload(MultipartFile file) { /* save locally */ }
}
```

---

## 🧩 Module Communication Example

### Upload Flow

```
API → WorkspaceService → ImportService → Common.TextProcessor → CorefPersistenceAdapter
```

* `genesis-api` receives request
* `genesis-workspace` handles file upload
* `genesis-import-export` parses file
* `genesis-common` segments text
* `genesis-coref` stores annotations

---

## 🐳 Docker Setup

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY pom.xml .
COPY genesis-api/pom.xml genesis-api/pom.xml
COPY genesis-common/pom.xml genesis-common/pom.xml
COPY genesis-coref/pom.xml genesis-coref/pom.xml
COPY genesis-workspace/pom.xml genesis-workspace/pom.xml
COPY genesis-import-export/pom.xml genesis-import-export/pom.xml
COPY genesis-infra/pom.xml genesis-infra/pom.xml
RUN mvn -B dependency:resolve dependency:resolve-plugins
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/genesis-api/target/genesis-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    container_name: genesis-postgres
    restart: always
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  genesis-app:
    build: .
    container_name: genesis-app
    depends_on:
      - postgres
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - .:/app

volumes:
  postgres_data:
```

### .env file

```bash
POSTGRES_DB=genesis
POSTGRES_USER=genesis_user
POSTGRES_PASSWORD=supersecret
SPRING_PROFILES_ACTIVE=dev
```

---

## 🌱 Spring Profiles

| Profile | Description                                            |
| ------- | ------------------------------------------------------ |
| `dev`   | Local development, auto schema updates, verbose logs   |
| `prod`  | Production, migrations via Flyway, stricter validation |

`application.yml` auto-loads based on profile:

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

---

## 🧩 Coding Conventions

| Type             | Convention                                |
| ---------------- | ----------------------------------------- |
| **Formatting**   | Google Java Format (Spotless)             |
| **Tests**        | JUnit + Mockito                           |
| **DTOs**         | Used for API payloads only                |
| **Entities**     | Pure JPA entities in domain layer         |
| **Services**     | Contain transactional business logic      |
| **Controllers**  | Thin layer delegating to services         |
| **Repositories** | In infrastructure layer only              |
| **Interfaces**   | Cross-module communication only via ports |

---

## 🧠 Example Code Flow

### Upload & Parse Document

```java
@PostMapping("/upload")
public ResponseEntity<?> upload(@RequestParam MultipartFile file) {
    workspaceService.uploadFile(file);
    return ResponseEntity.ok("File processed");
}
```

`workspaceService.uploadFile()` →

* Uploads to FileStorage
* Persists metadata
* Calls `importService.prepareForAnnotation()`

`importService.prepareForAnnotation()` →

* Downloads file from storage
* Uses `TextProcessor.process()` (from `common`)
* Persists sentence/token structure via `corefPersistencePort`

✅ Result: Document is parsed, tokenized, and ready for annotation.

---

## 🔒 Secrets & Configuration

* Secrets stored in `.env` (development)
* For production: use Docker Secrets or Vault
* Environment variables are injected into Spring automatically via `${VAR}` syntax.

---

## 🧩 Developer Workflow

1. Clone repository
2. Build project

   ```bash
   mvn clean install
   ```
3. Run locally

   ```bash
   mvn -pl genesis-api spring-boot:run
   ```
4. Run via Docker

   ```bash
   docker compose up --build
   ```
5. API available at → [http://localhost:8080](http://localhost:8080)

---

## ✅ Design Principles Summary

* **Clean Architecture**: domain-driven, layered structure
* **SOLID Principles**: independent, replaceable modules
* **Separation of Concerns**: distinct roles for API, domain, infra
* **Port & Adapter Pattern**: well-defined communication interfaces
* **Profile-driven Config**: different behavior in dev vs prod

---

## 📘 Notes for AI Coding Agents

* All code should follow **Spring Boot conventions**.
* Avoid circular dependencies between modules.
* For new annotation types → create a new module (`genesis-<type>`).
* Use existing examples in `genesis-coref` and `genesis-import-export`.
* Prefer using `TextProcessor` interface for all text parsing.
* Use environment variables for DB and secret management.
* Default DB entity naming should follow snake_case.


