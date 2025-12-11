# 🧠 Genesis — NLP Annotation Platform

Genesis is a **modular monolithic** NLP annotation platform designed for extensibility and clarity.
It supports multi-user projects, file-based annotation workflows, and annotation types such as **coreference resolution**.

Built using **Spring Boot (Java)**, **PostgreSQL**, and **Next.js** (frontend), Genesis follows a simplified modular architecture.

---

## 🚀 Key Features

* 🧑‍💻 **User & Role Management** (OAuth2, JWT)
* 🧰 **Workspace & Project Management**
* 🧉 **Coreference Annotation Module**
* 📄 **Import/Export Framework** supporting TXT, CoNLL-2012, and (future) XMI formats
* ⚙️ **Shared Kernel** with reusable domain objects and text processing interfaces
* 🗄️ **PostgreSQL Database**
* ☁️ **Docker-based Deployment** (Postgres + App)
* 🧱 **Simplified Modular Architecture**
* 🔒 **Environment Profiles (Dev/Prod) & Secrets Management**

---

## 🧰 Tech Stack

| Layer                 | Technology                      |
| --------------------- | ------------------------------- |
| **Backend**           | Java 21, Spring Boot 3          |
| **Database**          | PostgreSQL 15                   |
| **Build Tool**        | Maven (multi-module)            |
| **Containerization**  | Docker, Docker Compose          |
| **Frontend**          | Next.js (separate project)      |
| **ORM**               | Spring Data JPA                 |
| **Authentication**    | OAuth2 / JWT                    |
| **Config Management** | Spring Profiles (`dev`, `prod`) |

---

## 🎟️ Architecture Overview

The project is structured as a multi-module Maven project.
Files are located in the root of each module package for simplicity.

```
genesis/
├── genesis-api/             # Application entry point (Spring Boot)
├── genesis-common/          # Shared kernel (models, utils)
├── genesis-user/            # User/Auth management
├── genesis-workspace/       # Workspaces and file management
├── genesis-coref/           # Coreference annotation logic
├── genesis-import-export/   # File import/export logic
├── genesis-infra/           # Infrastructure configurations
├── data/                    # Local dev data (gitignored)
└── pom.xml                  # Parent Maven config
```

---

## 🚢 How to Run

### Option 1: Docker Compose (Recommended)

Run the entire stack (Database + Application) in containers.

```bash
docker-compose up --build
```
*   App: http://localhost:8080
*   DB: `postgres:5432`

### Option 2: Local Development

1.  Start the database:
    ```bash
    docker-compose up -d postgres
    ```

2.  Run the application using the Maven wrapper (located in `genesis-api`):
    ```bash
    # From project root directory
    
    # 1. Build and install all modules locally
    .\genesis-api\mvnw.cmd install -DskipTests

    # 2. Run the application
    .\genesis-api\mvnw.cmd spring-boot:run -pl genesis-api
    ```
    (On Linux/Mac use `./genesis-api/mvnw`)

---

## 🛠️ Module Responsibilities

*   **genesis-common**: Shared POJOs (`DocumentText`, `Token`) and interfaces (`TextProcessor`).
*   **genesis-user**: Users, Roles, Auth.
*   **genesis-workspace**: Projects, Documents usage.
*   **genesis-coref**: Coreference mentions and clusters.
*   **genesis-import-export**: Parsing logic (CoNLL, etc).
*   **genesis-infra**: Database config, file storage.
*   **genesis-api**: REST Controllers and Main application class.

---

## 🌍 Environment Variables

Create a `.env` file in the root if you need to override defaults, or rely on `application.yml` defaults for development.

---

## ⚡ Useful Commands

Run these from the project root (`d:\genesis`):

| Action | Command |
| :--- | :--- |
| **Clean Build** | `.\genesis-api\mvnw.cmd clean` |
| **Install (Build All)** | `.\genesis-api\mvnw.cmd install -DskipTests` |
| **Run App** | `.\genesis-api\mvnw.cmd spring-boot:run -pl genesis-api` |
| **Run Tests** | `.\genesis-api\mvnw.cmd test` |
| **Package JARs** | `.\genesis-api\mvnw.cmd package -DskipTests` |
