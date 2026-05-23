<h1 align="center">Genesis Backend</h1>

<p align="center">
  <strong>A modular monolith backend for the Genesis NLP annotation platform.</strong>
  <br/>
  Coreference resolution, named-entity recognition, part-of-speech tagging, and word-sense disambiguation — built on Spring Boot 3 with PostgreSQL and Flyway migrations.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3.3" src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white">
  <img alt="PostgreSQL 15" src="https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white">
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/runtime-Docker-2496ED?logo=docker&logoColor=white">
</p>

<p align="center">
  <a href="#overview">Overview</a> ·
  <a href="#features">Features</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#quick-start">Quick start</a> ·
  <a href="#configuration">Configuration</a> ·
  <a href="#api">API</a> ·
  <a href="#testing">Testing</a> ·
  <a href="#deployment">Deployment</a> ·
  <a href="#contributing">Contributing</a>
</p>

---

## Overview

Genesis is a full-stack NLP annotation platform for linguistics teams and ML data ops. This repository contains the **backend**: a Spring Boot 3 modular monolith that exposes a REST + WebSocket API consumed by the [Genesis frontend](https://github.com/gautam84/genesis-frontend).

Highlights:

- **Modular monolith** — fourteen Maven modules with clean boundaries, event-driven communication, and per-module health checks.
- **Multi-task annotation** — coreference (mentions + clusters), NER (with nested-span BIO round-trip), POS tagging, WSD (sense inventory + annotations).
- **Workspace-scoped access control** — `WorkspaceAccessControl` gates every workspace and document mutation behind `requireMember` / `requireAdmin`.
- **Stateless JWT auth** — `@ConfigurationProperties`-validated secret (≥256 bits, HS256-pinned), short-lived access tokens, rotating refresh tokens.
- **Flyway-managed schema** — `V1` baseline snapshot, `V2+` explicit ALTERs; `validate` mode prevents silent drift in prod.
- **Spotless + JUnit 5** — Google Java Format enforcement and 200+ tests across modules.

## Features

| Domain | Module | Capabilities |
|---|---|---|
| **Auth** | `genesis-user`, `genesis-infra` | Signup, email verification, JWT issue/refresh, rotation, BCrypt(12) password hashing |
| **Workspaces** | `genesis-workspace` | CRUD, member roles (`ADMIN` / `ANNOTATOR` / `CURATOR`), event publishing |
| **Documents** | `genesis-workspace`, `genesis-import-export` | Upload, Cloudinary storage, async tokenization, CoNLL-2012 import/export |
| **Coreference** | `genesis-coref` | Mentions, clusters, cluster compaction, progress tracking |
| **NER** | `genesis-ner` | Tag definitions, nested spans, BIO round-trip with OntoNotes 18 |
| **POS** | `genesis-pos` | Tag set, per-annotator overrides, majority-vote export |
| **WSD** | `genesis-wsd` | Sense inventory, annotations, export |
| **Editor** | `genesis-editor` | Session persistence (scroll, last-doc index, sentence pagination) |
| **Recommendations** | `genesis-recommend` | Active-learning hints surfaced in the editor |
| **Notifications** | `genesis-notification` | In-app + STOMP WebSocket events, origin-locked handshake |
| **Sharing** | — | Signed share-link tokens for read-only CoNLL export |

## Architecture

### System view

The platform is a single deployable JAR composed of independent Maven modules. Cross-module communication is **event-driven** — no module calls another's service directly.

![System architecture](./images/system_architecture.png)

### Module structure

```text
genesis-backend/
├── genesis-api/             # @SpringBootApplication, wires all modules
├── genesis-common/          # BaseEntity, ApiResponse<T>, exceptions, TextProcessor
├── genesis-infra/           # JWT, Spring Security, Cloudinary, CORS, request logging
├── genesis-user/            # User entity + signup
├── genesis-workspace/       # Workspace, document, member lifecycle (+ access control)
├── genesis-coref/           # Coreference mentions and clusters
├── genesis-ner/             # Named-entity recognition (nested spans, BIO)
├── genesis-pos/             # Part-of-speech tagging
├── genesis-wsd/             # Word-sense disambiguation
├── genesis-editor/          # Per-user editor sessions
├── genesis-import-export/   # TXT + CoNLL-2012 + ZIP workspace export
├── genesis-notification/    # Notifications, WebSocket + STOMP
├── genesis-recommend/       # Active-learning recommendations
└── genesis-logging/         # Annotation audit log
```

Each module follows the same layout:

```text
module/src/main/java/com/genesis/<module>/
├── <Module>ModuleConfig.java   # @Configuration imported by genesis-api
├── controller/                 # Thin REST layer — delegates to service
├── service/                    # @Transactional business logic
├── repository/                 # Spring Data JPA interfaces
├── entity/                     # JPA entities extending BaseEntity
├── dto/                        # Request/response objects (never crossed across modules)
├── event/                      # ApplicationEvent subclasses for cross-module signals
└── health/                     # HealthIndicator per module
```

### Database schema

Schema is owned by Flyway under `genesis-api/src/main/resources/db/migration/`. `V1` is a `pg_dump` baseline; `V2+` are explicit ALTERs. Hibernate runs in `ddl-auto=validate` mode against prod to refuse boot on drift.

![ER diagram](./images/erDiagram.png)

### Key design decisions

| Decision | Rationale |
|---|---|
| Modular monolith | Domain isolation without microservice overhead. Single deploy, single DB transaction boundary. |
| Spring `ApplicationEvent` for cross-module comms | Loose coupling, replaceable with Kafka/Redis later without rewriting business code. |
| `WorkspaceAccessControl` component | Single source of truth for "is the caller a member / admin of this workspace". Reused by `WorkspaceService` and `DocumentService`. |
| `*Internal` service variants | Trusted server-to-server flows (share-token export, async tokenization, annotation cascade) bypass auth explicitly via clearly-named methods. |
| Flyway over `ddl-auto=update` | Reviewable migrations; refuses silent schema drift in prod. |
| Two-layer JWT secret validation | `@Validated` `@NotBlank @Size(min=32)` on the property + `IllegalStateException` in `JwtTokenProvider` constructor. |

## Quick start

### Prerequisites

- **Java 21** (`java --version` → `openjdk 21`)
- **Maven 3.9+** (wrapper `./mvnw` ships with the repo)
- **Docker** + **Docker Compose** (for PostgreSQL)
- **Cloudinary** account (free tier works) for file storage

### Run with Docker (full stack)

```bash
git clone https://github.com/subarnasaikia/genesis.git
cd genesis
cp env.example .env
# edit .env — set JWT_SECRET, CLOUDINARY_*, DB_PASSWORD
docker-compose up --build
```

Backend listens on `http://localhost:8080`.

### Local development

```bash
# 1. Database only
docker-compose up -d postgres

# 2. Configure env
cp env.example .env
# Generate a strong JWT secret:
openssl rand -base64 48 | tr -d '\n=' | head -c 64

# 3. Build + run
./mvnw clean install -DskipTests
./mvnw spring-boot:run -pl genesis-api
```

Probe it:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Configuration

All configuration is environment-variable driven via Spring Boot property resolution. Copy `env.example` to `.env` — `spring-dotenv` loads it at boot in non-prod profiles.

### Required

| Variable | Notes |
|---|---|
| `JWT_SECRET` | **At least 32 ASCII chars** (256-bit HS256). Boot fails fast otherwise. Generate with `openssl rand -base64 48 \| tr -d '\n='`. |
| `DB_URL` / `DATABASE_URL` | `jdbc:postgresql://host:5432/genesis` (or Railway's `DATABASE_URL`). |
| `DB_USERNAME` / `PGUSER` | DB user. |
| `DB_PASSWORD` / `PGPASSWORD` | DB password. |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary config. |
| `CLOUDINARY_API_KEY` | Cloudinary config. |
| `CLOUDINARY_API_SECRET` | Cloudinary config. |

### Required in `prod` profile

| Variable | Notes |
|---|---|
| `CORS_ALLOWED_ORIGINS` | Comma-separated origin list. `application-prod.properties` drops the dev localhost fallback, so missing this fails boot loudly. |
| `SPRING_PROFILES_ACTIVE=prod` | Activates prod overrides (Actuator restriction, no SQL logging, Flyway baseline-on-migrate=false, larger pool). |

### Optional

| Variable | Default | Notes |
|---|---|---|
| `JWT_ACCESS_TOKEN_EXPIRY` | `15m` | Spring `Duration` (e.g. `30s`, `2h`, `14d`). |
| `JWT_REFRESH_TOKEN_EXPIRY` | `7d` | Spring `Duration`. |
| `PORT` | `8080` | HTTP listen port. |

### Profiles

| Profile | Behaviour |
|---|---|
| `dev` (default) | Verbose SQL logging, `*` Actuator exposure, Flyway `baseline-on-migrate=true`, dev-friendly localhost CORS fallback, pool 5/1. |
| `prod` | Actuator restricted to `health,info,metrics,prometheus` with `show-details=when-authorized`, SQL logging off, `com.genesis=INFO`, `flyway.baseline-on-migrate=false`, pool 10/2, `CORS_ALLOWED_ORIGINS` mandatory. |

## API

All endpoints return a uniform envelope:

```json
{ "success": true, "data": { ... }, "message": "...", "timestamp": "2026-05-24T..." }
```

Public REST groups:

| Prefix | Module | Auth |
|---|---|---|
| `/api/auth/**` | `genesis-user` + `genesis-infra` | Signup/login/refresh are public; everything else requires JWT |
| `/api/workspaces/**` | `genesis-workspace` | Member (read) or Admin (write) of the workspace |
| `/api/workspaces/{id}/documents`, `/api/documents/**` | `genesis-workspace` | Member (read + status), Admin (delete) |
| `/api/coref/**`, `/api/ner/**`, `/api/pos/**`, `/api/wsd/**` | `genesis-coref` / `-ner` / `-pos` / `-wsd` | Authenticated; workspace-scoped checks rolling out under P1 |
| `/api/editor/**` | `genesis-editor` | Authenticated |
| `/api/export/**` | `genesis-import-export` | Member of the workspace |
| `/api/public/export/**` | — | Signed JWT share token (no session) |
| `/api/notifications/**` | `genesis-notification` | Authenticated |
| `/ws` (STOMP) | `genesis-notification` | JWT validated via `WebSocketAuthInterceptor`; origin locked to `cors.allowed-origins` |

Detailed Postman collections live in [`docs/api/`](./docs/api/). OpenAPI/Swagger publication is tracked as a P2 follow-up.

## Testing

```bash
./mvnw test                                # full suite
./mvnw test -pl genesis-coref              # single module
./mvnw test jacoco:report                  # with coverage report
./mvnw spotless:check                      # format check
./mvnw spotless:apply                      # auto-format
```

Test layout follows production layout — unit tests for services, repository tests with `@DataJpaTest`, full Spring boot smoke test (`GenesisApplicationTests`). H2 in-memory replaces Postgres for tests; Flyway is disabled for the test profile and Hibernate `create-drop` builds the schema from entities.

## Deployment

A `docker-compose.yml` at the repo root provisions Postgres + the app. For a managed deploy:

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Provide all **Required + Required in prod** env vars above.
3. Provision Postgres separately; expose its URL via `DATABASE_URL` (Railway-style) or `DB_URL`.
4. The first migration run records the V1 baseline. After that `baseline-on-migrate=false` keeps drift from being silently re-baselined.

The repo is currently deployed to Railway via `Dockerfile` + the Railway PostgreSQL plugin.

## Security

Recent audit-driven hardening (see [`AUDIT_TODO.md`](./AUDIT_TODO.md) for the full backlog):

- ✅ Hardcoded JWT fallback removed; secret validated at boot and re-checked in `JwtTokenProvider`
- ✅ `DebugController` removed; `/api/debug/**` allowlist deleted from Spring Security
- ✅ Workspace + Document mutations IDOR-gated by `WorkspaceAccessControl`
- ✅ Actuator restricted under `prod`; SQL + DEBUG logging silenced
- ✅ WebSocket handshake honours `cors.allowed-origins` (no more `setAllowedOriginPatterns("*")`)
- ✅ JWT algorithm pinned to HS256

Open follow-ups: rate-limiting on auth endpoints, security response headers (CSP/HSTS), refresh-token rotation on use. Tracked in `AUDIT_TODO.md` P1.

Found a vulnerability? File a private security advisory via the repo's **Security** tab rather than a public issue.

## Contributing

Issues and PRs welcome.

- Branch from `main`; one task per branch, one PR per branch.
- Run `./mvnw spotless:apply` before pushing.
- Tests live alongside the code they cover. Add coverage for new branches.
- The developer guide in [`docs/developer-guide.md`](./docs/developer-guide.md) covers module conventions in depth.

Commit messages follow the loose Conventional Commits shape used in this repo (`feat:`, `fix:`, `docs:`, `security:`, `ops:`).

## License

License TBD — repository is currently private/source-available. Reach out to the maintainer before redistribution.

## Acknowledgments

- Schema and label set inspired by **OntoNotes 5.0** and **CoNLL-2012**.
- Built with [Spring Boot](https://spring.io/projects/spring-boot), [Flyway](https://flywaydb.org/), [JJWT](https://github.com/jwtk/jjwt), and [Cloudinary](https://cloudinary.com/).

---

<p align="center">
  Frontend repo: <a href="https://github.com/gautam84/genesis-frontend">gautam84/genesis-frontend</a>
</p>
