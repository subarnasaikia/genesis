package com.genesis.infra.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * <p>
 * Logs:
 * <ul>
 * <li>HTTP method and URL</li>
 * <li>Response status code</li>
 * <li>Request latency in milliseconds</li>
 * <li>Correlation ID (from MDC)</li>
 * </ul>
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
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
                request.getRequestURI(),
                response.getStatus(),
                latency,
                correlationId != null ? correlationId : "N/A");
    }
}
