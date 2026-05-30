# Genesis Backend — Audit Remediation TODO

Consolidated fix-list across the three backend audits, ordered by risk and effort.
Each item links to its source finding for full context.

**Sources:**
- [`SECURITY_AUDIT.md`](./SECURITY_AUDIT.md) — OWASP + secrets + authz
- [`ARCHITECTURE_AUDIT.md`](./ARCHITECTURE_AUDIT.md) — module boundaries + clean code
- [`SYSTEM_DESIGN_AUDIT.md`](./SYSTEM_DESIGN_AUDIT.md) — DB + perf + observability

Legend: 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Low

---

## P0 — STOP-SHIP (do before any shared/prod deploy)

These are exploit-ready. Fix this week.

- [x] 🔴 **Rotate Cloudinary creds + remove committed `.env` files** — live API key/secret in repo working tree → [SECURITY_AUDIT.md#critical-1](./SECURITY_AUDIT.md) · Effort: 30 min — Cloudinary keys rotated by owner; verified `.env` never tracked, `.gitignore` blocks `.env*`, `env.example` placeholders only, no hardcoded secrets in code.
- [x] 🔴 **Delete `DebugController.java` (or `@Profile("dev")`)** — unauthenticated endpoint leaks Cloudinary creds + hits live API → [SECURITY_AUDIT.md#critical-2](./SECURITY_AUDIT.md), [ARCHITECTURE_AUDIT.md A-008](./ARCHITECTURE_AUDIT.md) · Effort: 15 min — Deleted file + removed `/api/debug/**` permitAll from `SecurityConfig`.
- [x] 🔴 **Remove hardcoded JWT fallback secret + add startup validation** — `SecurityProperties.java:26` → [SECURITY_AUDIT.md#critical-4](./SECURITY_AUDIT.md), [ARCHITECTURE_AUDIT.md A-011](./ARCHITECTURE_AUDIT.md) · Effort: 1 h — Wired `JWT_SECRET` binding (was never read), dropped default, added `@Validated`+`@NotBlank`+`@Size(min=32)` and `JwtTokenProvider` constructor guard. Bonus: pinned HS256 explicitly.
- [x] 🔴 **Add `requireMember`/`requireAdmin` to Workspace + Document mutations** — IDOR: any user can archive/rename/delete any workspace → [SECURITY_AUDIT.md#critical-3](./SECURITY_AUDIT.md) · Effort: 1–2 days — Added `WorkspaceAccessControl` component; gated all WorkspaceController + DocumentController endpoints (read MEMBER, write ADMIN per matrix), introduced `*Internal` variants for trusted server-side callers (share-token export, async tokenization, annotation cascade). Annotation/coref/POS endpoints still un-gated (P1 MEDIUM-4).
- [x] 🟠 **Restrict Actuator exposure** — `management.endpoints.web.exposure.include=health,info,metrics` → [SECURITY_AUDIT.md#high-1](./SECURITY_AUDIT.md), [ARCHITECTURE_AUDIT.md A-009](./ARCHITECTURE_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md P0#2](./SYSTEM_DESIGN_AUDIT.md) · Effort: 5 min — `application-prod.properties` now restricts exposure to `health,info,metrics,prometheus` and sets health show-details/components to `when-authorized`.
- [x] 🟠 **WebSocket: replace `setAllowedOriginPatterns("*")` with `CORS_ALLOWED_ORIGINS`** — CSWSH → [SECURITY_AUDIT.md#high-3](./SECURITY_AUDIT.md), [ARCHITECTURE_AUDIT.md A-010](./ARCHITECTURE_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md P0#3](./SYSTEM_DESIGN_AUDIT.md) · Effort: 10 min — `WebSocketConfig` now reuses the `cors.allowed-origins` property and calls `setAllowedOrigins(origins)` on both the SockJS and raw `/ws` endpoints, with the same empty/blank guard as `SecurityConfig`.
- [x] 🟠 **Disable SQL + DEBUG logging in `application-prod.properties`** — PII in logs → [SECURITY_AUDIT.md#high-5](./SECURITY_AUDIT.md) · Effort: 15 min — prod overrides set `show-sql=false`, `com.genesis=INFO`, `org.springframework=WARN`, and `org.hibernate.SQL/orm.jdbc.bind=WARN`.
- [x] 🟠 **Tune HikariCP idle-timeout/keepalive + drop `flyway.baseline-on-migrate`** → [SYSTEM_DESIGN_AUDIT.md P0#4–5](./SYSTEM_DESIGN_AUDIT.md) · Effort: 10 min — Main props now declare `idle-timeout=300s`, `max-lifetime=30m`, `keepalive-time=60s`, `validation-timeout=5s`. Prod overrides `flyway.baseline-on-migrate=false` and bumps pool to 10/2.

---

## P1 — Sprint 1 (next 2 weeks)

Auth hardening, data integrity, hot-path correctness.

- [x] 🟠 **Rate-limit `/api/auth/login|signup|refresh` (bucket4j)** → [SECURITY_AUDIT.md#high-4](./SECURITY_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md P2#13](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day — `AuthRateLimitFilter` (bucket4j-core, in-memory `ConcurrentHashMap<endpoint|ip, Bucket>`); 5/min login, 3/min signup, 10/min refresh; X-Forwarded-For-aware; 429 + `Retry-After` + ApiResponse-shaped body. Single-node — multi-replica will need bucket4j-redis (P3 follow-up).
- [x] 🟠 **Remove `access_token` URL query support from `JwtAuthenticationFilter`** + add to redaction list → [SECURITY_AUDIT.md#high-2](./SECURITY_AUDIT.md) · Effort: 2 h — Deleted the `getParameter("access_token")` fallback in `JwtAuthenticationFilter.getJwtFromRequest`; added `"access_token"` to `RequestLoggingInterceptor.REDACTED_QUERY_PARAMS` as defense in depth. Follow-up: `WebSocketAuthInterceptor.getNativeHeader("access_token")` STOMP fallback is a separate transport (not in HTTP access logs) — schedule a deprecation if standardising on `Authorization: Bearer` for STOMP too.
- [x] 🟠 **Add security response headers (CSP, HSTS, X-Frame, nosniff)** → [SECURITY_AUDIT.md#high-6](./SECURITY_AUDIT.md) · Effort: 1 h — `SecurityConfig.filterChain` now adds CSP (`default-src 'self'; frame-ancestors 'none'`), HSTS (1y + includeSubDomains; HTTPS-only by Spring default), Referrer-Policy STRICT_ORIGIN_WHEN_CROSS_ORIGIN, X-Frame-Options DENY, X-Content-Type-Options nosniff. HSTS preload deliberately not enabled (one-way commitment). Follow-up: optional integration test asserting headers presence.
- [x] 🟠 **Generic error on signup duplicate (no user enumeration)** → [SECURITY_AUDIT.md#high-7](./SECURITY_AUDIT.md) · Effort: 30 min — Collapsed both throws into one generic `ValidationException("Unable to register with the provided credentials")`; both `existsByUsername` and `existsByEmail` still run; tests updated to assert the new exception and message.
- [x] 🟠 **Add FK constraints on annotation tables (V4 migration)** → [SYSTEM_DESIGN_AUDIT.md F-DB-01](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 h — V5 migration adds 20+ `ALTER TABLE … ADD CONSTRAINT FOREIGN KEY` statements across coref/POS/WSD/NER/notifications/editor_sessions/dismissed_recommendations/annotation_log with CASCADE for tightly-owned children and SET NULL for optional parents. Operational pre-deploy step: run orphan-detection queries + `TRUNCATE editor_sessions` (synthetic UUIDs from the pre-MEDIUM-2 EditorController bug). Follow-up gap noted: `notifications.actor_id` has no FK (pre-existing, narrow scope; schedule under a new F-DB entry).
- [x] 🟠 **Composite index `notifications(recipient_id, read, created_at)` + bulk `markAllAsRead`** → [SYSTEM_DESIGN_AUDIT.md F-DB-02](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 h — V6 adds `idx_notifications_recipient_read_created (recipient_id, read, created_at DESC)`; `NotificationService.markAllAsRead` now issues a single `@Modifying` UPDATE (`markAllAsReadByUserId`) instead of load-all-then-save.
- [x] 🟠 **Fix `notifications.created_at` to `timestamptz`** → [SYSTEM_DESIGN_AUDIT.md F-DB-04](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 h — V6 `ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE 'UTC'`; entity + DTO field switched from `LocalDateTime` to `Instant` for consistency with `BaseEntity`. Operational note: the UTC cast assumes historical rows were inserted from a UTC JVM (Docker / Railway default) — sanity-check pre-prod-deploy if any other JVM may have written rows.
- [x] 🟠 **Refresh token rotation on use** → [SECURITY_AUDIT.md#medium-1](./SECURITY_AUDIT.md) · Effort: 2 h — `RefreshTokenService.rotateToken(oldToken)` revokes the old row and creates a new one inside the class-level `@Transactional`; `AuthController.refresh` returns the new value. FE must persist the new `refreshToken` from each `/api/auth/refresh` response (deferred).
- [x] 🟠 **Fix `EditorController.getUserId()` — query DB, not hash username** → [SECURITY_AUDIT.md#medium-2](./SECURITY_AUDIT.md) · Effort: 1 h — Injected `UserRepository`; resolves via `findByUsername` with `UnauthorizedException` on null principal / missing user. Pre-fix `editor_sessions` rows carry synthetic UUIDs (orphan data; clean up before deploy or accept one-time session-state reset).
- [x] 🟠 **Add workspace-membership checks to coref/POS/NER services** → [SECURITY_AUDIT.md#medium-4](./SECURITY_AUDIT.md) · Effort: 1 day — Coref (MentionService, ClusterService) + POS (PosTaggingService, PosTagDefinitionService) + NER (NerAnnotationService, NerTagDefinitionService) all inject `WorkspaceAccessControl` and call `requireMember(workspaceId, callerId)` as the first statement of every public method. Controllers resolve callerId from `SecurityContextHolder` + `UserRepository.findByUsername`. `PosController.currentAnnotator()` no longer falls through to `"system"` — throws `UnauthorizedException` on missing principal. `*Internal` variants on PosTaggingService for share-token export trusted-path. Existing service tests updated.
- [x] 🔴 **Fix `MentionService.updateDocumentProgress()`: replace `System.err.println` + remove duplicate calls** → [ARCHITECTURE_AUDIT.md C-001, C-002](./ARCHITECTURE_AUDIT.md) · Effort: 15 min — Replaced with parameterized SLF4J `logger.error`; removed duplicate `updateDocumentProgress` calls in `createMention`, `assignToCluster`, `unassignFromCluster`.
- [x] 🟠 **Add `@Valid` to every `@RequestBody` across all controllers** → [ARCHITECTURE_AUDIT.md C-004](./ARCHITECTURE_AUDIT.md) · Effort: 30 min — Swept all 17 call sites across NerTag, NerAnnotation, Editor, Export, PosTag, Wsd, Pos, Coreference, Recommendation controllers.
- [x] 🟠 **Add `@Transactional(readOnly=true)` to read services + `@Transactional` to all `@Modifying` repos** → [ARCHITECTURE_AUDIT.md C-005, C-013, C-014](./ARCHITECTURE_AUDIT.md) · Effort: 1 h — Class-level read-only on CoreferenceService, ExportService, ContinuousTokenizationService (+ method-level write on `updateTokenIndices`); CloudinaryService deliberately untouched (pure external I/O); every `@Modifying` repo method verified to be reached only from a `@Transactional` service.
- [x] 🟠 **Scheduled cleanup of expired refresh tokens** → [SECURITY_AUDIT.md#low-2](./SECURITY_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md P1#10](./SYSTEM_DESIGN_AUDIT.md) · Effort: 30 min — `@EnableScheduling` on `GenesisApplication`; `@Scheduled(cron = "0 0 3 * * *", zone = "UTC")` on `RefreshTokenService.deleteExpiredTokens()`; SLF4J INFO line per run for auditability.
- [x] 🟡 **Add partial index on `documents(processing_status)`** → [SYSTEM_DESIGN_AUDIT.md F-DB-03](./SYSTEM_DESIGN_AUDIT.md) · Effort: 30 min — V4 migration adds `idx_documents_processing_status_active` partial index `WHERE processing_status IN ('PENDING','PROCESSING')`.

---

## P2 — Sprint 2 (next month)

Module boundaries, N+1s, pagination, observability.

- [x] 🟠 **Refactor `MentionService` to publish `MentionAnnotatedEvent` instead of injecting `DocumentService`** → [ARCHITECTURE_AUDIT.md A-001](./ARCHITECTURE_AUDIT.md) · Effort: 1 day — `MentionService` drops the `DocumentService` injection entirely. Hot path (create/assign/unassign): publishes `MentionAnnotatedEvent(documentId, mentionTokenCount)` (new, in `genesis-common.event` since coref/workspace are siblings); a new `DocumentAnnotationProgressListener` in genesis-workspace owns the ANNOTATING transition + progress formula on its own `Document` (AFTER_COMMIT, so a progress failure can't roll back the mention — preserves the old "log, don't fail" semantics). Read path (`getMentionsByDocument`): `workspaceId` now resolved in `CoreferenceController` (composition root) and passed in, so coref no longer calls `DocumentService`; the membership check stays in the service (behavior identical). coref `src/main` now depends on workspace only via `WorkspaceAccessControl` (sanctioned cross-cutting) + events. Tests: `DocumentAnnotationProgressListenerTest` (3), `MentionServiceTest` updated (+event-publish assertion). **ArchUnit baseline ratcheted 177 → 174** (MentionService→DocumentStatus pruned).
- [ ] 🟠 **Replace pos/wsd/ner/logging direct repo imports with query ports** → [ARCHITECTURE_AUDIT.md A-002, A-005](./ARCHITECTURE_AUDIT.md) · Effort: 2 days
- [x] 🟠 **Move `RefreshToken*` from `genesis-infra` → `genesis-user`** (break infra→user dep) → [ARCHITECTURE_AUDIT.md A-007](./ARCHITECTURE_AUDIT.md) · Effort: 4 h — Moved `RefreshToken` → `user.entity`, `RefreshTokenRepository` → `user.repository`, `RefreshTokenService` → `user.service` (TTL now via `@Value("${genesis.security.jwt.refresh-token-expiry:7d}")`, no `SecurityProperties` import). `UserDetailsServiceImpl` relocated to `genesis-api/security`; `SecurityConfig` + `JwtAuthenticationFilter` now depend on the `UserDetailsService` interface. Dropped `UserModuleConfig` from `InfraModuleConfig` and the dead `infra.security` entries from `@EntityScan`/`@EnableJpaRepositories`. **Removed `genesis-user` from `genesis-infra/pom.xml`** — infra no longer compiles against the domain module (verified: full reactor build + context-load test pass). `refresh_tokens` table unchanged (no migration).
- [x] 🟠 **Move `getUserIdFromPrincipal()` from controllers → `UserService`** → [ARCHITECTURE_AUDIT.md C-003](./ARCHITECTURE_AUDIT.md) · Effort: 1 h — Added `UserService.getUserIdByUsername()` (pure-domain resolver, `UnauthorizedException` on miss). New `AuthenticatedUserResolver` (`genesis-api/security`) is the single touchpoint of `SecurityContextHolder` for controllers and delegates id lookup to `UserService`. All `genesis-api` controllers (coref/pos/ner/wsd/export/log/recommendation/postag/editor) inject it; workspace + notification controllers drop their direct `UserRepository` access in favor of `UserService.getUserIdByUsername`. No repository access leaks into the API layer. Covered by `AuthenticatedUserResolverTest` + new `UserServiceTest` cases.
- [ ] 🟠 **Extract `ExportOrchestrationService`** (dedupe ExportController + ShareExportController) → [ARCHITECTURE_AUDIT.md C-006](./ARCHITECTURE_AUDIT.md) · Effort: 1 day
- [ ] 🟠 **Fix N+1 in `WorkspaceService.mapToResponse()`** → [ARCHITECTURE_AUDIT.md C-008](./ARCHITECTURE_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md F-PERF-03](./SYSTEM_DESIGN_AUDIT.md) · Effort: 3 h
- [ ] 🟠 **Fix O(N) loop in `EditorService.getDocumentContentWithOffset()`** — use stored `tokenStartIndex` → [ARCHITECTURE_AUDIT.md C-009](./ARCHITECTURE_AUDIT.md) · Effort: 1 h
- [ ] 🟠 **Switch `WorkspaceActivityListener` to `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`** → [ARCHITECTURE_AUDIT.md A-012](./ARCHITECTURE_AUDIT.md) · Effort: 30 min
- [ ] 🟠 **Paginate token/mention/cluster endpoints (cursor-based)** → [SYSTEM_DESIGN_AUDIT.md F-API-01, F-PERF-01](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟠 **Add Micrometer + Prometheus metrics** → [SYSTEM_DESIGN_AUDIT.md F-OBS-01](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟠 **Structured JSON logging + MDC propagation for `@Async`** → [SYSTEM_DESIGN_AUDIT.md F-OBS-03](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟠 **Add springdoc-openapi for typed FE contract** → [SYSTEM_DESIGN_AUDIT.md F-OBS-04](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [x] 🟠 **Notification listener — remove cross-module repo imports** → [ARCHITECTURE_AUDIT.md A-004](./ARCHITECTURE_AUDIT.md) · Effort: 4 h — Solved via dependency inversion rather than fat events (fat events would force import-export to fetch members for `DocumentTokenizedEvent`, colliding with A-006). New outbound port `RecipientDirectory` (genesis-notification) defines the module's needs (member ids, username, display name); `RecipientDirectoryAdapter` (genesis-api composition root, same pattern as A-007's UserDetailsServiceImpl) implements it over `WorkspaceMemberRepository`+`UserRepository`. `NotificationEventListener` and `NotificationService` drop their direct `WorkspaceMemberRepository`/`UserRepository` imports. Events stay thin; A-006 untouched. Added `spring-boot-starter-test` + first-ever notification tests (`NotificationEventListenerTest`, 3 cases) — the listener was previously untestable due to the repo coupling. `NotificationController`→`UserService` left as-is (sanctioned C-003 pattern, not a data-model coupling).
- [x] 🟠 **`AsyncDocumentProcessor` — publish event, drop `DocumentRepository` import** → [ARCHITECTURE_AUDIT.md A-006](./ARCHITECTURE_AUDIT.md) · Effort: 4 h — Inverted the flow: `AsyncDocumentProcessor` (import-export) no longer touches `Document`/`DocumentRepository`/`ProcessingStatus`. It publishes `DocumentProcessingStartedEvent` (new), `DocumentTokenizedEvent` (existing, on success — name sourced from the upload event's filename, no entity read), and `DocumentProcessingFailedEvent` (new). A new `DocumentProcessingStatusListener` in genesis-workspace owns all three status writes on its own `Document` (`@EventListener` + `@Transactional(REQUIRES_NEW)`, mirroring the old per-status commits). New events live in `workspace.event`. import-export `src/main` now has zero workspace repo/entity imports (only `workspace.event`, sanctioned). Tests: `DocumentProcessingStatusListenerTest` (4) + `AsyncDocumentProcessorTest` (2, event-publish on success/failure). **ArchUnit baseline ratcheted 194 → 177** (the guard auto-pruned the removed reaches — proof of progress).
- [ ] 🟡 **Add SSRF host whitelist to `FileStorageService.downloadAsString()`** → [SECURITY_AUDIT.md#medium-3](./SECURITY_AUDIT.md) · Effort: 30 min
- [ ] 🟡 **RFC 5987 filename encoding on `Content-Disposition`** → [SECURITY_AUDIT.md#medium-5](./SECURITY_AUDIT.md) · Effort: 30 min
- [ ] 🟡 **JSON-serialise audit log payloads with Jackson (not `String.format`)** → [ARCHITECTURE_AUDIT.md C-012](./ARCHITECTURE_AUDIT.md) · Effort: 1 h
- [x] 🟡 **Pin JWT algorithm explicitly: `Jwts.SIG.HS256`** → [SECURITY_AUDIT.md#low-1](./SECURITY_AUDIT.md) · Effort: 5 min — Bundled with JWT secret validation commit.
- [ ] 🟡 **`spring-dotenv` → `test` scope** → [SECURITY_AUDIT.md#low-3](./SECURITY_AUDIT.md) · Effort: 5 min

---

## P3 — Backlog (scaling + cleanup)

- [ ] 🟡 **Replace `compactClusterNumbers` two-phase flush with single SQL window-function UPDATE** → [ARCHITECTURE_AUDIT.md C-010](./ARCHITECTURE_AUDIT.md), [SYSTEM_DESIGN_AUDIT.md F-PERF-02](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 days
- [ ] 🟡 **Replace hand-rolled `HttpURLConnection` in `FileStorageService` with `RestClient`** → [ARCHITECTURE_AUDIT.md C-011](./ARCHITECTURE_AUDIT.md) · Effort: 1 day
- [ ] 🟡 **Stream ZIP export response** → [SYSTEM_DESIGN_AUDIT.md F-PERF-04](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 days
- [ ] 🟡 **Batch notification fan-out** → [SYSTEM_DESIGN_AUDIT.md F-SCALE-02](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 days
- [ ] 🟡 **Retry + circuit breaker around Cloudinary** → [SYSTEM_DESIGN_AUDIT.md F-SCALE-04](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟡 **Separate `@Async` thread pools by concern** → [SYSTEM_DESIGN_AUDIT.md F-SCALE-01](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟡 **Real `HealthIndicator` impls (replace hardcoded `true`)** → [SYSTEM_DESIGN_AUDIT.md F-OBS-02](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟡 **Refactor `genesis-recommend` projection repos (drop `DismissedRecommendationEntity` JPA-root hack)** → [ARCHITECTURE_AUDIT.md A-014](./ARCHITECTURE_AUDIT.md) · Effort: 1 day
- [ ] 🟡 **Extract `AuthService` (drain `AuthController`)** → [ARCHITECTURE_AUDIT.md C-007](./ARCHITECTURE_AUDIT.md) · Effort: 4 h
- [x] 🟢 **Add ArchUnit CI rule banning cross-module repo/service imports** → [ARCHITECTURE_AUDIT.md Sprint-1 #9](./ARCHITECTURE_AUDIT.md) · Effort: 4 h — `genesis-api/.../architecture/ModuleBoundaryTest` enforces: no class outside `api`/`common` may depend on another module's `..repository..`/`..entity..`. Uses ArchUnit `FreezingArchRule` — the 194 existing cross-module data-access touchpoints (A-001/A-002/A-005/A-006 debt) are frozen as a committed baseline (`src/test/resources/archunit_store`) so the build stays green, but any NEW reach fails the build; the store auto-prunes as fixes land (it's the live worklist). Verified both ways (passes vs baseline; fails on an injected probe reach). **Scope: repo/entity half only.** The service-import half is intentionally deferred — the dominant cross-module service dep is `WorkspaceAccessControl` (sanctioned cross-cutting security primitive, not debt); a blanket service ban would flag it. A-001 (`MentionService → DocumentService`) remains tracked as its own fix.
- [ ] 🟢 **`Collectors.toList()` → `Stream.toList()` (22 sites)** → [ARCHITECTURE_AUDIT.md C-015](./ARCHITECTURE_AUDIT.md) · Effort: 30 min
- [ ] 🟢 **Delete dead `AuditorAwareImpl` from `genesis-common`** → [ARCHITECTURE_AUDIT.md A-013](./ARCHITECTURE_AUDIT.md) · Effort: 5 min
- [ ] 🟢 **Move `JpaAuditingConfig` out of `genesis-common`** → [ARCHITECTURE_AUDIT.md C-020](./ARCHITECTURE_AUDIT.md) · Effort: 15 min
- [ ] 🟢 **Remove public `setId()` from `BaseEntity`** → [ARCHITECTURE_AUDIT.md C-019](./ARCHITECTURE_AUDIT.md) · Effort: 30 min
- [ ] 🟢 **Tests for `genesis-editor` and `genesis-notification`** → [ARCHITECTURE_AUDIT.md C-016, C-017](./ARCHITECTURE_AUDIT.md) · Effort: 1 day each
- [ ] 🟢 **Tag `GenesisApplicationTests` as integration; use H2 webEnv=NONE in CI** → [ARCHITECTURE_AUDIT.md C-018](./ARCHITECTURE_AUDIT.md) · Effort: 30 min
- [ ] 🟢 **Drop redundant `idx_documents_workspace_id`** → [SYSTEM_DESIGN_AUDIT.md F-DB-05](./SYSTEM_DESIGN_AUDIT.md) · Effort: 5 min
- [ ] 🟢 **Switch PKs to UUIDv7** → [SYSTEM_DESIGN_AUDIT.md F-DB-06](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 days
- [ ] 🟢 **Redis-backed STOMP broker** → [SYSTEM_DESIGN_AUDIT.md F-SCALE-03](./SYSTEM_DESIGN_AUDIT.md) · Effort: 2 days
- [ ] 🟢 **Least-privilege DB role** → [SYSTEM_DESIGN_AUDIT.md F-SEC-05](./SYSTEM_DESIGN_AUDIT.md) · Effort: 1 day
- [ ] 🟢 **Strong docker-compose Postgres password** → [SECURITY_AUDIT.md#info-1](./SECURITY_AUDIT.md) · Effort: 5 min

---

## Features (post-audit)

Items NOT flagged by the audit but identified separately. Tackled after the full P0–P3 audit list is clear.

- [ ] 🆕 **Real email verification flow (SMTP + token round-trip)** — Effort: 1–2 days backend + ~2 h frontend
  - **Why:** today `User.emailVerified` is a permanent `false`. The signup screen tells users to "check your inbox" but no email is ever sent; the frontend `/verify-email` page has `// TODO` blocks with hardcoded success after a 2-second timeout. Login does not gate on the flag, so anyone can sign up with any email — no proof of ownership.
  - **Free-tier transport:** start with **Gmail SMTP via App Password** (500/day on Workspace, ~100/day personal). Free, no third-party signup. Spring's `spring-boot-starter-mail` abstraction makes a later swap to Brevo / Resend / SES a 5-minute env-var change.
  - **Backend scope:**
    - Add `spring-boot-starter-mail` to `genesis-user/pom.xml` (or new `genesis-mail` module).
    - V*X* migration adds `email_verification_tokens(id, user_id, token_hash, expires_at, used_at, created_at)`.
    - `EmailService` with `sendVerificationEmail(email, rawToken)`; HTML + text templates.
    - `VerificationService`: `start(userId)` generates a 32-byte random token, stores SHA-256 hash, sends mail; `verify(rawToken)` looks up by hash, checks expiry, flips `User.emailVerified=true`, marks token used.
    - New endpoints: `POST /api/auth/verify-email` (body `{token}`), `POST /api/auth/resend-verification` (body `{email}`, returns 204 regardless of existence — anti-enumeration).
    - Rate-limit `resend-verification` to N/hour per email (reuses the P1 bucket4j infra once that lands).
    - Login gating — UX call: hard-block unverified users vs. soft banner. Default: soft banner for first iteration.
    - Config: `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`, `genesis.mail.from`, `genesis.mail.app-url` env vars; document in `env.example`.
  - **Frontend scope:**
    - Replace the two `// TODO` blocks in `src/app/verify-email/page.tsx` with real `fetch` calls.
    - Add a verification banner component shown on protected pages when `currentUser.emailVerified === false`.
  - **Deferred / nice-to-have:** password-reset flow reuses the same email infrastructure (separate feature, schedule after this lands).

- [ ] 🛠️ **Build-tooling cleanup (mvnw location, genesis.sh / .bat, .env loader, Byte Buddy flag)** — Effort: 30 min – 1 h
  - **Why:** the current build entry-points are inconsistent. `mvnw` and `mvnw.cmd` live under `genesis-api/` instead of the repo root; `genesis.sh` is checked in without the executable bit; the README and `developer-guide.md` reference a root-level `./mvnw` that doesn't exist; `genesis.sh` re-implements `.env` loading with `export $(grep -v '^#' .env | xargs)` which is brittle (mangles values containing `=`, e.g. base64 secrets) and redundant (`spring-dotenv` is already on the classpath and auto-loads `.env`); both wrapper scripts hard-code the Byte Buddy / Java 25 `-DargLine="-Dnet.bytebuddy.experimental=true"` flag that belongs in `genesis-api/pom.xml` Surefire config; `docker-compose` calls use v1 syntax (`docker compose` is v2).
  - **Scope (Path A — minimal fix):**
    - Move `mvnw`, `mvnw.cmd`, `.mvn/` from `genesis-api/` to the repo root.
    - Set the executable bit on `mvnw` and `genesis.sh` via `git update-index --chmod=+x`.
    - Update `genesis.sh` and `genesis.bat` to point at root-level `./mvnw` (`.cmd`).
    - Delete the manual `.env` export block from both scripts — `spring-dotenv` handles it.
    - Move the Byte Buddy `-DargLine` into `genesis-api/pom.xml` Surefire plugin config so it applies on every `mvn test` without script intervention.
    - `docker-compose` → `docker compose` (v2 plugin) in both scripts and any docs.
    - Mention `genesis.sh` / `genesis.bat` in the new README's commands section so newcomers discover them.
  - **Scope (Path C — optional graduation):** replace `genesis.sh` + `genesis.bat` with a single `justfile` (cross-platform, native on Mac/Linux/Windows, no WSL needed). Adds a one-time `brew install just` / `winget install just` per developer. Better long-term than maintaining two scripts in lockstep. Defer until after Path A is in place.
  - **Out of scope:** introducing Make (regresses Windows support — no native `make.exe`).

---

## Suggested order to start

1. **Today (under 1 hr total):** Cloudinary rotation → delete `DebugController` → restrict Actuator → fix WebSocket origins → disable SQL log in prod.
2. **This week:** JWT secret validation + IDOR checks on Workspace/Document mutations (CRITICAL-3 is the biggest exploitable surface).
3. **Sprint 1:** auth hardening (rate-limit, token rotation, headers) + FK migration V4.
4. **Sprint 2:** event-driven boundary fixes (A-001..A-007) + pagination + observability.
