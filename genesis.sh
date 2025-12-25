#!/bin/bash

# Genesis Project Management Script

COMMAND=$1

case "$COMMAND" in
  "build")
    echo "Building the project..."
    ./genesis-api/mvnw clean install -DskipTests
    ;;
  "clean")
    echo "Cleaning the project..."
    ./genesis-api/mvnw clean
    ;;
  "test")
    echo "Running all tests..."
    ./genesis-api/mvnw test -DargLine="-Dnet.bytebuddy.experimental=true"
    ;;
  "install")
    echo "Installing the project (with tests)..."
    ./genesis-api/mvnw clean install -DargLine="-Dnet.bytebuddy.experimental=true"
    ;;
  "run")
    echo "Running the application..."
    # Load environment variables from .env file
    if [ -f .env ]; then
      export $(grep -v '^#' .env | xargs)
    fi
    ./genesis-api/mvnw spring-boot:run -pl genesis-api
    ;;
  "db")
    echo "Starting PostgreSQL database..."
    docker-compose up -d postgres
    echo "PostgreSQL is starting on port 5432..."
    echo "Use './genesis.sh db-stop' to stop the database"
    ;;
  "db-stop")
    echo "Stopping PostgreSQL database..."
    docker-compose down
    ;;
  "docker-build")
    echo "Building Docker image..."
    docker build -t genesis .
    ;;
  "docker-run")
    echo "Running Docker container..."
    docker run -p 8080:8080 genesis
    ;;
  *)
    echo "Usage: ./genesis.sh [command]"
    echo "Commands:"
    echo "  build        - Build the entire project (skips tests)"
    echo "  clean        - Clean build artifacts"
    echo "  test         - Run all unit and integration tests"
    echo "  install      - Clean install (runs tests and builds artifacts)"
    echo "  run          - Run the application locally"
    echo "  db           - Start PostgreSQL database (docker-compose)"
    echo "  db-stop      - Stop PostgreSQL database"
    echo "  docker-build - Build the Docker image"
    echo "  docker-run   - Run the application in Docker"
    exit 1
    ;;
esac

