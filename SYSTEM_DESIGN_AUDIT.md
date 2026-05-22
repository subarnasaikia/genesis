# Genesis — System Design & Database Audit

**Date:** 2026-05-22
**Scope:** genesis-backend (Spring Boot 3.3 / Java 21 / PostgreSQL 15) + genesis-frontend (Next.js 15)
**Auditor:** Database Reviewer Agent

---

## Executive Summary

Genesis is a well-structured modular monolith with solid foundations: Flyway-managed migrations, Hibernate in `validate` mode, async document tokenization via `@Async` + `@TransactionalEventListener`, server-side HttpOnly cookies, and WebSocket notifications. The code is readable and follows consistent patterns.

The primary risk areas fall into three buckets:

1. **DB integrity gaps** — no foreign key constraints on the most-written annotation tables (tokens, mentions, POS/NER/WSD annotations, notifications). Orphaned rows accumulate silently.
2. **Unbounded list queries** — every core list endpoint returns `List<T>` with no pagination. At scale (10k+ tokens per document, 100+ workspaces) these become full table scans.
3. **Observability blind spots** — no Micrometer metrics, no distributed tracing, no structured JSON logging, health indicators are all hardcoded `true`. The system cannot be monitored or debugged in production without ad-hoc log trawling.

---

## Schema Relationship Map

```
users ──< workspace_members >── workspaces ──< documents >── stored_files
                                      │
                              coref_clusters ──< coref_mentions
                              pos_tag_definitions
                              wsd_sense ──< wsd_annotation
                              ner_tag_definitions
                              ner_annotations (no FK to documents)
                              pos_annotations  (no FK to tokens/documents)
                              dismissed_recommendations
                              annotation_log
                              notifications
                              editor_sessions

documents ──< tokens
documents ──< sentences  (implicit via document_id, no FK)
```

Key structural observation: `coref_mentions.cluster_id` is a bare UUID column — no FK to `coref_clusters`. The comment in `ClusterService.mergeClusters` calls this out as intentional ("safe after batch reassignment"), but it means orphaned mentions can persist without any constraint enforcement.

---

## Top 5 Scaling Risks

| Rank | Risk | Current State | Impact at Scale |
|---|---|---|---|
| 1 | **Unbounded token fetches** | `findByDocumentIdOrderByGlobalIndexAsc` returns the full `List<TokenEntity>` — used in export, editor load, CoNLL import | A single 9 MB CoNLL file produces 80–120k tokens. Loading them all into heap on every export or editor open will cause GC pressure and OOM at workspace scale |
| 2 | **Notification fan-out N+1** | `notifyWorkspaceMembers` loops over all members and calls `createNotification` (1 SELECT + 1 INSERT + 1 WebSocket push per member) inside an `@Async` thread | A workspace with 50 annotators triggers 50 DB writes and 50 WebSocket dispatches per document upload. Multiplied by concurrent uploads this saturates the 5-connection HikariCP pool |
| 3 | **Cluster compaction on every delete/merge** | `compactClusterNumbers` does a full ordered scan + two-phase flush of every cluster in a workspace on each delete and merge | A workspace with 500 clusters executes 1000 individual UPDATEs per merge operation |
| 4 | **CoNLL import transaction size** | `ConllMentionImportListener.onConllImported` runs in a single `@Transactional` that materialises all tokens per sentence per mention span, then does `saveAll` for all mentions | Train.conll (~9 MB, ~4000 sentences, ~100k tokens) holds a transaction open for the full parse duration, holding DB connections and locks |
| 5 | **Export workspace materialises all tokens** | `ExportService.getTokensBySentence` calls `findByDocumentIdOrderByGlobalIndexAsc` for every document in a workspace in a loop. For a 20-document workspace this is 20 unbounded full-table reads inside one HTTP request | Response body buffered entirely in `ByteArrayOutputStream` in heap before streaming — OOM risk for large workspaces |

---

## Top 10 Database Wins

| Priority | Win | Effort |
|---|---|---|
| 1 | Add FK constraints on annotation tables (F-DB-01) | Low — 1 migration |
| 2 | Add `recipient_id` + `read` composite index on `notifications` (F-DB-02) | Low — 1 migration |
| 3 | Add `processing_status` partial index on `documents` (F-DB-03) | Low — 1 migration |
| 4 | Tune HikariCP idle-timeout and keepalive (F-DB-09) | Low — config change |
| 5 | Replace `compactClusterNumbers` with export-time renumbering (F-SCALE-01) | Medium |
| 6 | Paginate token/sentence/mention list endpoints (F-PERF-01) | Medium |
| 7 | Batch notification inserts instead of per-row saves (F-SCALE-02) | Medium |
| 8 | Add partial index for unread notifications (F-DB-02) | Low |
| 9 | Fix `notifications.created_at` to use `timestamptz` (F-DB-04) | Low — 1 migration |
| 10 | Add `@Scheduled` cleanup for expired refresh tokens (F-DB-10) | Low |

---

## Detailed Findings

---

### DATABASE

---

#### F-DB-01 — Missing Foreign Key Constraints on Annotation Tables
**Severity:** High
**Category:** DB
**Location:** `V1__baseline.sql`, `V3__ner_tables.sql`

**Issue:**
The following columns hold logical foreign keys but have no FK constraint in the schema:

| Table | Column | Should reference |
|---|---|---|
| `coref_mentions` | `cluster_id` | `coref_clusters(id)` |
| `coref_mentions` | `document_id` | `documents(id)` |
| `coref_clusters` | `workspace_id` | `workspaces(id)` |
| `coref_mentions` | `workspace_id` | `workspaces(id)` |
| `pos_annotations` | `token_id` | `tokens(id)` |
| `pos_annotations` | `document_id` | `documents(id)` |
| `wsd_annotation` | `token_id` | `tokens(id)` |
| `wsd_annotation` | `sense_id` | `wsd_sense(id)` |
| `wsd_annotation` | `workspace_id` | `workspaces(id)` |
| `wsd_sense` | `workspace_id` | `workspaces(id)` |
| `ner_annotations` | `document_id` | `documents(id)` |
| `annotation_log` | `workspace_id` | `workspaces(id)` |
| `notifications` | `recipient_id` | `users(id)` |
| `notifications` | `workspace_id` | `workspaces(id)` |
| `dismissed_recommendations` | `user_id` | `users(id)` |
| `dismissed_recommendations` | `workspace_id` | `workspaces(id)` |
| `sentences` | `document_id` | `documents(id)` |
| `tokens` | `document_id` | `documents(id)` |
| `editor_sessions` | `workspace_id` | `workspaces(id)` |
| `editor_sessions` | `user_id` | `users(id)` |

Without these constraints: deleting a document leaves orphaned tokens, sentences, mentions, POS/WSD/NER annotations, and log rows. The application-layer cleanup in `DocumentService.delete` and the workspace deletion loop is the only guard, but it can miss rows added by future code paths.

**Fix:**
```sql
-- V4__add_annotation_fk_constraints.sql
ALTER TABLE coref_mentions
  ADD CONSTRAINT fk_mention_document  FOREIGN KEY (document_id)  REFERENCES documents(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_mention_cluster   FOREIGN KEY (cluster_id)   REFERENCES coref_clusters(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_mention_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE coref_clusters
  ADD CONSTRAINT fk_cluster_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE tokens    ADD CONSTRAINT fk_token_document    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
ALTER TABLE sentences ADD CONSTRAINT fk_sentence_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE pos_annotations
  ADD CONSTRAINT fk_pos_token    FOREIGN KEY (token_id)    REFERENCES tokens(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_pos_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE wsd_annotation
  ADD CONSTRAINT fk_wsd_token     FOREIGN KEY (token_id)     REFERENCES tokens(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_wsd_sense     FOREIGN KEY (sense_id)     REFERENCES wsd_sense(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_wsd_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE wsd_sense
  ADD CONSTRAINT fk_wsd_sense_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE ner_annotations
  ADD CONSTRAINT fk_ner_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE notifications
  ADD CONSTRAINT fk_notif_recipient  FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_notif_workspace  FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

ALTER TABLE editor_sessions
  ADD CONSTRAINT fk_editor_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_editor_user      FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE dismissed_recommendations
  ADD CONSTRAINT fk_dismissed_user      FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_dismissed_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

ALTER TABLE annotation_log
  ADD CONSTRAINT fk_log_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
```

---

#### F-DB-02 — Missing Index on notifications(recipient_id, read, created_at)
**Severity:** High
**Category:** DB / Performance
**Location:** `V1__baseline.sql`, `NotificationRepository.java`

**Issue:**
`NotificationRepository.findByRecipientIdOrderByCreatedAtDesc` and `countByRecipientIdAndReadFalse` filter on `recipient_id`. No index on this column exists in V1. The `markAllAsRead` path loads the entire notification list into heap before marking — for an active annotator with thousands of notifications this is a full sequential scan.

**Fix:**
```sql
-- Add to V4 migration
CREATE INDEX idx_notifications_recipient_read
  ON notifications (recipient_id, read, created_at DESC);
```

Replace `markAllAsRead` bulk-load pattern in `NotificationService`:
```java
// Replace the load-all-then-save pattern with a bulk UPDATE
@Modifying
@Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :userId AND n.read = false")
void markAllAsReadByUserId(@Param("userId") UUID userId);
```

---

#### F-DB-03 — Missing Partial Index on documents(processing_status)
**Severity:** Medium
**Category:** DB
**Location:** `V1__baseline.sql`

**Issue:**
`idx_documents_status` covers the `status` column but not `processing_status`. Queries for documents stuck in `PROCESSING` or `FAILED` require a full table scan.

**Fix:**
```sql
CREATE INDEX idx_documents_processing_status ON documents (processing_status)
  WHERE processing_status IN ('PENDING', 'PROCESSING', 'FAILED');
```

---

#### F-DB-04 — notifications.created_at Uses timestamp WITHOUT time zone
**Severity:** Medium
**Category:** DB
**Location:** `V1__baseline.sql` line 162, `Notification.java` line 45

**Issue:**
All other audit columns use `timestamp(6) with time zone`. `notifications.created_at` is `timestamp(6) without time zone` and the Java field is `LocalDateTime`. This breaks chronological ordering when the DB server or application server is not in UTC. It is also inconsistent with `BaseEntity` which uses `Instant` mapped to `timestamptz`.

**Fix:**
```sql
-- V4
ALTER TABLE notifications ALTER COLUMN created_at TYPE timestamp(6) with time zone
  USING created_at AT TIME ZONE 'UTC';
```
```java
// Notification.java
@CreatedDate
@Column(nullable = false, updatable = false)
private Instant createdAt;
```

---

#### F-DB-05 — Redundant Single-Column Index on documents(workspace_id)
**Severity:** Low
**Category:** DB
**Location:** `V1__baseline.sql`

**Issue:**
Both `idx_documents_workspace_id` (single column) and `idx_documents_order_index` (composite `workspace_id, order_index`) exist. The composite index already satisfies all queries filtered on `workspace_id` alone because `workspace_id` is the leading column. The single-column index adds write overhead on every INSERT with no query benefit.

**Fix:**
```sql
DROP INDEX idx_documents_workspace_id;
-- idx_documents_order_index covers workspace_id-only lookups via leading column
```

---

#### F-DB-06 — UUID Primary Keys Use Random UUID v4 Causing Index Fragmentation
**Severity:** Medium
**Category:** DB / Performance
**Location:** `BaseEntity.java` line 35

**Issue:**
`@GeneratedValue(strategy = GenerationType.UUID)` generates random UUID v4. PostgreSQL's B-tree index for UUID PKs suffers from page splits on every insert because random UUIDs land at non-monotonic positions. At scale (millions of tokens and annotations) this degrades insert throughput and inflates index page count.

**Fix:**
Switch to UUID v7 (time-ordered). A minimal implementation:
```java
// com/genesis/common/util/UuidV7.java
public class UuidV7 {
    public static UUID generate() {
        long ts = System.currentTimeMillis();
        long msb = (ts << 16) | (0x7000L) | (ThreadLocalRandom.current().nextLong() & 0x0FFFL);
        long lsb = (ThreadLocalRandom.current().nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}

// BaseEntity.java
@Id
@Column(name = "id", updatable = false, nullable = false)
private UUID id = UuidV7.generate();
// Remove @GeneratedValue
```

---

#### F-DB-07 — annotation_log CHECK Constraint Missing NER_ANNOTATED Action Type
**Severity:** Medium
**Category:** DB
**Location:** `V1__baseline.sql` line 41

**Issue:**
The `annotation_log_action_type_check` constraint lists:
`MENTION_CREATED, MENTION_DELETED, MENTION_ASSIGNED, CLUSTER_CREATED, CLUSTER_MERGED, POS_TAGGED, WSD_ANNOTATED`

There is no `NER_ANNOTATED` value. The `genesis-ner` module exists and follows the same annotation pattern as POS and WSD. If NER annotation logging is added following the existing pattern, the INSERT will fail at the DB constraint before any migration is written.

**Fix:**
```sql
-- V4
ALTER TABLE annotation_log DROP CONSTRAINT annotation_log_action_type_check;
ALTER TABLE annotation_log ADD CONSTRAINT annotation_log_action_type_check
  CHECK (action_type IN (
    'MENTION_CREATED','MENTION_DELETED','MENTION_ASSIGNED',
    'CLUSTER_CREATED','CLUSTER_MERGED',
    'POS_TAGGED','WSD_ANNOTATED','NER_ANNOTATED'
  ));
```
Update `ActionType` enum in `genesis-common` accordingly.

---

#### F-DB-08 — HikariCP Pool Too Small, Missing idle-timeout and keepalive-time
**Severity:** High
**Category:** DB / Performance
**Location:** `application.properties` lines 36–39

**Issue:**
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.initialization-fail-timeout=0
```

Problems:
- `minimum-idle=1` causes cold connection re-establishment on every traffic burst, adding ~10–50 ms latency spikes.
- `initialization-fail-timeout=0` means the app boots successfully even with no DB reachable. The first request fails instead of a clear startup error.
- No `idle-timeout` is set. HikariCP defaults to 600000 ms (10 minutes). Railway PostgreSQL times out idle connections after ~5 minutes, causing "connection closed" errors on the first request after a quiet period.
- No `keepalive-time` is set.

**Fix:**
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=3
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.keepalive-time=60000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.initialization-fail-timeout=1
```

---

#### F-DB-09 — Expired Refresh Tokens Never Purged
**Severity:** Medium
**Category:** DB
**Location:** `RefreshTokenService.java`, `RefreshTokenRepository.java`

**Issue:**
`deleteExpiredTokens()` exists but is never called from any scheduled task. Revoked and expired tokens accumulate in `refresh_tokens` indefinitely. The table grows without bound and `idx_refresh_tokens_token` index bloats.

**Fix:**
```java
// RefreshTokenService.java — add scheduled cleanup
@Scheduled(cron = "0 0 3 * * *") // 3 AM daily
@Transactional
public void cleanupExpiredTokens() {
    refreshTokenRepository.deleteAllExpiredTokens();
    log.info("Cleaned up expired refresh tokens");
}
```
Add `@EnableScheduling` to `AsyncConfig` or `InfraModuleConfig`.

---

#### F-DB-10 — Flyway baseline-on-migrate Silently Creates Empty Schema on New Databases
**Severity:** Medium
**Category:** DB / Migration
**Location:** `application.properties` lines 29–33

**Issue:**
`spring.flyway.baseline-on-migrate=true` with `baseline-version=1` means: on a brand-new empty database, Flyway marks V1 as already applied without running it, then applies V2 and V3 on top of an empty schema. V1 is the `pg_dump` baseline that creates the entire schema — skipping it leaves the database empty and the application fails to start with Hibernate `validate` errors.

This setting is correct only for existing databases that predate Flyway adoption. It should not be the default for all environments.

**Fix:**
Remove `baseline-on-migrate` from the default profile. Restrict it to a one-time ops flag:
```properties
# application.properties — remove:
# spring.flyway.baseline-on-migrate=true
# spring.flyway.baseline-version=1

# For existing non-Flyway-managed DBs, run once before deploying:
# flyway -url=... -baselineVersion=1 baseline
```

---

### SECURITY

---

#### F-SEC-01 — /api/debug/** Is Publicly Accessible
**Severity:** Critical
**Category:** Security
**Location:** `SecurityConfig.java` line 84

**Issue:**
```java
.requestMatchers("/api/debug/**").permitAll()
// "Debug endpoints (remove in production!)"
```
`DebugController` is unauthenticated by design. Any actor can call debug endpoints without credentials.

**Fix:**
Remove the `permitAll` rule and annotate `DebugController` with `@Profile("dev")`:
```java
// Remove from SecurityConfig:
// .requestMatchers("/api/debug/**").permitAll()

// DebugController.java
@Profile("dev")
@RestController
@RequestMapping("/api/debug")
public class DebugController { ... }
```

---

#### F-SEC-02 — WebSocket Endpoint Uses Wildcard Origin Pattern
**Severity:** High
**Category:** Security
**Location:** `WebSocketConfig.java` lines 30, 33

**Issue:**
```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")  // TODO: restrictive CORS in production
```
Both STOMP and raw WebSocket endpoints accept connections from any origin. This allows cross-origin WebSocket hijacking from arbitrary websites.

**Fix:**
Inject the same origin list used by the HTTP CORS config:
```java
@Value("${cors.allowed-origins}")
private String allowedOrigins;

registry.addEndpoint("/ws")
    .setAllowedOriginPatterns(allowedOrigins.split(","))
    .withSockJS();
```

---

#### F-SEC-03 — No Rate Limiting on Auth Endpoints
**Severity:** High
**Category:** Security
**Location:** `SecurityConfig.java`

**Issue:**
`/api/auth/login`, `/api/auth/signup`, and `/api/auth/refresh` have no rate limiting. The application is exposed to brute-force and credential stuffing attacks. No Bucket4j, no filter-level throttling, and no IP-based limiting exists anywhere in the codebase.

**Fix:**
Add Bucket4j with in-memory token buckets per IP:
```java
// AuthRateLimitFilter implements Filter
Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));
Bucket bucket = bucketCache.computeIfAbsent(clientIp,
    k -> Bucket.builder().addLimit(limit).build());
if (!bucket.tryConsume(1)) {
    response.setStatus(429);
    response.getWriter().write("{\"message\":\"Too many requests\"}");
    return;
}
```

---

#### F-SEC-04 — Actuator Exposes All Endpoints Without Authentication
**Severity:** High
**Category:** Security
**Location:** `application.properties` line 42

**Issue:**
```properties
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```
`/actuator/heapdump`, `/actuator/env`, `/actuator/beans`, `/actuator/loggers` are all publicly reachable without authentication. The `/actuator/heapdump` endpoint alone can expose JWT signing keys and database credentials from heap memory.

**Fix:**
```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics

# application-prod.properties (add)
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```
Additionally, restrict `/actuator/**` to authenticated admin roles in `SecurityConfig`.

---

#### F-SEC-05 — No Row Level Security on Any Table
**Severity:** High
**Category:** Security
**Location:** Schema-wide

**Issue:**
Workspace isolation is enforced entirely at the application layer. A bug in any service method — or a direct DB query by a developer — can return cross-tenant annotation data. Tables like `coref_mentions`, `pos_annotations`, `ner_annotations`, and `annotation_log` contain `workspace_id` columns that are never validated against the authenticated user's workspace memberships at the database layer.

Additionally, the application uses the PostgreSQL `postgres` superuser (per `docker-compose.yml`). The application role should be the least privileged role possible.

**Fix (minimum viable):**
```sql
-- Create a restricted application role
CREATE ROLE genesis_app LOGIN PASSWORD '...';
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO genesis_app;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
REVOKE SUPERUSER FROM genesis_app;
```

---

### PERFORMANCE

---

#### F-PERF-01 — Unbounded List Queries on High-Volume Tables
**Severity:** Critical
**Category:** Performance
**Location:** `TokenRepository.java`, `MentionRepository.java`, `NerAnnotationRepository.java`, `ExportService.java`

**Issue:**
The following methods return unbounded `List<T>`:
- `TokenRepository.findByDocumentIdOrderByGlobalIndexAsc` — used in export for every document
- `MentionRepository.findByWorkspaceId` — returns all mentions across all documents in a workspace
- `NerAnnotationRepository.findByDocumentId` — used in export, no pagination
- `ClusterRepository.findByWorkspaceIdOrderByClusterNumberAsc` — used in `getClusters` and `compactClusterNumbers`

A single tokenised CoNLL file at 100k tokens loads 100k `TokenEntity` objects into heap per query. In the export path this happens once per document within a single HTTP request.

**Fix — streaming for export:**
```java
// TokenRepository
@Query("SELECT t FROM TokenEntity t WHERE t.documentId = :documentId ORDER BY t.globalIndex ASC")
Stream<TokenEntity> streamByDocumentId(@Param("documentId") UUID documentId);
// Use inside a @Transactional method with try-with-resources on the Stream
```

**Fix — pagination for UI endpoints:**
```java
// MentionRepository
Page<MentionEntity> findByWorkspaceId(UUID workspaceId, Pageable pageable);
```

---

#### F-PERF-02 — compactClusterNumbers Is O(N) UPDATEs on Every Delete/Merge
**Severity:** High
**Category:** Performance
**Location:** `ClusterService.java` lines 252–283

**Issue:**
`compactClusterNumbers` runs two full flush cycles (`saveAllAndFlush` twice) of all clusters in the workspace on every delete and merge. For a workspace with 200 clusters, a single merge triggers 400 individual UPDATEs. The unique index on `(workspace_id, cluster_number)` adds index maintenance cost to every UPDATE.

**Recommendation:**
Tolerate gaps in `cluster_number` at rest. CoNLL export only needs sequential numbers in the output file — compute them at export time using `ROW_NUMBER() OVER (ORDER BY cluster_number)` and never write them back:
```sql
SELECT id, ROW_NUMBER() OVER (ORDER BY cluster_number) AS export_number
FROM coref_clusters
WHERE workspace_id = :workspaceId
ORDER BY cluster_number;
```

---

#### F-PERF-03 — N+1 COUNT Queries in WorkspaceService.mapToResponse
**Severity:** High
**Category:** Performance
**Location:** `WorkspaceService.java` lines 307–309

**Issue:**
```java
long totalDocs = documentRepository.countByWorkspaceId(workspace.getId());
long completedDocs = documentRepository.countByWorkspaceIdAndStatus(workspace.getId(), DocumentStatus.COMPLETE);
```
`getAllForUser` calls `mapToResponse` for every workspace in the list. Each call issues 2 COUNT queries. For a user with 20 workspaces, listing workspaces triggers 40 separate database round-trips.

**Fix — single aggregate query:**
```java
@Query("""
  SELECT d.workspace.id, COUNT(d), SUM(CASE WHEN d.status = 'COMPLETE' THEN 1 ELSE 0 END)
  FROM Document d WHERE d.workspace.id IN :workspaceIds
  GROUP BY d.workspace.id
  """)
List<Object[]> countDocumentStatsByWorkspaceIds(@Param("workspaceIds") List<UUID> workspaceIds);
```

---

#### F-PERF-04 — Export Buffers Entire ZIP in Heap Before Streaming
**Severity:** Medium
**Category:** Performance
**Location:** `ExportService.java` lines 206–290

**Issue:**
`exportAsZip` accumulates all document content into a `ByteArrayOutputStream` and returns a `byte[]` to the caller. The HTTP controller then sends this. For a 20-document workspace this can produce a 50–200 MB byte array on the heap before any response byte is written to the client.

**Fix — stream directly to the HTTP response:**
```java
// ExportController
return ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
    .body((StreamingResponseBody) outputStream -> {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            exportService.writeZipEntries(documents, corefAnnotations, options, workspaceName, zos);
        }
    });
```

---

### SYSTEM DESIGN

---

#### F-SCALE-01 — Async Thread Pool Too Small and Not Separated by Concern
**Severity:** High
**Category:** Scaling
**Location:** `AsyncConfig.java`

**Issue:**
```java
executor.setCorePoolSize(2);
executor.setMaxPoolSize(5);
executor.setQueueCapacity(100);
```
Tokenization (CPU-bound, seconds per file), notification fan-out (I/O-bound, many small writes), and audit logging all share this single pool. A single large CoNLL tokenization holds one thread for 10–30 seconds. With only 2 core threads, 3 concurrent uploads will queue. If the queue fills (100 items), `RejectedExecutionException` is thrown silently.

**Fix:**
Separate thread pools named explicitly:
```java
@Bean("tokenizationExecutor")
public Executor tokenizationExecutor() {
    ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
    e.setCorePoolSize(4); e.setMaxPoolSize(8); e.setQueueCapacity(500);
    e.setThreadNamePrefix("tokenize-");
    e.initialize();
    return e;
}

@Bean("notificationExecutor")
public Executor notificationExecutor() {
    ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
    e.setCorePoolSize(4); e.setMaxPoolSize(20); e.setQueueCapacity(2000);
    e.setThreadNamePrefix("notify-");
    e.initialize();
    return e;
}
```
Annotate callers: `@Async("tokenizationExecutor")`, `@Async("notificationExecutor")`.

---

#### F-SCALE-02 — Notification Fan-Out Is Per-Row with N+1 DB Writes
**Severity:** High
**Category:** Scaling
**Location:** `NotificationEventListener.java`, `NotificationService.java`

**Issue:**
`notifyWorkspaceMembers` iterates workspace members and calls `createNotification` per member. Each call: 1 INSERT + 1 user SELECT + 1 WebSocket send. For a workspace with 50 annotators, one `DocumentTokenizedEvent` causes 50 INSERTs + 50 user lookups + 50 WebSocket dispatches in a single async thread.

**Fix:**
```java
// Batch insert all notifications
List<Notification> batch = members.stream()
    .map(m -> buildNotification(m.getUser().getId(), title, message, type, workspaceId, actorId, link))
    .collect(Collectors.toList());
notificationRepository.saveAll(batch);

// Use workspace topic for WebSocket push instead of per-user queue
messagingTemplate.convertAndSend(
    "/topic/workspace/" + workspaceId + "/notifications",
    buildDto(title, message, type)
);
```

---

#### F-SCALE-03 — Single-Node In-Memory WebSocket Broker Prevents Horizontal Scaling
**Severity:** Medium
**Category:** Scaling
**Location:** `WebSocketConfig.java` line 22

**Issue:**
```java
config.enableSimpleBroker("/topic", "/queue");
```
Spring's `SimpleBroker` is in-memory. When the application is scaled to multiple instances (horizontal scaling on Railway or Kubernetes), WebSocket messages from instance A cannot reach users connected to instance B.

**Fix:**
Replace with a Redis-backed STOMP broker relay when horizontal scaling becomes necessary:
```java
config.enableStompBrokerRelay("/topic", "/queue")
    .setRelayHost(redisHost).setRelayPort(61613);
```

---

#### F-SCALE-04 — No Retry or Circuit Breaker for Cloudinary Downloads
**Severity:** High
**Category:** Resilience
**Location:** `AsyncDocumentProcessor.java` line 84

**Issue:**
Tokenization downloads file content from Cloudinary via an HTTP call:
```java
String content = fileStorageService.downloadAsString(event.getStoredFileUrl());
```
If Cloudinary is unreachable, the document is marked `FAILED` with no retry. There is no circuit breaker. A Cloudinary outage will cause all in-flight and subsequent document uploads to fail permanently until a user manually re-uploads.

**Fix:**
```java
// FileStorageService — add Spring Retry
@Retryable(
    value = {RuntimeException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000)
)
public String downloadAsString(String url) { ... }

@Recover
public String recoverDownload(RuntimeException e, String url) {
    log.error("Cloudinary download failed after retries: {}", url, e);
    throw e;
}
```
Add `spring-retry` dependency and `@EnableRetry` on `InfraModuleConfig`.

---

### OBSERVABILITY

---

#### F-OBS-01 — No Micrometer Metrics or Prometheus Export
**Severity:** High
**Category:** Observability
**Location:** `pom.xml` (genesis-api)

**Issue:**
`spring-boot-starter-actuator` is present but no Micrometer registry is configured. There is no `micrometer-registry-prometheus` dependency. There are no custom metrics for annotation throughput, tokenization duration, WebSocket connections, or export latency. The system cannot be monitored in production.

**Fix:**
```xml
<!-- genesis-api/pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
```
Add custom metrics to `ImportService` and `ExportService`:
```java
Timer.Sample sample = Timer.start(meterRegistry);
// ... operation ...
sample.stop(meterRegistry.timer("genesis.tokenization.duration",
    "status", "success", "format", isConll ? "conll" : "text"));
```

---

#### F-OBS-02 — Health Indicators Are Hardcoded true and Do Not Integrate with Actuator
**Severity:** Medium
**Category:** Observability
**Location:** `WorkspaceModuleHealthCheck.java`, `CorefModuleHealthCheck.java`, `ImportExportModuleHealthCheck.java`, `UserModuleHealthCheck.java`

**Issue:**
These health checks all return `true` unconditionally. They do not implement Spring Boot's `HealthIndicator` interface, so they do not appear under `/actuator/health`. The custom `/api/health` endpoint always reports `UP` regardless of actual module state.

**Fix:**
```java
// Example for CorefModuleHealthCheck
@Component("corefHealth")
public class CorefModuleHealthCheck implements HealthIndicator {
    private final ClusterRepository clusterRepository;

    @Override
    public Health health() {
        try {
            clusterRepository.count();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

---

#### F-OBS-03 — No Structured JSON Logging
**Severity:** Medium
**Category:** Observability
**Location:** `application.properties` lines 53–54

**Issue:**
Standard Logback text format with no MDC propagation across async boundaries. Log correlation across the tokenization chain (HTTP thread → `@Async` tokenization → `@EventListener` notification) is impossible. Production log lines cannot be parsed or queried in a log aggregator.

**Fix:**
```xml
<!-- Add to genesis-api/pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```
Add MDC propagation decorator to `AsyncConfig`:
```java
executor.setTaskDecorator(runnable -> {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    return () -> {
        if (mdcContext != null) MDC.setContextMap(mdcContext);
        try { runnable.run(); } finally { MDC.clear(); }
    };
});
```

---

#### F-OBS-04 — No OpenAPI / Swagger Documentation
**Severity:** Medium
**Category:** API
**Location:** All controllers

**Issue:**
There is no `springdoc-openapi` dependency. The frontend has no machine-readable API contract. DTO changes in the backend silently break the frontend until runtime. The frontend TypeScript types in `api.ts` are manually maintained and can drift from the actual backend DTOs.

**Fix:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```
Expose at `/v3/api-docs` for automated TypeScript client generation (`openapi-typescript-codegen`).

---

### API DESIGN

---

#### F-API-01 — No Pagination on Any List Endpoint
**Severity:** High
**Category:** API / Performance
**Location:** All controllers

**Issue:**
`GET /api/workspaces/{id}/mentions`, `GET /api/workspaces/{id}/clusters`, and all document-level annotation endpoints return unbounded `List<T>`. There is no `?page=&size=` contract on any endpoint.

**Fix:**
Adopt cursor-based pagination for high-volume resources (mentions, tokens):
```java
// MentionRepository
@Query("SELECT m FROM MentionEntity m WHERE m.workspaceId = :wsId AND m.id > :cursor ORDER BY m.id ASC")
List<MentionEntity> findPage(@Param("wsId") UUID wsId, @Param("cursor") UUID cursor, Pageable pageable);
```
Return `PagedApiResponse<T>` with `nextCursor`, `pageSize`, `hasMore` in the envelope.

---

#### F-API-02 — AnnotationLogRepository IS NULL Filter Pattern May Prevent Index Use
**Severity:** Low
**Category:** Performance
**Location:** `AnnotationLogRepository.java` lines 22–32

**Issue:**
```java
"AND (:actionType IS NULL OR l.actionType = :actionType) "
```
This pattern can prevent PostgreSQL from choosing an efficient index plan because the condition is not sargable when the parameter is null. PostgreSQL may prefer a sequential scan over the `idx_log_action_type` index even when `actionType` is specified.

**Recommendation:**
Verify with `EXPLAIN ANALYZE` under production data volume. If the plan is suboptimal, replace with `Specification<AnnotationLogEntity>` or separate query methods per filter combination.

---

### FRONTEND

---

#### F-FE-01 — No HTTP Caching on Server Component Fetches
**Severity:** Medium
**Category:** Performance
**Location:** `genesis-frontend/src/lib/server/api.ts`

**Issue:**
`serverFetch` issues a fresh `fetch()` on every render with no Next.js cache directive. Every navigation to `/home` reloads all workspace data from the Spring backend. There is no `next: { revalidate: N }` option, no SWR for client components, and no `Cache-Control` header from the backend.

**Fix:**
For data that changes infrequently (workspace list, cluster list):
```typescript
await serverFetch('/api/workspaces', { next: { revalidate: 30 } });
```
For high-frequency client data (editor annotations):
```typescript
const { data } = useSWR(`/api/workspaces/${id}/clusters`, fetcher, {
    refreshInterval: 5000,
    dedupingInterval: 2000,
});
```

---

## Prioritized Action List

### P0 — Fix Before First Production Users

| # | Action | Effort | Location |
|---|---|---|---|
| 1 | Remove `/api/debug/**` permitAll (F-SEC-01) | 5 min | `SecurityConfig.java` |
| 2 | Restrict Actuator exposure to health+metrics only (F-SEC-04) | 5 min | `application.properties`, `application-prod.properties` |
| 3 | Fix WebSocket CORS wildcard (F-SEC-02) | 10 min | `WebSocketConfig.java` |
| 4 | Fix HikariCP idle-timeout and keepalive (F-DB-08) | 10 min | `application.properties` |
| 5 | Remove Flyway baseline-on-migrate from default profile (F-DB-10) | 10 min | `application.properties` |

### P1 — Fix Within First Sprint

| # | Action | Effort | Location |
|---|---|---|---|
| 6 | Add FK constraints migration (F-DB-01) | 2 h | New V4 migration |
| 7 | Add notifications composite index + bulk markAllAsRead (F-DB-02) | 2 h | Migration + `NotificationService` |
| 8 | Fix `notifications.created_at` timezone (F-DB-04) | 1 h | Migration + `Notification.java` |
| 9 | Add annotation_log action type for NER (F-DB-07) | 30 min | V4 migration + `ActionType` enum |
| 10 | Add scheduled refresh token cleanup (F-DB-09) | 30 min | `RefreshTokenService.java` |
| 11 | Fix N+1 in `WorkspaceService.mapToResponse` (F-PERF-03) | 3 h | `WorkspaceService`, new repository query |

### P2 — Fix Within First Month

| # | Action | Effort | Location |
|---|---|---|---|
| 12 | Paginate token/mention/cluster endpoints (F-PERF-01, F-API-01) | 1 day | Multiple repositories + controllers |
| 13 | Add rate limiting on auth endpoints (F-SEC-03) | 1 day | New filter + Bucket4j dependency |
| 14 | Add Micrometer/Prometheus metrics (F-OBS-01) | 1 day | `pom.xml` + service annotations |
| 15 | Add structured JSON logging + MDC propagation (F-OBS-03) | 1 day | Logback config + `AsyncConfig` |
| 16 | Add OpenAPI/Swagger (F-OBS-04) | 1 day | `pom.xml` + controller annotations |
| 17 | Replace cluster compaction with export-time renumbering (F-PERF-02) | 2 days | `ClusterService`, `ExportService` |
| 18 | Drop redundant `idx_documents_workspace_id` (F-DB-05) | 30 min | V4 migration |

### P3 — Scaling Readiness

| # | Action | Effort | Location |
|---|---|---|---|
| 19 | Stream ZIP export response (F-PERF-04) | 2 days | `ExportService`, `ExportController` |
| 20 | Batch notification fan-out (F-SCALE-02) | 2 days | `NotificationEventListener`, `NotificationService` |
| 21 | Add retry/circuit breaker for Cloudinary (F-SCALE-04) | 1 day | `AsyncDocumentProcessor`, `CloudinaryService` |
| 22 | Separate async thread pools by concern (F-SCALE-01) | 1 day | `AsyncConfig` |
| 23 | Implement true `HealthIndicator` beans (F-OBS-02) | 1 day | All `*ModuleHealthCheck` files |
| 24 | Switch to Redis-backed STOMP broker (F-SCALE-03) | 2 days | `WebSocketConfig` + infrastructure |
| 25 | Switch to UUIDv7 PKs (F-DB-06) | 2 days | `BaseEntity` + custom generator |
| 26 | Create least-privilege DB role (F-SEC-05) | 1 day | Infrastructure / deployment config |

---

## Files Referenced

- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/db/migration/V1__baseline.sql`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/db/migration/V2__ensure_wsd_annotation_type.sql`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/db/migration/V3__ner_tables.sql`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/application.properties`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/application-prod.properties`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-common/src/main/java/com/genesis/common/entity/BaseEntity.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/async/AsyncConfig.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/SecurityConfig.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/RefreshTokenService.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/health/InfraModuleHealthCheck.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-notification/src/main/java/com/genesis/notification/config/WebSocketConfig.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-notification/src/main/java/com/genesis/notification/listener/NotificationEventListener.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-notification/src/main/java/com/genesis/notification/service/NotificationService.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-notification/src/main/java/com/genesis/notification/entity/Notification.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-coref/src/main/java/com/genesis/coref/service/ClusterService.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-coref/src/main/java/com/genesis/coref/repository/MentionRepository.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-import-export/src/main/java/com/genesis/importexport/service/AsyncDocumentProcessor.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-import-export/src/main/java/com/genesis/importexport/service/ExportService.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-workspace/src/main/java/com/genesis/workspace/service/WorkspaceService.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-workspace/src/main/java/com/genesis/workspace/health/WorkspaceModuleHealthCheck.java`
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/docker-compose.yml`
- `/Users/pocketfm/Desktop/Genesis/genesis-frontend/src/lib/server/api.ts`
