package com.genesis.infra.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP request logging interceptor for capturing request metadata.
 *
 * <p>Logs HTTP method, URI (with query string), status, latency, and
 * the correlation id. Sensitive query-param values — currently the
 * {@code token} used by the share-link CoNLL download (eng-review D8)
 * — are replaced with the literal {@code REDACTED} via
 * {@link #redactQueryString(String)} so share links never appear in
 * access logs verbatim.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    /** Query params whose value must never appear in logs. */
    static final Set<String> REDACTED_QUERY_PARAMS = Set.of("token", "access_token");

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        log.info("Incoming request: {} {}", request.getMethod(), safePath(request));
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long latency = startTime != null ? System.currentTimeMillis() - startTime : -1;
        String correlationId = MDC.get("correlationId");

        log.info(
                "Completed request: {} {} | Status: {} | Latency: {}ms | CorrelationId: {}",
                request.getMethod(),
                safePath(request),
                response.getStatus(),
                latency,
                correlationId != null ? correlationId : "N/A");
    }

    /**
     * URI plus redacted query string. Returns just the path when there's
     * no query string — the common case.
     */
    static String safePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return uri;
        }
        return uri + "?" + redactQueryString(query);
    }

    /**
     * Replace the value of any {@link #REDACTED_QUERY_PARAMS} entry with
     * the literal string {@code REDACTED}. Param ordering preserved.
     */
    static String redactQueryString(String query) {
        StringBuilder out = new StringBuilder(query.length());
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) out.append('&');
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq < 0) {
                out.append(pair);
                continue;
            }
            String key = pair.substring(0, eq);
            if (REDACTED_QUERY_PARAMS.contains(key)) {
                out.append(key).append("=REDACTED");
            } else {
                out.append(pair);
            }
        }
        return out.toString();
    }
}
