# Genesis Backend — Security Audit Report

**Date:** 2026-05-22  
**Auditor:** Claude Security Reviewer (Sonnet 4.6)  
**Scope:** Full Spring Boot modular monolith — all 14 Maven modules  
**Methodology:** Manual static analysis, OWASP Top 10, auth flow tracing, IDOR mapping  

---

## Executive Summary

The Genesis backend is a well-structured Spring Boot application with several solid security foundations: BCrypt(12) password hashing, JJWT 0.12.3 signature verification, a functioning `GlobalExceptionHandler` that avoids raw stack trace leakage, and a CORS guard that refuses to boot with an empty `allowed-origins` list. However, the audit uncovered **4 Critical** and **7 High** severity findings that represent real exploit paths requiring immediate remediation before production exposure. The most impactful findings are: hardcoded live Cloudinary credentials committed to `.env` files tracked by the repository, an unauthenticated debug endpoint that exposes partial API keys, pervasive missing workspace authorization allowing any authenticated user to modify or delete any workspace/document, and a JWT access token exposed in URL query parameters (server logs).

---

## Findings

### CRITICAL-1 — Hardcoded Live Cloudinary Credentials in Committed .env Files

**Severity:** Critical  
**Category:** A02-Cryptographic-Failures / Secrets Management  
**Location:**
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/.env` lines 11–13
- `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/.env` lines 11–13

**Description:**  
Both `.env` files (root and `genesis-api/`) contain identical, live Cloudinary credentials:

```
CLOUDINARY_CLOUD_NAME=dxfcwhzok
CLOUDINARY_API_KEY=747718273962881
CLOUDINARY_API_SECRET=UX6AqVvVXZEW2OcVYXyjuD0JNKc
```

These are not placeholder values. The `DebugController` at `GET /api/debug/cloudinary-config` (unauthenticated — see CRITICAL-2) confirms they are loaded into the live application. The `.gitignore` does list `.env`, but having two committed `.env` files with real credentials in the working tree means any `git add .` slip, any IDE commit, or any CI artifact upload will exfiltrate them. The `genesis-api/.env` has no `.gitignore` override to exclude it.

**Risk:**  
An attacker with read access to the repository or the filesystem can use these credentials to: read/delete all stored annotation documents from Cloudinary, upload malicious files to your Cloudinary account, enumerate your Cloudinary storage, and incur billing charges.

**Fix:**  
1. Rotate the Cloudinary API key and secret immediately at https://cloudinary.com/console/settings/security.
2. Delete `genesis-api/.env` entirely. It should not exist.
3. Ensure the root `.env` is added to `.gitignore` at the module level as well, and run `git rm --cached .env genesis-api/.env` to remove any tracked versions.
4. Replace real values in both `.env` files with placeholder comments matching `env.example`.
5. Use a secrets manager (AWS Secrets Manager, Railway secrets, or Doppler) for production credentials.

---

### CRITICAL-2 — Unauthenticated Debug Endpoint Exposes Cloudinary Credentials and Tests Live API

**Severity:** Critical  
**Category:** A05-Security-Misconfiguration  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/java/com/genesis/api/controller/DebugController.java` lines 29–96  
**Security config:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/SecurityConfig.java` line 84 — `.requestMatchers("/api/debug/**").permitAll()`

**Description:**  
Two endpoints are fully public with no authentication:
- `GET /api/debug/cloudinary-config` — returns cloud name, first 4 chars and last 2 chars of the API key, first 4 chars of the API secret, and boolean `is_configured`.
- `GET /api/debug/cloudinary-test` — instantiates a live `Cloudinary` object with the production credentials and calls `cloudinary.api().usage()`, returning plan information and credit usage. On error it returns the exception message verbatim, which can reveal internal error strings.

The source file itself contains the comment "Remove in production!" but there is no environment guard, no `@Profile("!prod")` annotation, and the security config permits all requests to `/api/debug/**` even when `SPRING_PROFILES_ACTIVE=prod`.

**Risk:**  
Any unauthenticated user can learn partial Cloudinary credentials, verify they are correct, and monitor account usage. The `-test` endpoint is callable from any internet IP.

**Fix:**  
Delete `DebugController.java` entirely before deploying to any shared environment. If it is needed for local development, gate it with `@Profile("dev")` and remove the `permitAll` rule from `SecurityConfig`. The security config line `.requestMatchers("/api/debug/**").permitAll()` must be removed.

---

### CRITICAL-3 — Missing Authorization on Workspace and Document Mutation Endpoints (IDOR)

**Severity:** Critical  
**Category:** A01-Broken-Access-Control  
**Location (workspace):**
- `WorkspaceController.java` lines 83–99 (`PUT /{id}/status`, `PUT /{id}`) — no caller identity passed to service
- `WorkspaceController.java` lines 131–148 (`DELETE /{id}/members/{userId}`, `PUT /{id}/members/{userId}`) — no actor auth check
- `WorkspaceService.java` lines 100–118 (`update`) — no ownership/membership check
- `WorkspaceService.java` lines 252–256 (`updateStatus`) — no ownership/membership check
- `WorkspaceService.java` lines 178–195 (`removeMember`) — no check that caller is an ADMIN

**Location (document):**
- `DocumentController.java` lines 64–68 (`GET /documents/{id}`) — no workspace membership check
- `DocumentController.java` lines 73–79 (`PUT /documents/{id}/status`) — no auth check at all; `updateStatus` service method has no userId parameter
- `DocumentService.java` lines 128–147 (`updateStatus`) — no caller identity, no membership enforcement

**Description:**  
Any authenticated user can:

1. Call `PUT /api/workspaces/{anyId}/status?status=ARCHIVED` to archive any workspace they do not belong to.
2. Call `PUT /api/workspaces/{anyId}` to rename or change the description of any workspace.
3. Call `DELETE /api/workspaces/{anyId}/members/{anyUserId}` to remove a member from any workspace.
4. Call `PUT /api/workspaces/{anyId}/members/{anyUserId}?role=ANNOTATOR` to downgrade any admin.
5. Call `GET /api/documents/{anyId}` to read document metadata for any document.
6. Call `PUT /api/documents/{anyId}/status?status=COMPLETE` to mark any document complete.

The `delete` workspace method does accept a `userId` parameter but only uses it for `documentService.delete` — it never verifies that the caller is the workspace owner or an admin.

Similarly, `CoreferenceController`, `ExportController`, and `EditorController` perform zero workspace membership checks. Any authenticated user can read mentions, clusters, POS annotations, and export data for workspaces they are not members of.

**Risk:**  
Authenticated users can fully control workspaces and documents owned by other users. This is a horizontal privilege escalation affecting data integrity and confidentiality for all users.

**Fix:**  
Add a `requireMember` or `requireAdmin` helper to `WorkspaceService` (following the existing pattern in `AnnotationLogService`, `WsdSenseService`, and `RecommendationService`) and call it at the start of every mutating service method. For example:

```java
// WorkspaceService.java — add this helper
private void requireAdmin(UUID workspaceId, UUID callerId) {
    WorkspaceMember m = workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, callerId)
        .orElseThrow(() -> new UnauthorizedException("Not a member of this workspace"));
    if (m.getRole() != MemberRole.ADMIN) {
        throw new UnauthorizedException("Admin role required", true);
    }
}

// Then prefix every mutating method:
public WorkspaceResponse update(UUID id, UpdateWorkspaceRequest req, UUID callerId) {
    requireAdmin(id, callerId);
    // ...
}
```

Pass `callerId` from `WorkspaceController` (already retrieved via `getUserIdFromPrincipal`) to all mutating service calls. For `updateStatus` and `removeMember`, the caller UUID must be added as a parameter and passed through.

---

### CRITICAL-4 — Hardcoded Fallback JWT Secret in Source Code

**Severity:** Critical  
**Category:** A02-Cryptographic-Failures  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/SecurityProperties.java` line 26

```java
private String secret = "defaultSecretKeyThatShouldBeChangedInProduction12345";
```

**Description:**  
The `SecurityProperties.Jwt` class has a hardcoded fallback secret that is used if `genesis.security.jwt.secret` (backed by `JWT_SECRET`) is not set in the environment. This means if a deployment accidentally omits the `JWT_SECRET` environment variable, the application starts silently using this well-known, publicly audited string as the HMAC signing key. Any attacker who reads this source file (or this report) can forge valid JWTs for any username.

The `.env` files show `JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-256-bits-minimum` which is also a well-known placeholder, not a real secret.

**Risk:**  
If `JWT_SECRET` is not set at deployment time, anyone can generate tokens signed with the hardcoded secret, authenticate as any user including future admins, and exfiltrate all annotation data.

**Fix:**  
Remove the default value entirely. Use `@ConfigurationProperties` validation to require the property at startup:

```java
// In SecurityProperties.java
@NotBlank(message = "genesis.security.jwt.secret must be set")
@Size(min = 32, message = "JWT secret must be at least 256 bits (32 chars)")
private String secret;
```

Add `spring-boot-starter-validation` dependency and annotate `SecurityProperties` with `@Validated`. Also enforce a minimum secret length at runtime in `JwtTokenProvider`:

```java
// In JwtTokenProvider constructor
if (secret.length() < 32) {
    throw new IllegalStateException("JWT_SECRET is too short. Minimum 256 bits (32 ASCII chars) required.");
}
```

---

### HIGH-1 — All Spring Actuator Endpoints Publicly Exposed Without Authentication

**Severity:** High  
**Category:** A05-Security-Misconfiguration  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/application.properties` lines 42–44

```properties
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

**Security config:** `SecurityConfig.java` lines 75–77 only `permitAll` for `/actuator/health` and `/actuator/info`. However, `management.endpoints.web.exposure.include=*` exposes all endpoints including `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/loggers`, `/actuator/mappings`, `/actuator/threaddump`, and `/actuator/heapdump`.

The `anyRequest().authenticated()` rule in `SecurityConfig` does require auth for the non-whitelisted actuator endpoints. However, this is a misconfiguration risk: the full endpoint set is exposed and relies entirely on that single catch-all rule. Any future `permitAll` expansion could accidentally expose these.

**Risk:**  
`/actuator/env` reveals all environment variables, potentially including `JWT_SECRET`, `CLOUDINARY_API_SECRET`, `DB_PASSWORD` values (masked by default, but masking is bypassable). `/actuator/heapdump` provides a full JVM heap dump that can contain plaintext credentials extracted from memory.

**Fix:**  
Restrict exposed endpoints to only what is needed for health checks and monitoring:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

Never expose `env`, `heapdump`, `threaddump`, `configprops`, or `beans` in production.

---

### HIGH-2 — JWT Access Token Exposed in URL Query Parameter

**Severity:** High  
**Category:** A02-Cryptographic-Failures  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/JwtAuthenticationFilter.java` lines 74–78

```java
// Support for WebSocket (param access_token)
String paramToken = request.getParameter("access_token");
if (StringUtils.hasText(paramToken)) {
    return paramToken;
}
```

**Description:**  
The JWT filter accepts the access token as a URL query parameter `access_token`. URL query parameters are logged by the `RequestLoggingInterceptor` (which only redacts the parameter named `"token"`, not `"access_token"`), stored in browser history, appear in server access logs (Nginx/load balancer/proxy logs), and are included in HTTP `Referer` headers when navigating to external links.

**Risk:**  
Access tokens leaked from server logs or browser history allow session hijacking for the token's 15-minute lifetime. With a compromised token, an attacker can enumerate workspaces, download annotation data, and create/delete resources.

**Fix:**  
For WebSocket authentication, require the token in the STOMP `Authorization` header (already supported by `WebSocketAuthInterceptor`) rather than a URL parameter. Remove the `getParameter("access_token")` branch from `JwtAuthenticationFilter`. If the WebSocket client cannot send headers at connection time, use a short-lived (30s) one-time WebSocket ticket endpoint that issues a random opaque token tied to the user's session, which is then validated at the WebSocket handshake via a `HandshakeInterceptor`.

As an interim mitigation, update `RequestLoggingInterceptor.REDACTED_QUERY_PARAMS` to also include `"access_token"`.

---

### HIGH-3 — WebSocket Endpoint Allows Any Origin

**Severity:** High  
**Category:** A01-Broken-Access-Control / A05-Security-Misconfiguration  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-notification/src/main/java/com/genesis/notification/config/WebSocketConfig.java` lines 29–33

```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")  // TODO: restrictive CORS in production
        .withSockJS();
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*"); // Fallback for direct WebSocket
```

**Description:**  
The WebSocket STOMP endpoint accepts connections from any origin (`*`). This is separate from the HTTP CORS configuration which properly enforces `cors.allowed-origins`. A malicious page on any domain can initiate a WebSocket connection to the backend using a victim user's credentials. The comment acknowledges this as a known issue.

**Risk:**  
Cross-Site WebSocket Hijacking (CSWSH). An attacker who tricks a logged-in user into visiting a malicious page can establish a WebSocket connection to the backend as that user and receive real-time notifications intended for the victim.

**Fix:**  
Replace the wildcard with the same allowed origins list used by the HTTP CORS config:

```java
// In WebSocketConfig.java
@Value("${cors.allowed-origins}")
private String allowedOrigins;

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    String[] origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim).toArray(String[]::new);
    registry.addEndpoint("/ws")
            .setAllowedOrigins(origins)
            .withSockJS();
    registry.addEndpoint("/ws")
            .setAllowedOrigins(origins);
}
```

---

### HIGH-4 — No Rate Limiting on Authentication Endpoints

**Severity:** High  
**Category:** A07-Identification-and-Authentication-Failures  
**Location:**  
- `AuthController.java` — `POST /api/auth/login`, `POST /api/auth/signup`, `POST /api/auth/refresh`
- No rate-limiting dependency found anywhere in `pom.xml` files or Java source

**Description:**  
There is no rate limiting applied to any endpoint. A search across all 14 modules found no reference to `bucket4j`, `resilience4j`, or any custom filter implementing request throttling. The `/api/auth/login` endpoint in particular has no protection against credential stuffing or brute-force attacks.

The `BCryptPasswordEncoder(12)` provides some natural brute-force resistance per attempt (~250ms), but at scale an attacker with a botnet can still attempt thousands of credentials per minute across distributed source IPs.

**Risk:**  
Automated credential stuffing attacks against `/api/auth/login`. Account enumeration through timing differences in `/api/auth/signup` (different error paths for duplicate username vs duplicate email). Refresh token grinding against `/api/auth/refresh`.

**Fix:**  
Add `bucket4j-spring-boot-starter` or Spring's rate limiting support and configure per-IP rate limits:

```xml
<!-- pom.xml in genesis-infra -->
<dependency>
    <groupId>com.giffing.bucket4j.spring.boot.starter</groupId>
    <artifactId>bucket4j-spring-boot-starter</artifactId>
    <version>0.9.0</version>
</dependency>
```

Target at minimum: 5 requests/minute/IP on `/api/auth/login`, 3 requests/minute/IP on `/api/auth/signup`, and 10 requests/minute/IP on `/api/auth/refresh`.

---

### HIGH-5 — SQL Logging Enabled With No Production Override

**Severity:** High  
**Category:** A09-Security-Logging-and-Monitoring-Failures / Information Disclosure  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/resources/application.properties` lines 20–21 and 54

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.com.genesis=DEBUG
```

**Description:**  
Both `show-sql` and `format_sql` are set to `true` in the base `application.properties`. The `application-prod.properties` file contains only the CORS override and does not disable these. When `SPRING_PROFILES_ACTIVE=prod`, the `prod` profile is loaded on top of the base profile, leaving SQL logging and DEBUG-level application logging active in production.

Every SELECT, INSERT, UPDATE, and DELETE query — including those containing user email addresses, annotation data, and workspace names — is written to the application log. `logging.level.com.genesis=DEBUG` globally exposes internal service state in log management platforms.

**Risk:**  
SQL logs containing PII (email addresses, usernames, annotation content) are written to log aggregation systems and accessible to anyone with log access. Debug logging can expose sensitive business logic state.

**Fix:**  
Add to `application-prod.properties`:

```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
logging.level.com.genesis=INFO
logging.level.org.springframework.security=WARN
```

---

### HIGH-6 — Missing Security Response Headers (CSP, HSTS, X-Frame-Options)

**Severity:** High  
**Category:** A05-Security-Misconfiguration  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/SecurityConfig.java`

**Description:**  
The `SecurityFilterChain` does not configure any security response headers. `Content-Security-Policy`, `Strict-Transport-Security` (HSTS), `X-Content-Type-Options`, `Referrer-Policy`, and `Permissions-Policy` are not explicitly configured. There is no `headers()` customizer in `securityFilterChain`.

**Risk:**  
Lack of HSTS means connections can be downgraded from HTTPS to HTTP. Lack of CSP means the risk surface for any XSS is wider. Lack of `X-Content-Type-Options: nosniff` enables MIME-type sniffing attacks against file download endpoints.

**Fix:**  
Add headers configuration to `securityFilterChain`:

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; frame-ancestors 'none'"))
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true).maxAgeInSeconds(31536000))
    .referrerPolicy(ref -> ref.policy(
        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
);
```

---

### HIGH-7 — User Enumeration via Distinct Error Messages on Signup

**Severity:** High  
**Category:** A07-Identification-and-Authentication-Failures  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-user/src/main/java/com/genesis/user/service/UserService.java` lines 38–44

```java
if (userRepository.existsByUsername(request.getUsername())) {
    throw new IllegalArgumentException("Username already exists: " + request.getUsername());
}
if (userRepository.existsByEmail(request.getEmail())) {
    throw new IllegalArgumentException("Email already exists: " + request.getEmail());
}
```

**Description:**  
The signup endpoint returns distinct error messages depending on which field is a duplicate. This allows an attacker to enumerate registered usernames and email addresses. The exact username or email is also echoed back in the error message. `GlobalExceptionHandler` at line 197–204 propagates the `IllegalArgumentException` message directly to the HTTP response body, making the exact username/email visible in the API response.

Additionally, `WorkspaceService.addMember` at line 155 reveals whether an email address is registered: `"User with email " + request.getEmail() + " not found"` — any workspace member can probe arbitrary email addresses.

**Risk:**  
An attacker can build a list of registered usernames and emails, which can be used for targeted phishing, credential stuffing with known usernames, or user base enumeration.

**Fix:**  
Return a generic message for both cases:

```java
// UserService.java
if (userRepository.existsByUsername(request.getUsername())
    || userRepository.existsByEmail(request.getEmail())) {
    throw new ValidationException("signup", "Username or email is already registered");
}
```

---

### MEDIUM-1 — No Refresh Token Rotation on Use

**Severity:** Medium  
**Category:** A07-Identification-and-Authentication-Failures  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/java/com/genesis/api/controller/AuthController.java` lines 104–121

**Description:**  
The refresh endpoint returns the same refresh token value on each call (`request.getRefreshToken()` is returned unchanged at line 119). Refresh tokens should be rotated on every use: the old token is invalidated and a new one is issued. Without rotation, a stolen refresh token grants indefinite access (7 days by default) without any detection signal.

**Risk:**  
If a refresh token is exfiltrated, an attacker can silently maintain access for up to 7 days without triggering any session anomaly.

**Fix:**  

```java
// AuthController.java — refresh endpoint
RefreshToken newRefreshToken = refreshTokenService.rotateToken(existingRefreshToken);
TokenResponse tokenResponse = TokenResponse.of(
    accessToken,
    newRefreshToken.getToken(),
    jwtTokenProvider.getAccessTokenExpiryMs() / 1000);
```

```java
// RefreshTokenService.java
@Transactional
public RefreshToken rotateToken(RefreshToken old) {
    old.setRevoked(true);
    refreshTokenRepository.save(old);
    return createRefreshToken(old.getUser());
}
```

---

### MEDIUM-2 — `EditorController.getUserId` Generates Deterministic UUID from Username (Not Real DB ID)

**Severity:** Medium  
**Category:** A01-Broken-Access-Control  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/src/main/java/com/genesis/api/controller/EditorController.java` line 183

```java
private UUID getUserId(Principal principal) {
    // In a real implementation, this would extract UUID from the principal
    // For now, use a deterministic UUID based on username
    return UUID.nameUUIDFromBytes(principal.getName().getBytes());
}
```

**Description:**  
`EditorController` generates a UUID by hashing the username string instead of querying the database for the user's real UUID. The UUID used in `EditorService.openWorkspace`, `getSession`, `saveSession`, and `closeSession` does not match the real user UUID in the database. This means editor sessions are stored under a UUID that is not the user's actual database ID, foreign-key relationships are broken, and if a user changes their username their session UUID silently changes, orphaning all previous sessions.

**Risk:**  
Incorrect user identity in editor sessions, potential data cross-contamination, orphaned session records in the database.

**Fix:**  
Use the same DB lookup pattern as other controllers:

```java
private UUID getUserId(Principal principal) {
    return userRepository.findByUsernameOrEmail(principal.getName(), principal.getName())
        .orElseThrow(() -> new UnauthorizedException("User not found"))
        .getId();
}
```

Inject `UserRepository` into `EditorController`.

---

### MEDIUM-3 — SSRF Risk in `FileStorageService.downloadAsString`

**Severity:** Medium  
**Category:** A10-Server-Side-Request-Forgery  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/storage/FileStorageService.java` lines 138–163

```java
public String downloadAsString(@NonNull String fileUrl) {
    java.net.URL url = new java.net.URL(fileUrl);
    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
```

**Description:**  
`downloadAsString` opens an `HttpURLConnection` to whatever URL string is passed with no host validation or protocol whitelist. The callers are `AsyncDocumentProcessor.java` (low risk, uses URL from DB) and `EditorController.java` (uses `docInfo.getStoredFileUrl()` from the database). Currently the URL is always derived from Cloudinary upload results and is not directly user-supplied. However, the method has no URL whitelist, making it fragile to any future code path that passes user-influenced data to it.

**Risk:**  
If a future feature passes attacker-controlled input to `downloadAsString`, the server would make requests to internal infrastructure, cloud metadata endpoints (`http://169.254.169.254`), or internal services. This is a latent SSRF vulnerability.

**Fix:**  
Add URL validation to `downloadAsString`:

```java
private static final String ALLOWED_CLOUDINARY_HOST = "res.cloudinary.com";

public String downloadAsString(String fileUrl) {
    URL url = new URL(fileUrl);
    if (!"https".equals(url.getProtocol())) {
        throw new GenesisException("Only HTTPS downloads are permitted");
    }
    if (!url.getHost().endsWith(ALLOWED_CLOUDINARY_HOST)) {
        throw new GenesisException("Disallowed download URL host: " + url.getHost());
    }
    // ... existing connection logic
}
```

---

### MEDIUM-4 — Coref and POS Services Do Not Verify Workspace Membership

**Severity:** Medium  
**Category:** A01-Broken-Access-Control  
**Location:**
- `genesis-coref/MentionService.java` — `createMention`, `getMentionsByWorkspace`, `getMentionsByDocument`, `assignToCluster`, `unassignFromCluster`, `deleteMention`
- `genesis-coref/ClusterService.java` — all cluster operations
- `genesis-pos/PosTaggingService.java` — `updatePos`, `batchUpdate`, `getAnnotationsByToken`, `getAnnotationsByDocument`

**Description:**  
Unlike `WsdAnnotationService`, `WsdSenseService`, `RecommendationService`, and `AnnotationLogService` — which all call `requireMember(workspaceId, callerId)` — the coreference and POS services perform no workspace membership check. Any authenticated user can read or write mentions, clusters, and POS annotations for workspaces they are not members of.

`PosController.currentAnnotator()` also falls back to the string `"system"` if the user is unauthenticated:

```java
private String currentAnnotator() {
    // ...
    return "system";  // falls through if not authenticated
}
```

This would allow POS annotation writes under the annotator ID `"system"` if the auth filter is bypassed.

**Risk:**  
Horizontal privilege escalation across annotation data. Any authenticated user can corrupt coreference and POS annotations for workspaces they do not belong to.

**Fix:**  
Follow the `WsdAnnotationService` pattern. Pass `workspaceId` and `callerId` to service methods and call `requireMember` before any data access. Change `PosController.currentAnnotator()` to throw `UnauthorizedException` rather than returning `"system"` when unauthenticated.

---

### MEDIUM-5 — Header Injection Risk in Content-Disposition Filename (String Concatenation)

**Severity:** Medium  
**Category:** A03-Injection  
**Location:**
- `ExportController.java` lines 82, 130
- `ShareExportController.java` line 136
- `WsdController.java` line 140

```java
.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
```

**Description:**  
The `Content-Disposition` filename is constructed by string concatenation. Although `ExportService.sanitizeFilename` strips non-alphanumeric characters from workspace/document names before use, the pattern is fragile. Any future code path that passes unsanitized user input to a `Content-Disposition` header could introduce HTTP header injection allowing attackers to inject arbitrary response headers or modify download behavior.

**Fix:**  
Use RFC 5987 encoding for filenames consistently:

```java
String encodedFilename = URLEncoder.encode(sanitizedName, StandardCharsets.UTF_8)
    .replace("+", "%20");
.header(HttpHeaders.CONTENT_DISPOSITION,
    "attachment; filename=\"" + sanitizedName + "\"; filename*=UTF-8''" + encodedFilename)
```

---

### LOW-1 — JWT Algorithm Not Explicitly Pinned

**Severity:** Low  
**Category:** A02-Cryptographic-Failures  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-infra/src/main/java/com/genesis/infra/security/JwtTokenProvider.java` lines 47–53, 127–131

**Description:**  
The `generateAccessToken` and `generateToken` methods call `.signWith(signingKey)` without specifying the algorithm. JJWT 0.12.x infers the algorithm from the key type, so this is currently safe, but the algorithm is implicit rather than enforced.

**Fix:**  
Make the algorithm explicit: `.signWith(signingKey, Jwts.SIG.HS256)`.

---

### LOW-2 — Refresh Token Accumulates Revoked Records (No Cleanup Job)

**Severity:** Low  
**Category:** A07-Identification-and-Authentication-Failures  
**Location:** `RefreshTokenService.deleteExpiredTokens()` — method exists but no `@Scheduled` task calls it

**Description:**  
The `logout` endpoint marks refresh tokens as `revoked = true` but does not delete them. `deleteExpiredTokens()` exists in `RefreshTokenRepository` but no scheduled job invokes it. Over time, the `refresh_tokens` table accumulates revoked/expired records.

**Fix:**  

```java
@Scheduled(cron = "0 0 3 * * *")  // 3 AM daily
public void cleanupExpiredTokens() {
    refreshTokenService.deleteExpiredTokens();
}
```

---

### LOW-3 — `spring-dotenv` Is a Runtime Dependency (Not Test-Scoped)

**Severity:** Low  
**Category:** A05-Security-Misconfiguration  
**Location:** `/Users/pocketfm/Desktop/Genesis/genesis-backend/genesis-api/pom.xml` lines 121–124

```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

**Description:**  
`spring-dotenv` is included as a runtime (not test-scoped) dependency. In production, property values should come from real environment variables, not parsed `.env` files. Having the dotenv library active in production means a `.env` file accidentally included in the deployment artifact would be silently loaded.

**Fix:**  
Add `<scope>test</scope>` or remove the dependency entirely and rely on the `env_file` directive in `docker-compose.yml` (which already handles `.env` injection at the OS level).

---

### INFO-1 — Docker Compose Uses Default postgres/postgres Credentials

**Severity:** Info  
**Category:** A07-Identification-and-Authentication-Failures  
**Location:** `docker-compose.yml` lines 7–10

The Docker Compose configuration hardcodes `POSTGRES_PASSWORD=postgres`. Acceptable for local development but must not be used in any shared or staging environment. The `docker-compose.yml` is committed to the repository.

---

### INFO-2 — `UsernameNotFoundException` Message Logged at WARN Level

**Severity:** Info  
**Category:** A09-Security-Logging-and-Monitoring-Failures  
**Location:** `UserDetailsServiceImpl.java` lines 29–31

```java
throw new UsernameNotFoundException(
    "User not found with username or email: " + usernameOrEmail);
```

Spring Security converts this to `BadCredentialsException` before it reaches the response, so it does not directly expose the username to callers. However, the full message including the attempted username appears in WARN-level logs, which are visible in log aggregation systems.

---

## Prioritized Action List (Top 10)

| Priority | Finding | Effort | Action |
|---|---|---|---|
| 1 | CRITICAL-1 | 30 min | Rotate Cloudinary credentials immediately. Delete `genesis-api/.env`. Verify `.gitignore` coverage. |
| 2 | CRITICAL-2 | 15 min | Delete `DebugController.java`. Remove `/api/debug/**` from `permitAll` in `SecurityConfig`. |
| 3 | CRITICAL-4 | 1 hour | Remove hardcoded JWT fallback secret. Add `@NotBlank` `@Validated` startup check. Add minimum-length guard in `JwtTokenProvider`. |
| 4 | CRITICAL-3 | 1–2 days | Add `requireAdmin`/`requireMember` checks to `WorkspaceService.update`, `updateStatus`, `removeMember`, `updateMemberRole`, and `DocumentService.updateStatus`. Pass `callerId` through all mutating controller paths. |
| 5 | HIGH-1 | 15 min | Set `management.endpoints.web.exposure.include=health,info,metrics` and `show-details=when-authorized` in `application-prod.properties`. |
| 6 | HIGH-4 | 1 day | Add bucket4j or equivalent rate limiting on `/api/auth/login`, `/api/auth/signup`, `/api/auth/refresh`. |
| 7 | HIGH-2 | 2 hours | Remove `access_token` URL query param support from `JwtAuthenticationFilter`. Add `access_token` to `REDACTED_QUERY_PARAMS` as interim. |
| 8 | HIGH-3 | 1 hour | Replace `setAllowedOriginPatterns("*")` with `cors.allowed-origins` value in `WebSocketConfig`. |
| 9 | HIGH-5 | 15 min | Disable SQL logging and reduce log level to INFO in `application-prod.properties`. |
| 10 | MEDIUM-1 | 2 hours | Implement refresh token rotation (`rotateToken` method in `RefreshTokenService`, call it from `AuthController.refresh`). |

---

## OWASP Top 10 Coverage Matrix

| OWASP Category | Status | Key Findings |
|---|---|---|
| A01 Broken Access Control | FAIL | CRITICAL-3 (workspace IDOR), MEDIUM-4 (coref/POS missing membership checks) |
| A02 Cryptographic Failures | FAIL | CRITICAL-4 (hardcoded JWT secret), HIGH-2 (token in URL parameter), LOW-1 (implicit JWT algorithm) |
| A03 Injection | PASS | All JPQL uses parameterized queries; MEDIUM-5 header injection pattern is low-impact currently |
| A04 Insecure Design | PASS | Event-driven architecture, no direct cross-module service calls, BCrypt(12) |
| A05 Security Misconfiguration | FAIL | CRITICAL-2 (unauthenticated debug endpoint), HIGH-1 (Actuator wildcard), HIGH-3 (WebSocket CORS wildcard), HIGH-6 (no security headers) |
| A06 Vulnerable/Outdated Components | PASS | Spring Boot 3.3.3, JJWT 0.12.3, Cloudinary 1.38.0 — all current as of audit date |
| A07 Auth Failures | FAIL | HIGH-4 (no rate limiting), HIGH-7 (user enumeration), MEDIUM-1 (no token rotation), MEDIUM-2 (wrong UUID in editor) |
| A08 Software/Data Integrity | PASS | Flyway migrations, no deserialization of untrusted user input |
| A09 Logging/Monitoring | FAIL | HIGH-5 (SQL logging in prod), CRITICAL-1 (live credentials in committed files), INFO-2 (username in auth failure logs) |
| A10 SSRF | MEDIUM | MEDIUM-3 — `downloadAsString` lacks URL whitelist; not currently exploitable but structurally vulnerable |

---

## Secrets Exposure Summary

| Secret | Status | Location | Action Required |
|---|---|---|---|
| `CLOUDINARY_API_KEY=747718273962881` | **LIVE — EXPOSED** | `.env` line 12, `genesis-api/.env` line 12 | Rotate immediately |
| `CLOUDINARY_API_SECRET=UX6AqVvVXZEW2OcVYXyjuD0JNKc` | **LIVE — EXPOSED** | `.env` line 13, `genesis-api/.env` line 13 | Rotate immediately |
| JWT fallback secret (hardcoded in source) | **HARDCODED** | `SecurityProperties.java` line 26 | Remove default, enforce startup validation |
| `JWT_SECRET` placeholder in `.env` | Low risk (placeholder) | `.env` line 8 | Replace with real 256-bit random secret before deploying |
| `DB_PASSWORD=postgres` | Low risk (local only) | `.env`, `docker-compose.yml` | Use strong password in staging/prod |

---

*Report generated by manual static analysis. No automated scanning tools were run. All findings are based on direct code review of source files and configuration.*
