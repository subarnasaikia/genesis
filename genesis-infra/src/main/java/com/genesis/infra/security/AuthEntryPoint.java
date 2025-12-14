package com.genesis.infra.security;

import com.genesis.common.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Entry point for handling unauthorized access attempts.
 * Returns a proper JSON error response instead of default HTML.
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = ErrorResponse.of(
                "UNAUTHORIZED",
                "You need to be authenticated to access this resource",
                request.getRequestURI(),
                HttpServletResponse.SC_UNAUTHORIZED);

        String json = String.format(
                "{\"success\":false,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                errorResponse.error(),
                errorResponse.message(),
                errorResponse.path(),
                errorResponse.status(),
                errorResponse.timestamp());

        response.getOutputStream().write(json.getBytes());
    }
}
