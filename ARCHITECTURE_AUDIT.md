# Genesis Backend — Architecture & Clean-Code Audit

**Date:** 2026-05-22  
**Auditor:** Claude Code (senior Java / Spring Boot review)  
**Scope:** All 14 Maven modules (266 `.java` files), production sources only unless noted  
**Spring Boot version:** 3.3.3  
**Java version:** 21  

---

## Module Dependency Matrix

Legend: R = Repository access, S = Service call, E = Entity import, EV = Event only, X = None

| Consumer \ Provider | common | user | workspace | coref | import-export | infra | editor | notification | pos | logging | wsd | recommend | ner | api |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **genesis-api** | R+S | S+E+R | S | S | S | S | S | — | S | S | S | S | S | — |
| **genesis-workspace** | R | E+R | — | — | — | E (StoredFile) | — | — | — | — | — | — | — | — |
| **genesis-coref** | EV | — | — | — | E+R+EV | — | — | — | — | — | — | — | — | — |
| **genesis-import-export** | — | — | E+R+S+EV | — | — | S | — | — | — | — | — | — | — | — |
| **genesis-editor** | — | — | S | — | S+E | — | — | — | — | — | — | — | — | — |
| **genesis-notification** | — | R | EV (workspace) | — | EV | S (JWT) | — | — | — | — | — | — | — | — |
| **genesis-pos** | EV | — | E+R | — | E+R | — | — | — | — | — | — | — | — | — |
| **genesis-wsd** | — | — | E+R | — | E+R | — | — | — | — | — | — | — | — | — |
| **genesis-ner** | EV | — | E+R | — | R | — | — | — | — | — | — | — | — | — |
| **genesis-logging** | EV | — | E+R | — | — | — | — | — | — | — | — | — | — | — |
| **genesis-recommend** | EV | — | E+R | E+R | — | — | — | — | — | — | — | — | — | — |
| **genesis-infra** | — | E+R | — | — | — | — | — | — | — | — | — | — | — | — |
| **genesis-common** | — | — | — | — | — | — | — | — | — | — | — | — | — | — |

*Key: E=entity import, R=repository import, S=service call, EV=event-only dependency*

---

## ARCHITECTURE FINDINGS

---

### A-001 — CRITICAL: `genesis-coref/MentionService` directly injects `DocumentService` from `genesis-workspace`

**Severity:** Critical  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-coref/src/main/java/com/genesis/coref/service/MentionService.java:32-44`

**Issue:**  
`MentionService` holds a direct constructor-injected reference to `com.genesis.workspace.service.DocumentService` and calls `documentService.getById()`, `documentService.updateStatus()`, and `documentService.updateProgress()` on the hot path (every mention create, assign, unassign). This creates a compile-time service-layer dependency from `genesis-coref` to `genesis-workspace`, directly violating the stated architecture rule that "modules never call each other's services directly."

Consequences: a change to `DocumentService`'s method signatures breaks `genesis-coref`; the two modules cannot be developed or tested in isolation; and the coref module now participates in the workspace module's transaction implicitly.

**Fix:**  
Publish a `MentionAnnotatedEvent` (or `DocumentProgressUpdateEvent`) from `MentionService` carrying `documentId` and the new progress value. Let a listener in `genesis-workspace` respond to it and call `documentService.updateProgress()` within its own transaction.

---

### A-002 — High: Multiple annotation modules (`genesis-pos`, `genesis-wsd`, `genesis-ner`) import `genesis-workspace` entities and repositories directly

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:**  
- `genesis-pos/src/main/java/com/genesis/pos/service/PosTaggingService.java:13-15` — imports `TokenRepository`, `DocumentRepository`, `Document`  
- `genesis-wsd/src/main/java/com/genesis/wsd/service/WsdAnnotationService.java` — imports `DocumentRepository`, `WorkspaceMemberRepository`, workspace entities  
- `genesis-ner/src/main/java/com/genesis/ner/service/NerAnnotationService.java:14-15` — imports `TokenRepository`, `DocumentRepository`, `Document`

**Issue:**  
Three annotation modules reach directly into `genesis-workspace` and `genesis-import-export` repositories. This is a one-step escalation below the `genesis-coref/MentionService` violation: they avoid calling a service, but they still bypass module boundaries by importing repositories. Any schema change to `Document`, `TokenEntity`, or their repositories ripples into three annotation modules simultaneously.

**Fix:**  
Extract the necessary queries into read-only query interfaces in `genesis-common` (e.g., a `DocumentQueryPort` and `TokenQueryPort` backed by projections), or route through events / a shared DTO façade. The key is that no module outside `genesis-workspace` should hold a reference to `WorkspaceRepository`, `DocumentRepository`, or `WorkspaceMemberRepository`.

---

### A-003 — High: `genesis-recommend` imports `genesis-coref` entities and repositories directly

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-recommend/src/main/java/com/genesis/recommend/repository/ClusterChainProjectionRepository.java:28-37` and `genesis-recommend/src/main/java/com/genesis/recommend/rule/CorefChainGapRule.java`

**Issue:**  
`genesis-recommend` is declared as a peer module but takes a compile-time dependency on `genesis-coref` entities (`ClusterEntity`, `MentionEntity`) and its `MentionRepository`. The `ClusterChainProjectionRepository` works around this by using a JPQL query that references coref entities by fully-qualified class name without a Java import, but `CorefChainGapRule` imports `MentionRepository` directly. This creates a hidden coupling: `genesis-recommend` cannot be built if `genesis-coref` changes its entity package structure.

The `pom.xml` for `genesis-recommend` explicitly declares `genesis-coref` and `genesis-import-export` as Maven dependencies, making the dependency structural, not accidental.

**Fix:**  
Expose a `RecommendationDataPort` interface in `genesis-common` (or a new `genesis-recommend-api` module) that `genesis-coref` and `genesis-import-export` implement. `genesis-recommend` depends only on the port. Alternatively, if the workspace is always the unit of analysis, publish aggregated workspace-snapshot events that `genesis-recommend` consumes.

---

### A-004 — High: `genesis-notification` imports `genesis-workspace` repositories directly (not events)

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-notification/src/main/java/com/genesis/notification/listener/NotificationEventListener.java:22-31`

**Issue:**  
`NotificationEventListener` constructor-injects `WorkspaceMemberRepository` and `UserRepository` (from `genesis-workspace` and `genesis-user` respectively) to look up member lists and usernames when building notification messages. Importing another module's repository in a listener breaks the same principle as a direct service call — notification now couples to workspace's data model.

Additionally, the listener calls `workspaceMemberRepository.findByWorkspaceId()` inside `@Async` `@EventListener` methods, which means each notification event triggers a full member table scan outside any parent transaction, with no batching.

**Fix:**  
Carry sufficient data inside the event payload itself (recipient IDs, usernames, workspace name are already known at event publication time). `MemberAddedEvent`, `WorkspaceDeletedEvent`, etc. already include `memberIds` and `workspaceName` — populate `recipientUserIds` and `actorDisplayName` in the event so the listener does not need any repository.

---

### A-005 — High: `genesis-logging` imports `genesis-workspace` entities and repositories

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-logging/src/main/java/com/genesis/logging/service/AnnotationLogService.java:7-9`

**Issue:**  
`AnnotationLogService` imports `WorkspaceMember`, `MemberRole`, and `WorkspaceMemberRepository` to enforce an ADMIN-only read gate. While the intent is correct, importing workspace internals into the logging module creates a structural dependency. If workspace membership semantics change (new roles, membership model refactored), logging is affected.

**Fix:**  
Move the membership check into a dedicated `@PreAuthorize` expression or a common `WorkspaceAuthorizationService` in `genesis-common` that exposes only a boolean `isAdmin(workspaceId, userId)` contract. The logging module depends only on that port.

---

### A-006 — High: `AsyncDocumentProcessor` directly accesses `genesis-workspace` repository from `genesis-import-export`

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-import-export/src/main/java/com/genesis/importexport/service/AsyncDocumentProcessor.java:7-8`

**Issue:**  
`AsyncDocumentProcessor` imports and uses `DocumentRepository` and `Document` directly from `genesis-workspace` to update `ProcessingStatus` fields. This is the inverse of the intended flow: `genesis-import-export` should publish a `DocumentProcessedEvent` (or `DocumentTokenizedEvent` already exists), and `genesis-workspace` should listen and update its own entity.

Compounding this: `AsyncDocumentProcessor` also imports `WorkspaceService` indirectly through `DocumentService.java` (which it doesn't import itself, but the full chain is `import-export → workspace → infra`).

**Fix:**  
`AsyncDocumentProcessor` should publish `DocumentTokenizedEvent` (it already does this) and a new `DocumentProcessingFailedEvent`. A listener in `genesis-workspace` updates `ProcessingStatus`. Remove the `DocumentRepository` import from `genesis-import-export`.

---

### A-007 — High: `genesis-infra` imports `genesis-user` entities and repositories

**Severity:** High  
**Category:** Architecture — Module Boundary Violation  
**Location:** `genesis-infra/src/main/java/com/genesis/infra/security/UserDetailsServiceImpl.java:8-9`; `genesis-infra/src/main/java/com/genesis/infra/security/RefreshToken.java:7-8`; `genesis-infra/src/main/java/com/genesis/infra/security/RefreshTokenService.java:6`

**Issue:**  
`genesis-infra` (infrastructure/cross-cutting) has a compile-time dependency on `genesis-user` (domain module): it imports `User`, `UserRepository`, and `RefreshToken` references the `User` entity via a JPA `@ManyToOne`. This inverts the expected dependency direction — infrastructure should not depend on domain modules.

The `pom.xml` for `genesis-infra` does not declare `genesis-user` as a dependency, meaning this works at runtime only because `genesis-api` has both on its classpath. This is a hidden coupling that will break if modules are ever deployed separately.

**Fix:**  
Move `RefreshToken`, `RefreshTokenService`, and `RefreshTokenRepository` into `genesis-user` (they are user-domain concepts). `UserDetailsServiceImpl` can be a `@Bean` defined in `genesis-api`'s wiring layer, removing the infra-to-user dependency entirely.

---

### A-008 — High: `DebugController` exposes Cloudinary credentials metadata on a publicly accessible endpoint

**Severity:** High (Security adjacent)  
**Category:** Architecture — Security Configuration  
**Location:** `genesis-api/src/main/java/com/genesis/api/controller/DebugController.java`; `genesis-infra/src/main/java/com/genesis/infra/security/SecurityConfig.java:84`

**Issue:**  
`DebugController` exposes `/api/debug/cloudinary-config` and `/api/debug/cloudinary-test` which return the API key prefix, suffix, length, and performs a live Cloudinary API call (billing/usage data). The `SecurityConfig` permits all requests to `/api/debug/**` without authentication. The class-level comment says "Remove in production!" but there is no enforcement mechanism (no `@Profile("dev")`, no Spring Security `@Profile` gate).

**Fix:**  
Annotate `DebugController` with `@Profile("dev")` so it is not registered outside the `dev` profile. Remove the `permitAll()` matcher for `/api/debug/**` or restrict it to the `dev` profile via `SecurityConfig` conditional bean. Add a CI gate that fails if `/api/debug/**` is open in the `prod` profile.

---

### A-009 — High: Actuator exposes all endpoints (`management.endpoints.web.exposure.include=*`) with no authentication gate

**Severity:** High (Security)  
**Category:** Architecture — Security Configuration  
**Location:** `genesis-api/src/main/resources/application.properties:42`

**Issue:**  
`management.endpoints.web.exposure.include=*` exposes `/actuator/env`, `/actuator/beans`, `/actuator/threaddump`, `/actuator/heapdump`, etc. to any caller. While `/actuator/health` and `/actuator/info` being public is reasonable, the full actuator surface leaks environment variables (which may include secrets), bean graph, thread state, and heap snapshots.

**Fix:**  
In `application.properties` restrict to: `management.endpoints.web.exposure.include=health,info,metrics`. In `application-prod.properties` additionally add Spring Security rules requiring `ACTUATOR_ADMIN` role for all non-health actuator paths.

---

### A-010 — High: WebSocket endpoint allows all origins (`setAllowedOriginPatterns("*")`) in production

**Severity:** High (Security)  
**Category:** Architecture — Security Configuration  
**Location:** `genesis-notification/src/main/java/com/genesis/notification/config/WebSocketConfig.java:29-33`

**Issue:**  
The WebSocket STOMP endpoint uses `setAllowedOriginPatterns("*")` for both SockJS and raw WebSocket fallback. This negates origin-based CSRF protection for WebSocket connections. The `TODO: restrictive CORS in production` comment confirms awareness but provides no enforcement. The HTTP CORS configuration in `SecurityConfig` is correctly parameterised via `CORS_ALLOWED_ORIGINS`, but the WebSocket side is not.

**Fix:**  
Inject the same `cors.allowed-origins` property into `WebSocketConfig` and pass the resolved list to `setAllowedOrigins(origins)`. Add a startup assertion (similar to `SecurityConfig.corsConfigurationSource()`) that rejects blank or wildcard origins.

---

### A-011 — Medium: Hardcoded JWT secret default in `SecurityProperties`

**Severity:** Medium (Security-adjacent)  
**Category:** Architecture — Configuration  
**Location:** `genesis-infra/src/main/java/com/genesis/infra/security/SecurityProperties.java:26`

**Issue:**  
The default value `"defaultSecretKeyThatShouldBeChangedInProduction12345"` is baked into source code. If `genesis.security.jwt.secret` is not overridden in the environment, the application starts with a known secret that any attacker can use to forge valid JWTs. There is no startup validation that the secret has been changed from the default.

**Fix:**  
Remove the default value (set `private String secret;` with no initialiser). Add a `@PostConstruct` validation in `SecurityProperties` or `JwtTokenProvider` that throws `IllegalStateException` if the secret is null, blank, or matches the old default string. Document that `JWT_SECRET` is a required environment variable.

---

### A-012 — Medium: `WorkspaceActivityListener` uses synchronous `@EventListener` with `Propagation.REQUIRED` inside write transactions

**Severity:** Medium  
**Category:** Architecture — Event-Driven Coupling / Transaction Propagation  
**Location:** `genesis-workspace/src/main/java/com/genesis/workspace/listener/WorkspaceActivityListener.java:44-49`

**Issue:**  
`WorkspaceActivityListener.onWorkspaceActivity()` uses `@EventListener` (synchronous, in-transaction) with `@Transactional(propagation = Propagation.REQUIRED)`. This means the listener runs inside the calling method's transaction. If the listener's UPDATE to `workspaces.updated_at` fails, it rolls back the entire parent operation (mention create, cluster create, etc.). Additionally, `WorkspaceActivityEvent` is fired on nearly every annotation write, so this adds a serialised DB round-trip to every hot path.

The class comment acknowledges the tension but leaves it unresolved.

**Fix:**  
Switch to `@TransactionalEventListener(phase = AFTER_COMMIT)` with `@Async` so the `updated_at` refresh is a fire-and-forget after the annotation commits. If precise `updatedAt` is not business-critical (it is a UI convenience field), this is the correct trade-off.

---

### A-013 — Medium: `AuditorAwareImpl` in `genesis-common` returns `"system"` for all operations — real user not captured

**Severity:** Medium  
**Category:** Architecture — Cross-Cutting Concern  
**Location:** `genesis-common/src/main/java/com/genesis/common/audit/AuditorAwareImpl.java:24-27`

**Issue:**  
The `AuditorAwareImpl` in `genesis-common` always returns `"system"` despite having a `TODO` comment. Meanwhile, `GenesisApplication` defines an `auditorProvider` bean that correctly reads from `SecurityContextHolder`. Both beans exist in the same context (the `@EnableJpaAuditing(auditorAwareRef = "auditorProvider")` points to the one in `GenesisApplication`), but `AuditorAwareImpl` is registered as a Spring component unnecessarily and creates confusion. The `createdBy`/`updatedBy` columns are never wrong at runtime (the correct bean wins), but the dead bean is misleading and the TODO should be resolved.

**Fix:**  
Delete `AuditorAwareImpl.java` from `genesis-common`. The sole authoritative bean is the lambda in `GenesisApplication`. If a standalone-module default is needed for tests, define a test-scoped configuration.

---

### A-014 — Medium: `genesis-recommend` projection repositories abuse `DismissedRecommendationEntity` as a JPA root

**Severity:** Medium  
**Category:** Architecture — JPA Misuse  
**Location:** `genesis-recommend/src/main/java/com/genesis/recommend/repository/ClusterChainProjectionRepository.java`; `genesis-recommend/src/main/java/com/genesis/recommend/repository/TokenFormProjectionRepository.java`

**Issue:**  
Both cross-domain projection repositories extend `JpaRepository<DismissedRecommendationEntity, UUID>` purely to satisfy Spring Data's requirement of an entity type, while their only `@Query` methods reference entities from completely different modules (`ClusterEntity`, `MentionEntity`, `TokenEntity`, `Document`). The class-level JavaDoc explicitly calls this a hack. Beyond the smell, these repositories are registered by Spring Data for `DismissedRecommendationEntity`, meaning two repositories operate on the same entity — potentially causing ambiguity in query derivation.

**Fix:**  
Use `@Repository`-annotated classes backed by `EntityManager` and JPQL/native queries directly (no `JpaRepository` extension needed). Alternatively, use Spring Data's `@Query` on a `JpaRepository<Object, Long>` with a non-entity placeholder, but the cleanest solution is `EntityManager.createQuery()` in a `@Repository` component.

---

## CLEAN CODE FINDINGS

---

### C-001 — Critical: `MentionService.updateDocumentProgress()` swallows exceptions with `System.err.println`

**Severity:** Critical  
**Category:** Clean Code — Exception Handling  
**Location:** `genesis-coref/src/main/java/com/genesis/coref/service/MentionService.java:137-140`

**Issue:**  
```java
} catch (Exception e) {
    // Log but don't fail the operation
    System.err.println("Failed to update document progress: " + e.getMessage());
}
```
Two problems: (1) `System.err.println` bypasses the structured logging framework — this output will not appear in any log aggregator, will not carry correlation IDs, and will be silently discarded in containerised deployments where stderr is not wired. (2) The caught `Exception` is the broadest possible type; a `NullPointerException` from a null `tokenEndIndex` would be swallowed here and only surface as a silent data inconsistency. The `updateDocumentProgress` call is duplicated on three separate lines in the same method (`createMention`), meaning this issue has double the blast radius.

**Fix:**  
Replace with `log.warn("Failed to update document progress for document {}: {}", documentId, e.getMessage(), e)` using the class-level SLF4J logger. Investigate whether the progress update should instead propagate the exception (the annotation should not silently succeed with stale progress data).

---

### C-002 — High: `MentionService.updateDocumentProgress()` is called twice consecutively in three separate methods

**Severity:** High  
**Category:** Clean Code — Duplicate Code / Copy-Paste Bug  
**Location:** `genesis-coref/src/main/java/com/genesis/coref/service/MentionService.java:80-82, 203-205, 239-241`

**Issue:**  
In `createMention()`, `assignToCluster()`, and `unassignFromCluster()`, `updateDocumentProgress(saved.getDocumentId())` is called twice back-to-back with no intervening state change. Each call is a network round-trip to the database (two separate queries: `countByDocumentId`, `sumMentionTokensByDocumentId`), plus a write to update progress. This doubles the I/O cost of every annotation operation and is clearly a copy-paste error.

**Fix:**  
Remove one of the two consecutive calls in each of the three methods. Add a `@VisibleForTesting` annotation or a comment clarifying the single call intent.

---

### C-003 — High: `WorkspaceController` and `DocumentController` inject `UserRepository` directly — controller should not touch repositories

**Severity:** High  
**Category:** Clean Code — Layer Purity  
**Location:**  
- `genesis-workspace/src/main/java/com/genesis/workspace/controller/WorkspaceController.java:41-44`  
- `genesis-workspace/src/main/java/com/genesis/workspace/controller/DocumentController.java:29-34`

**Issue:**  
Both controllers inject `UserRepository` and call `userRepository.findByUsername()` in a `getUserIdFromPrincipal()` helper. Controllers must not hold repository references — that is service-layer work. This violates the standard layering rule and means a user lookup query runs in the HTTP thread outside any managed transaction.

**Fix:**  
Move the `getUserIdFromPrincipal()` helper logic into `UserService` (e.g., `userService.getIdByUsername(String)`) or expose it via a `UserFacade`. The controller then calls `userService.getIdByUsername(userDetails.getUsername())`.

---

### C-004 — High: `CoreferenceController` missing `@Valid` on all `@RequestBody` parameters

**Severity:** High  
**Category:** Clean Code — Missing Validation  
**Location:** `genesis-api/src/main/java/com/genesis/api/controller/CoreferenceController.java:51, 135, 174, 199`

**Issue:**  
`CreateMentionRequest`, `CreateClusterRequest`, `MergeClustersRequest` are all accepted as `@RequestBody` without `@Valid`. Bean Validation constraints on these DTOs (if present) are never enforced at the HTTP boundary. A caller can send null `documentId`, negative token indices, or empty `sourceClusterIds` and the service layer will encounter runtime errors rather than clean 400 validation responses.

The same issue affects: `NerAnnotationController` (line 46, 54), `PosController` (line 43, 54), `WsdController` (lines 72, 81, 99), `NerTagController` (line 44), `PosTagController` (line 44), `EditorController` (line 137), `RecommendationController` (line 51).

**Fix:**  
Add `@Valid` to every `@RequestBody` parameter across all affected controllers. Add Bean Validation constraints (`@NotNull`, `@Min(0)`, `@NotBlank`) to the corresponding request DTO fields. The `GlobalExceptionHandler` already handles `MethodArgumentNotValidException` correctly.

---

### C-005 — High: Service read methods missing `@Transactional(readOnly = true)`

**Severity:** High  
**Category:** Clean Code — Transaction Configuration  
**Location:**  
- `genesis-workspace/src/main/java/com/genesis/workspace/service/DocumentService.java` — `getByWorkspaceId()`, `getById()`, `countByWorkspaceId()` have no `@Transactional` annotation  
- `genesis-workspace/src/main/java/com/genesis/workspace/service/WorkspaceService.java` — `getById()`, `getAllForUser()`, `getMembers()` have no `@Transactional` annotation  
- `genesis-coref/src/main/java/com/genesis/coref/service/ClusterService.java` — `getClusters()`, `getCluster()` have no `@Transactional` annotation  
- `genesis-coref/src/main/java/com/genesis/coref/service/MentionService.java` — all `getMentionsByX()` methods have no `@Transactional` annotation  
- `genesis-notification/src/main/java/com/genesis/notification/service/NotificationService.java` — `getUserNotifications()`, `getUnreadCount()` have no `@Transactional` annotation

**Issue:**  
Read-only service methods without `@Transactional(readOnly = true)` miss several benefits: Hibernate's flush mode is not set to NEVER (causing unnecessary dirty-checking), the connection pool driver cannot optimise for read-only (e.g., route to a read replica), and lazy-loaded associations will trigger `LazyInitializationException` if accessed outside the session context they were obtained in.

**Fix:**  
Annotate all read-only service methods with `@Transactional(readOnly = true)`. For service classes that are predominantly read-only, place `@Transactional(readOnly = true)` at the class level and override individual write methods with `@Transactional`.

---

### C-006 — High: `ExportController` and `ShareExportController` contain business orchestration logic in the controller layer

**Severity:** High  
**Category:** Clean Code — Layer Purity  
**Location:** `genesis-api/src/main/java/com/genesis/api/controller/ExportController.java:66-83`; `genesis-api/src/main/java/com/genesis/api/controller/ShareExportController.java:111-130`

**Issue:**  
Both export controllers perform multi-step orchestration: fetch documents, call `coreferenceService.generateWorkspaceCorefAnnotations()`, iterate documents to call `posTaggingService.getMajorityPosByDocument()` and `getAnnotatorCountsByDocument()` in a loop, then assemble the result. This is business logic that belongs in a service. Controllers should have a single service call.

The loop over documents to build `posOverridesPerDoc` and `annotatorCountsPerDoc` is duplicated verbatim in both controllers (copy-paste).

**Fix:**  
Create `ExportOrchestrationService` in `genesis-import-export` (or a new `genesis-export-api` module) that encapsulates the full multi-source assembly. Both controllers reduce to a single `exportOrchestrationService.exportWorkspace(workspaceId, options)` call.

---

### C-007 — Medium: `AuthController` contains partial authentication business logic that bypasses the service layer

**Severity:** Medium  
**Category:** Clean Code — Layer Purity  
**Location:** `genesis-api/src/main/java/com/genesis/api/controller/AuthController.java:74-98`

**Issue:**  
The `login()` method in `AuthController` directly orchestrates: `authenticationManager.authenticate()`, token generation via `jwtTokenProvider.generateAccessToken()`, refresh token creation via `refreshTokenService.createRefreshToken()`, and `userService.updateLastLogin()`. This multi-step auth flow belongs in an `AuthService`, not a controller. The controller currently has five dependencies, which is a signal it is doing too much.

**Fix:**  
Create `AuthService` in `genesis-user` (or `genesis-infra`) that exposes `login(LoginRequest)` returning `TokenResponse`. The controller holds a single `AuthService` reference.

---

### C-008 — Medium: `WorkspaceService.mapToResponse()` performs live database queries inside the mapping method

**Severity:** Medium  
**Category:** Clean Code — N+1 Risk / Method Responsibility  
**Location:** `genesis-workspace/src/main/java/com/genesis/workspace/service/WorkspaceService.java:294-319`

**Issue:**  
`mapToResponse(Workspace)` calls `documentRepository.countByWorkspaceId()` and `documentRepository.countByWorkspaceIdAndStatus()` inside the mapping step. When `getAllForUser()` returns a list of workspaces and calls `map(this::mapToResponse)`, this results in 2N additional queries for N workspaces — a classic N+1 problem. For a user with 20 workspaces, this is 40 extra queries per list endpoint call.

**Fix:**  
Execute a single aggregate query (`SELECT workspace_id, COUNT(*), COUNT(*) FILTER (WHERE status = 'COMPLETE')`) before the mapping step and pass the result as a `Map<UUID, long[]>` into `mapToResponse`. Alternatively, add a JPQL query returning a projection with the counts joined.

---

### C-009 — Medium: `EditorService.getDocumentContentWithOffset()` performs an O(N) token count loop per document

**Severity:** Medium  
**Category:** Clean Code — Performance  
**Location:** `genesis-editor/src/main/java/com/genesis/editor/service/EditorService.java:193-203`

**Issue:**  
`getDocumentContentWithOffset()` fetches all documents for the workspace, then iterates through them calling `importService.getTokenCount(doc.getId())` per document until it reaches the requested document. Each `getTokenCount` is a `COUNT(*)` query. For a workspace with 50 documents, a call requesting the last document triggers 49 database queries.

**Fix:**  
Store the cumulative `tokenStartIndex` on `Document` (it already has `tokenStartIndex` and `tokenEndIndex` fields populated during tokenization). Use `document.getTokenStartIndex()` as the global offset directly, eliminating the loop entirely.

---

### C-010 — Medium: `ClusterService.compactClusterNumbers()` performs a two-phase flush of potentially large in-memory lists

**Severity:** Medium  
**Category:** Clean Code — Performance / Correctness  
**Location:** `genesis-coref/src/main/java/com/genesis/coref/service/ClusterService.java:252-283`

**Issue:**  
`compactClusterNumbers()` loads all clusters for a workspace into memory (unbounded), sets negative temporary numbers, flushes, then sets positive numbers and flushes again. This is called after every `deleteCluster()` and `mergeClusters()`. For workspaces with hundreds of clusters this is expensive, and the two-phase approach was chosen to avoid a unique constraint violation — but the better fix is to use a deferred constraint or a temporary sequence in SQL.

The method is also `@Transactional` and called from within other `@Transactional` methods, so the flushes happen in the same transaction, which is correct but means they hold a write lock on the entire cluster table for the duration.

**Fix:**  
Execute a single UPDATE using a window function or a case expression in native SQL: `UPDATE clusters SET cluster_number = row_number FROM (SELECT id FROM clusters WHERE workspace_id = ? ORDER BY cluster_number) ordered WHERE clusters.id = ordered.id`. This avoids the two-phase in-memory approach entirely.

---

### C-011 — Medium: `FileStorageService.downloadAsString()` uses raw `java.net.URL` / `HttpURLConnection` with no retry or timeout beyond 10 seconds

**Severity:** Medium  
**Category:** Clean Code — Resilience  
**Location:** `genesis-infra/src/main/java/com/genesis/infra/storage/FileStorageService.java:138-163`

**Issue:**  
The method uses a hand-rolled HTTP client (`java.net.URL` + `HttpURLConnection`) to download files from Cloudinary. The 10-second read timeout is the only safety net; there is no retry logic, no connection pool, and no streaming for large files (the entire content is buffered in a `StringBuilder`). For a 50 MB CoNLL file (the max upload size), this allocates a large String in heap. If Cloudinary is slow or the connection drops, the tokenization pipeline hangs and eventually marks the document as FAILED.

**Fix:**  
Replace with Spring's `RestClient` or `WebClient` (already on the classpath) with configurable timeouts, retry with exponential backoff, and streaming support. For files over a size threshold, stream to a temp file and process line-by-line rather than loading entirely into memory.

---

### C-012 — Medium: JSON hand-written via `String.format()` in audit log events — no escaping safety

**Severity:** Medium  
**Category:** Clean Code — Security / Correctness  
**Location:** `genesis-coref/src/main/java/com/genesis/coref/service/ClusterService.java:68-75`; `MentionService.java:93-95`; `NerAnnotationService.java:207-215`; `PosTaggingService.java:91-94`

**Issue:**  
Multiple services build the `payloadJson` for `AnnotationLogEvent` using `String.format()` with hand-escaped values. `ClusterService` includes a custom `escape()` helper but it only handles `\"` and `\\` — it misses other JSON special characters (`\n`, `\r`, `\t`, control characters). A cluster label containing a newline or a NUL character produces malformed JSON in the audit log.

**Fix:**  
Use a proper JSON serialiser for the audit payload. Jackson's `ObjectMapper.writeValueAsString(Map.of(...))` is already available. A two-line helper in `genesis-common` would eliminate all the hand-rolling.

---

### C-013 — Medium: `@Transactional` missing on `WorkspaceRepository.updateLastModified()` — `@Modifying` query without transaction

**Severity:** Medium  
**Category:** Clean Code — Transaction Safety  
**Location:** `genesis-workspace/src/main/java/com/genesis/workspace/repository/WorkspaceRepository.java:68-70`

**Issue:**  
`updateLastModified()` uses `@Modifying` without `@Transactional`. While in practice the `WorkspaceActivityListener` that calls it is `@Transactional(propagation = REQUIRED)`, relying on callers to always provide a transaction for a `@Modifying` method is fragile. Spring Data JPA will throw `TransactionRequiredException` if this method is ever called without a surrounding transaction.

**Fix:**  
Add `@Transactional` to the `updateLastModified()` method declaration on the repository interface, as is standard for `@Modifying` methods.

---

### C-014 — Medium: Missing `@Transactional` on `@Modifying` delete methods in `MentionRepository`, `TokenRepository`, `SentenceRepository`

**Severity:** Medium  
**Category:** Clean Code — Transaction Safety  
**Location:**  
- `genesis-coref/src/main/java/com/genesis/coref/repository/MentionRepository.java:70-83` — `deleteByClusterId`, `deleteByWorkspaceId`, `deleteByDocumentId`, `unassignFromCluster`  
- `genesis-import-export/.../TokenRepository.java` — `deleteByDocumentId`  
- `genesis-import-export/.../SentenceRepository.java` — `deleteByDocumentId`

**Issue:**  
All `@Modifying` methods that delete or bulk-update rows lack `@Transactional`. Spring Data JPA requires a transaction for all DML operations; the absence of `@Transactional` means callers must provide one. If any caller adds a new code path that is not `@Transactional`, a `TransactionRequiredException` will surface at runtime rather than compile time.

**Fix:**  
Add `@Transactional` to every `@Modifying` method on every repository. This is the Spring Data JPA convention.

---

### C-015 — Low: `Collectors.toList()` used throughout — replace with `Stream.toList()` (Java 16+)

**Severity:** Low  
**Category:** Clean Code — Java Idioms  
**Location:** 22 occurrences across main sources (all services and controllers)

**Issue:**  
The codebase targets Java 21 but universally uses `stream().collect(Collectors.toList())` instead of the more concise and GC-friendly `stream().toList()` available since Java 16. `Stream.toList()` returns an unmodifiable list, which is correct for DTO response lists that should not be mutated by callers.

**Fix:**  
Global search-and-replace `collect(Collectors.toList())` → `toList()`. Add a Checkstyle/SpotBugs rule to enforce this going forward.

---

### C-016 — Low: `genesis-editor` module has zero behavioural test coverage

**Severity:** Low  
**Category:** Testing  
**Location:** `genesis-editor/src/test/java/com/genesis/editor/` — contains only `package-info.java` and one stub `EditorServiceTest.java`

**Issue:**  
`EditorService` is a complex class with pagination logic, global token offset calculation (with an O(N) query loop), and session management. None of its business paths (including the pagination boundary conditions and the global offset computation) are unit-tested. The `EditorServiceTest.java` file exists but is essentially empty (only a `package-info.java` accompanies it).

**Fix:**  
Write unit tests for `EditorService` using `@ExtendWith(MockitoExtension.class)`. Test: correct page boundary calculation, correct global offset accumulation, session creation on first open, and the `getDocumentContentWithOffset()` method at page boundaries.

---

### C-017 — Low: `genesis-notification` module has zero automated tests

**Severity:** Low  
**Category:** Testing  
**Location:** `genesis-notification/src/` — no `test` directory exists

**Issue:**  
`NotificationEventListener` and `NotificationService` have non-trivial logic: they filter workspace members, build WebSocket push messages, and handle seven distinct event types. None of this is tested. The `@Async` listener behaviour (exception isolation, thread context) is particularly important to validate.

**Fix:**  
Create unit tests with `@ExtendWith(MockitoExtension.class)` for `NotificationService`. For `NotificationEventListener`, write integration tests using `@SpringBootTest` slice with a mock `SimpMessagingTemplate`.

---

### C-018 — Low: `@SpringBootTest` used on `GenesisApplicationTests` — this is a smoke test that loads the full context

**Severity:** Low  
**Category:** Testing  
**Location:** `genesis-api/src/test/java/com/genesis/api/GenesisApplicationTests.java`

**Issue:**  
`@SpringBootTest` without any `webEnvironment` setting loads the full application context, starts all beans, and requires a live database (or a complete H2 test profile). This is expensive and fragile — it fails on schema changes. Its purpose appears to be "the app starts" which is a valid smoke test, but it should be clearly annotated and excluded from the fast unit-test suite.

**Fix:**  
Keep as a smoke test but annotate with `@Tag("integration")` and exclude it from the default Surefire run. Use `@SpringBootTest(webEnvironment = NONE)` with an H2 test profile for CI.

---

### C-019 — Low: `instance_id` / `setId()` exposed on `BaseEntity` — JPA-generated IDs should not be settable

**Severity:** Low  
**Category:** Clean Code — JPA Design  
**Location:** `genesis-common/src/main/java/com/genesis/common/entity/BaseEntity.java:62-65`

**Issue:**  
`BaseEntity` exposes a public `setId(UUID id)` setter. JPA entities with `GenerationType.UUID` should not have their IDs set externally — Hibernate manages the lifecycle. Exposing the setter invites accidental ID assignment in mappers or tests that could lead to duplicate-key errors or silent entity detachment.

**Fix:**  
Remove `setId()`. For tests that need to set IDs, use reflection or Hibernate's `@GeneratedValue` in test mode with a seeded sequence. The `setCreatedAt()`, `setUpdatedAt()`, `setCreatedBy()`, `setUpdatedBy()` setters should similarly be package-private or removed.

---

### C-020 — Info: `genesis-common` contains infrastructure concerns (`AuditorAwareImpl`, `JpaAuditingConfig`)

**Severity:** Info  
**Category:** Architecture — Cohesion  
**Location:** `genesis-common/src/main/java/com/genesis/common/audit/`

**Issue:**  
`genesis-common` is intended as a shared-value-object/DTO module. It contains `AuditorAwareImpl` and `JpaAuditingConfig` which are infrastructure beans. These leak JPA infrastructure into what should be a pure POJO module. `genesis-common` currently has no dependency on any infrastructure framework besides JPA annotations on `BaseEntity`, which is acceptable.

**Fix:**  
Move `AuditorAwareImpl` and `JpaAuditingConfig` to `genesis-infra` (the correct home for infrastructure configuration) or into `genesis-api`'s wiring layer. Keep `genesis-common` as a pure shared-model module.

---

## TOP 5 ARCHITECTURAL RISKS

### Risk 1: Direct Service Injection Across Module Boundaries Will Stall the Architecture

`MentionService` (coref) → `DocumentService` (workspace) is a live, runtime dependency that bypasses the event contract. As the codebase grows with more annotation types (wsd, ner, pos all have the same pattern of importing workspace repositories), the module graph collapses into a tightly coupled mesh. The claimed "modules never call each other's services" architectural guarantee is already broken in four places. Without structural enforcement (e.g., ArchUnit rules in CI, or Maven module dependency restrictions), this erosion will accelerate.

### Risk 2: The `genesis-api` Controller Layer Is Growing Into an Orchestration Layer

`ExportController` and `ShareExportController` duplicate multi-source orchestration logic. `AuthController` does full auth flow management. As more features are added, controllers will accumulate service dependencies and business logic unless a service-layer extraction is enforced. The anti-pattern is already set as a precedent.

### Risk 3: No Idempotency on the Async Tokenization Pipeline

If `DocumentUploadedEvent` is published but the async tokenization fails after updating `ProcessingStatus.PROCESSING`, a retry (by the user re-uploading or a system retry) will re-run tokenization. `ImportService.importPlainText()` does `deleteByDocumentId` before re-tokenizing — this is safe for plain text — but the `importConll2012` path publishes `ConllImportedEvent` which triggers the coref listener to create clusters and mentions. A retry would duplicate all coref annotations. There is no idempotency key or guard against double-processing.

### Risk 4: WebSocket `setAllowedOriginPatterns("*")` Is a Production Security Gap

The HTTP REST CORS is correctly locked down via `CORS_ALLOWED_ORIGINS`, but WebSocket connections bypass this entirely. Any browser from any origin can establish a WebSocket connection, subscribe to notification queues, and receive real-time notification data. Since notification payloads include `workspaceId`, `actorId`, and annotation event details, this is a data leak risk if a CSRF-like attack can be crafted through a malicious origin.

### Risk 5: `ddl-auto=validate` + `baseline-on-migrate=true` in the Same Config Is a Footgun

`application.properties` sets both `spring.jpa.hibernate.ddl-auto=validate` and `spring.flyway.baseline-on-migrate=true`. The `validate` mode causes Hibernate to compare entities against the schema at startup and refuse to start if they disagree. But `baseline-on-migrate=true` means Flyway will adopt any existing database schema at version 1 without verifying its content matches `V1__*.sql`. This means a database drifted by manual DDL changes will pass Flyway baseline but potentially fail Hibernate validate — or worse, Flyway baseline silently adopts a wrong schema and later migrations break. The two mechanisms need a clear contract about who is the source of truth.

---

## TOP 10 CLEAN-CODE WINS

These are high-value, low-risk changes that can be made immediately:

1. **Add `@Valid` to all `@RequestBody` parameters across all controllers** (C-004) — 15 minutes, zero logic change, immediately improves error reporting and removes a class of service-layer NPEs.

2. **Remove duplicate `updateDocumentProgress()` calls in `MentionService`** (C-002) — delete 3 lines, fix a silent double-query bug.

3. **Replace `System.err.println` with SLF4J logger** (C-001) — one-line fix that restores observability in production.

4. **Add `@Transactional` to all `@Modifying` repository methods** (C-013, C-014) — mechanical change across ~8 methods, prevents future `TransactionRequiredException` surprises.

5. **Add `@Transactional(readOnly = true)` to all read service methods** (C-005) — annotate 15–20 methods, improves Hibernate flush mode, reduces dirty-checking overhead.

6. **Move `getUserIdFromPrincipal()` out of controllers into `UserService`** (C-003) — removes repository imports from controllers, improves testability.

7. **Replace `Collectors.toList()` with `Stream.toList()`** (C-015) — 22 replacements, more idiomatic Java 21.

8. **Add `@Profile("dev")` to `DebugController`** (A-008) — single annotation, eliminates the credential-leak endpoint in production.

9. **Restrict `management.endpoints.web.exposure.include` to `health,info,metrics`** (A-009) — one-line properties change, closes the actuator information-disclosure surface.

10. **Delete `AuditorAwareImpl` from `genesis-common`** (A-013) — remove dead code with a TODO that has been superseded by the correct implementation in `GenesisApplication`.

---

## EXECUTIVE SUMMARY

The Genesis backend has a well-designed structural intent: a modular monolith where Spring Events decouple modules. The infrastructure is solid — constructor injection is used consistently, there is a centralised `GlobalExceptionHandler`, the exception hierarchy is clean, JPA entities are generally not leaked to the API layer (DTOs are used), and the test suite for core modules (`genesis-workspace`, `genesis-coref`) uses proper Mockito extensions.

However, six of the nine non-infrastructure modules violate the module boundary contract by importing repositories, entities, or services from peer modules. This has turned the "modular" monolith into a tightly coupled monolith with a thin event facade. The violations are structural (declared in `pom.xml` dependencies for `genesis-recommend`) and runtime (service injection for `genesis-coref`).

Three security issues require immediate attention: the `DebugController` is unauthenticated and exposes cloud credentials metadata in what is intended to be a production binary; all Actuator endpoints are public; and WebSocket origin validation is absent.

The codebase has no swallowed-exception anti-pattern at scale except the one critical `System.err.println` case, which is hiding a repeated double-query bug. There are no field-injected beans. There are no hardcoded secrets in source except the JWT default in `SecurityProperties`. The `AuditorAwareImpl` TODO is benign (the correct bean wins) but should be cleaned up.

---

## PRIORITIZED ACTION LIST

**Immediate (block deployment if going to production):**

1. Add `@Profile("dev")` to `DebugController` and restrict `/api/debug/**` in `SecurityConfig` (A-008)
2. Restrict Actuator endpoints to `health,info,metrics` (A-009)  
3. Remove `defaultSecretKeyThatShouldBeChangedInProduction` JWT default and add startup validation (A-011)
4. Set WebSocket `setAllowedOrigins(resolvedOrigins)` instead of wildcard pattern (A-010)

**Sprint 1 (within 2 weeks):**

5. Add `@Valid` to all `@RequestBody` parameters (C-004)
6. Fix duplicate `updateDocumentProgress()` calls and replace `System.err.println` (C-001, C-002)
7. Add `@Transactional(readOnly = true)` to all read methods; add `@Transactional` to all `@Modifying` methods (C-005, C-013, C-014)
8. Move `getUserIdFromPrincipal()` out of controllers (C-003)
9. Add ArchUnit or Enforcer rules to CI that prevent cross-module service/repository imports

**Sprint 2 (within 1 month):**

10. Refactor `MentionService` to publish events instead of calling `DocumentService` directly (A-001)
11. Replace `pos`, `wsd`, `ner`, `logging` direct repository imports with a common query port (A-002, A-005)
12. Extract export orchestration into `ExportOrchestrationService` (C-006)
13. Fix `WorkspaceService.mapToResponse()` N+1 query (C-008)
14. Move `RefreshToken` / `RefreshTokenService` to `genesis-user`, breaking infra→user dependency (A-007)

**Backlog:**

15. Refactor `genesis-recommend` projection repositories (A-014)
16. Replace hand-rolled HTTP client in `FileStorageService.downloadAsString()` with `RestClient` (C-011)
17. Add comprehensive tests for `genesis-notification` and `genesis-editor` (C-016, C-017)
18. Replace hand-written JSON strings with `ObjectMapper` in audit log events (C-012)
19. Fix `compactClusterNumbers()` two-phase flush with a single SQL window-function UPDATE (C-010)
20. Delete `AuditorAwareImpl` from `genesis-common` and move `JpaAuditingConfig` to `genesis-infra` (A-013, C-020)
