@echo off
setlocal

REM Genesis Project Management Script

if "%1"=="" goto help

if "%1"=="build" goto build
if "%1"=="clean" goto clean
if "%1"=="test" goto test
if "%1"=="install" goto install
if "%1"=="run" goto run
if "%1"=="docker-build" goto docker_build
if "%1"=="docker-run" goto docker_run
if "%1"=="help" goto help

echo Unknown command: %1
goto help

:build
echo Building the project...
call .\genesis-api\mvnw.cmd clean install -DskipTests
goto end

:clean
echo Cleaning the project...
call .\genesis-api\mvnw.cmd clean
goto end

:test
echo Running all tests...
call .\genesis-api\mvnw.cmd test -DargLine="-Dnet.bytebuddy.experimental=true"
goto end

:install
echo Installing the project (with tests)...
call .\genesis-api\mvnw.cmd clean install -DargLine="-Dnet.bytebuddy.experimental=true"
goto end

:run
echo Running the application...
call .\genesis-api\mvnw.cmd spring-boot:run -pl genesis-api
goto end

:docker_build
echo Building Docker image...
docker build -t genesis .
goto end

:docker_run
echo Running Docker container...
docker run -p 8080:8080 genesis
goto end

:help
echo Usage: genesis.bat [command]
echo Commands:
echo   build        - Build the entire project (skips tests)
echo   clean        - Clean build artifacts
echo   test         - Run all unit and integration tests
echo   install      - Clean install (runs tests and builds artifacts)
echo   run          - Run the application locally
echo   docker-build - Build the Docker image
echo   docker-run   - Run the application in Docker
goto end

:end
endlocal
