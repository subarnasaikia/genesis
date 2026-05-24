package com.genesis.infra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP rate limiting on the public auth endpoints. Buckets live in memory
 * keyed by (endpoint, client IP). Closes SECURITY_AUDIT HIGH-4.
 *
 * <p>Limits (per IP, per endpoint, per minute):
 *
 * <ul>
 *   <li>{@code POST /api/auth/login} — 5
 *   <li>{@code POST /api/auth/signup} — 3
 *   <li>{@code POST /api/auth/refresh} — 10
 * </ul>
 *
 * <p>Over-limit requests get HTTP 429 with a {@code Retry-After} header and an
 * {@link ApiResponse}-shaped JSON body.
 *
 * <p>In-memory storage is sufficient for a single-node deployment; if we scale
 * to multiple replicas the buckets would need to move to Redis (bucket4j-redis)
 * to prevent per-node bypass.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    /**
     * Per-endpoint refill specs. {@link Bandwidth} is immutable, so we reuse a
     * single instance across every bucket built for the same endpoint.
     */
    private static final Map<String, Bandwidth> LIMITS = Map.of(
            "/api/auth/login",
            Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(1))
                    .build(),
            "/api/auth/signup",
            Bandwidth.builder()
                    .capacity(3)
                    .refillIntervally(3, Duration.ofMinutes(1))
                    .build(),
            "/api/auth/refresh",
            Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(1))
                    .build());

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Bandwidth limit = LIMITS.get(path);
        if (limit == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = path + "|" + clientIp;
        Bucket bucket = buckets.computeIfAbsent(
                key, k -> Bucket.builder().addLimit(limit).build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds =
                Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        logger.warn(
                "Rate limit exceeded for {} from {} — retry after {}s",
                path,
                clientIp,
                retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Hand-rolled JSON matching the project's ApiResponse envelope shape
        // (success, data, message, timestamp). Avoids pulling Jackson onto the
        // genesis-infra classpath solely for one error body.
        String body = "{\"success\":false,\"data\":null,\"message\":\"Too many requests. Try again in "
                + retryAfterSeconds + " seconds.\",\"timestamp\":\""
                + java.time.Instant.now().toString() + "\"}";
        response.getWriter().write(body);
    }

    /**
     * Resolve the client's IP, honouring {@code X-Forwarded-For} when behind a
     * trusted load balancer (Railway / Nginx). Uses the LEFTMOST entry — that's
     * the original client per RFC 7239 §5.2.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            return (comma == -1 ? xff : xff.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }
}
