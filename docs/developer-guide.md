# Developer Guide

## Prerequisite
- Java 21+
- Docker (optional)

## Project Management Scripts
We provide cross-platform scripts to manage standard development tasks.
- **Windows**: `genesis.bat`
- **Linux/Mac**: `genesis.sh` (ensure you run `chmod +x genesis.sh` first)

### Common Commands

| Task | Command | Description |
|------|---------|-------------|
| **Build** | `genesis build` | Compiles the project and builds JARs (skips tests). |
| **Clean** | `genesis clean` | Removes `target` directories. |
| **Test** | `genesis test` | Runs all unit and integration tests across modules. |
| **Install** | `genesis install` | Runs full clean install (includes tests). |
| **Run** | `genesis run` | Starts the Spring Boot application locally on port 8080. |
| **Docker Build** | `genesis docker-build` | Builds a Docker image named `genesis`. |
| **Docker Run** | `genesis docker-run` | Runs the `genesis` container on port 8080. |

## Testing
We follow TDD. The project has both unit tests (per module) and integration tests (in `genesis-api`).

### How to Check Testing
Run:
```bash
genesis test
```
This wrapper executes standard Maven commands to discover and run all tests ensuring the application is healthy.

## Maven Commands Explained
The scripts wrap standard Maven commands. Here is what happens under the hood:
- `mvnw clean install`: Cleans old builds and installs artifacts to your local repository.
- `mvnw test`: Runs the test phase.
- `mvnw spring-boot:run`: Uses the Spring Boot plugin to start the app.
