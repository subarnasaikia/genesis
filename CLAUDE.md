# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules (skip tests for speed)
mvn clean install -DskipTests

# Run the application (requires DB — see Docker section below)
mvn spring-boot:run -pl genesis-api

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl genesis-coref

# Run tests + generate JaCoCo coverage report
mvn test jacoco:report

# Format code (Google Java Format via Spotless)
mvn spotless:apply

# Check formatting without applying
mvn spotless:check

# Start only the database
docker-compose up -d postgres

# Full stack via Docker
docker-compose up --build
```

Application runs on `http://localhost:8080`.

## Architecture

**Modular monolith** — nine Maven modules, all under the root `pom.xml`. `genesis-api` is the only runnable module; it imports all others via `@Import` and registers their entities/repositories via `@EntityScan` / `@EnableJpaRepositories`.

### Module Responsibilities

| Module | Package prefix | Role |
|---|---|---|
| `genesis-api` | `com.genesis.api` | Entry point only — `GenesisApplication`, `AuditorAware` bean |
| `genesis-common` | `com.genesis.common` | `BaseEntity`, `ApiResponse<T>`, exceptions, `TextProcessor` interface, `Token`/`DocumentText` value objects |
| `genesis-user` | `com.genesis.user` | User CRUD and signup; owns refresh-token persistence (`RefreshToken`/`RefreshTokenService`). No JWT/Spring Security logic |
| `genesis-workspace` | `com.genesis.workspace` | Workspace/document/member lifecycle; publishes domain events |
| `genesis-coref` | `com.genesis.coref` | Mention and cluster CRUD for coreference annotation |
| `genesis-editor` | `com.genesis.editor` | Per-user `EditorSession` (last document index, scroll position) |
| `genesis-import-export` | `com.genesis.importexport` | TXT/CoNLL-2012 import, tokenization, CoNLL export; owns `SentenceEntity` and `TokenEntity` |
| `genesis-notification` | `com.genesis.notification` | In-app notifications, WebSocket config, event listener |
| `genesis-infra` | `com.genesis.infra` | JWT (`JwtTokenProvider`), Spring Security config, Cloudinary file storage, request logging, CORS. Depends on no domain module — consumes the `UserDetailsService` interface, wired in `genesis-api` |

### Within Each Module

```
module/src/main/java/com/genesis/<module>/
├── <Module>ModuleConfig.java   # @Configuration class imported by genesis-api
├── controller/                 # REST layer — thin, delegates to service
├── service/                    # @Transactional business logic
├── repository/                 # Spring Data JPA interfaces
├── entity/                     # JPA entities extending BaseEntity
├── dto/                        # Request/response objects only; never pass entities across modules
├── event/                      # ApplicationEvent subclasses for cross-module signals
└── health/                     # HealthIndicator per module
```

### Cross-Module Communication

**Modules never call each other's services directly.** Use Spring `ApplicationEventPublisher` to publish events; other modules listen with `@EventListener` or `@TransactionalEventListener`.

Key events published by `genesis-workspace`:
- `DocumentUploadedEvent` → triggers `ImportService` (tokenization)
- `DocumentTokenizedEvent` → updates document status
- `MemberAddedEvent` → triggers `NotificationService`
- `DocumentDeletedEvent`, `WorkspaceDeletedEvent`, `WorkspaceCreatedEvent`

**Boundary enforcement:** `ModuleBoundaryTest` (in `genesis-api`, ArchUnit) fails the
build if any module outside `genesis-api`/`genesis-common` depends on another module's
`..repository..`/`..entity..` package. Existing debt is frozen in a committed baseline
(`genesis-api/src/test/resources/archunit_store`); **new** cross-module data-access
reaches fail the build. When you genuinely need another module's data, publish/consume an
event or define an outbound port in the consuming module with the adapter wired in
`genesis-api` (see `RecipientDirectory`/`UserDetailsServiceImpl`). To re-baseline after a
legitimate boundary fix: `mvn test -pl genesis-api -Dtest=ModuleBoundaryTest -Darchunit.freeze.refreeze=true`.

**Boundary tooling (Claude Code):** `.claude/skills/boundary-fix` packages the full
workflow (event-vs-port decision, the `@TransactionalEventListener(AFTER_COMMIT)` +
`@Transactional(REQUIRES_NEW)` trap, full-reactor verify, baseline refreeze). A
`PostToolUse` hook (`.claude/hooks/check-module-boundary.sh`) warns at edit-time when a
feature module's `src/main` imports another module's `repository`/`entity`; a `PreToolUse`
hook blocks edits to `.env*`. For a focused pre-merge pass, run the `module-boundary-reviewer`
subagent. These are heuristics — `ModuleBoundaryTest` remains the source of truth.

### Adding a New Module

1. Create Maven module with a `<Module>ModuleConfig.java` `@Configuration` class.
2. Add it to the root `pom.xml` `<modules>` list.
3. In `genesis-api/GenesisApplication.java`, add to `@EntityScan`, `@EnableJpaRepositories`, and `@Import`.
4. Use `genesis-coref` as the reference implementation.

## Key Patterns

**All entities extend `BaseEntity`** (`com.genesis.common.entity.BaseEntity`) which provides `id` (UUID), `createdAt`, `updatedAt`, `createdBy`, `updatedBy` via JPA auditing.

**All REST responses use `ApiResponse<T>`** (`com.genesis.common.response.ApiResponse`) — a wrapper with `success`, `data`, `message`, `timestamp`. Controllers should always return this.

**Exception hierarchy** (`com.genesis.common.exception`):
- `GenesisException` — base
- `ResourceNotFoundException` → 404
- `UnauthorizedException` → 401/403
- `ValidationException` → 400

**DB naming:** snake_case for all tables and columns. Enum columns stored as `STRING`.

**Tokenization flow:**
```
POST /api/workspaces/{id}/documents  (file upload)
  → DocumentService saves file to Cloudinary
  → publishes DocumentUploadedEvent
  → ImportService.prepareForAnnotation() listens
  → UnicodeTokenizer + LineSentenceSegmenter (from common TextProcessor)
  → saves TokenEntity + SentenceEntity rows
  → publishes DocumentTokenizedEvent
  → DocumentService updates status to TOKENIZED
```

**Security:** JWT filter lives in `genesis-infra` (`JwtAuthenticationFilter`). The `genesis-user` module handles user data; `genesis-api` wires them together via Spring Security's `UserDetailsService`.

**Async processing:** `genesis-infra` configures a task executor (`AsyncConfig`). Use `@Async` for background work like tokenization.

**Pluggable file storage:** raw uploads go through `FileStorageService`, which delegates to a
`FileStorageBackend` chosen per deployment via `genesis.storage.provider`:
- `cloudinary` (default) — `CloudinaryService`, uploads to Cloudinary, downloads by HTTP.
- `local` — `LocalDiskStorageBackend`, writes under `genesis.storage.local.base-path`; the
  stored URL is `local:{key}` and downloads read from disk. Needs a durable, single-writer
  volume (ephemeral/multi-instance hosts must use cloudinary).

Backends are selected with `@ConditionalOnProperty`; exactly one is active. After tokenization,
only re-tokenize reads the raw source (editor/annotation/export use DB token rows), so
`genesis.storage.retain-source=false` deletes the source once `DocumentTokenizedEvent` fires —
`DocumentSourceRetentionListener` clears the FK (via `DocumentSourceReclaimer`, a separate
`REQUIRES_NEW` tx) then deletes the file.

## Environment Variables

Copy `env.example` to `.env`. Required variables:

| Variable | Purpose |
|---|---|
| `DB_URL` or `DATABASE_URL` | JDBC URL (`jdbc:postgresql://...`) |
| `DB_USERNAME` / `PGUSER` | DB user |
| `DB_PASSWORD` / `PGPASSWORD` | DB password |
| `JWT_SECRET` | HMAC secret for signing tokens |
| `STORAGE_PROVIDER` | `cloudinary` (default) or `local` |
| `STORAGE_RETAIN_SOURCE` | Keep raw upload after tokenization (default `true`) |
| `STORAGE_LOCAL_BASE_PATH` | Local-provider upload dir (default `./data/uploads`) |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary config (only when provider=cloudinary) |
| `CLOUDINARY_API_KEY` | Cloudinary config (only when provider=cloudinary) |
| `CLOUDINARY_API_SECRET` | Cloudinary config (only when provider=cloudinary) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (default: `http://localhost:3000`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (ddl-auto=update) or `prod` |
| `PORT` | HTTP port (default: 8080) |

## API Documentation

Postman collections in `docs/api/`. Spring Actuator endpoints available at `/actuator/*` — all exposed in current config.
