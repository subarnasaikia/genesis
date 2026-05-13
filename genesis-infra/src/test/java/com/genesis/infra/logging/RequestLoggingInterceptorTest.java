package com.genesis.infra.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that share tokens never reach the access log.
 * (eng-review D8 — token-in-query redaction.)
 */
class RequestLoggingInterceptorTest {

    @Test
    @DisplayName("token value is replaced with REDACTED")
    void redactsTokenParam() {
        String input = "token=eyJhbGciOiJIUzI1NiJ9.foo.bar";
        String out = RequestLoggingInterceptor.redactQueryString(input);
        assertEquals("token=REDACTED", out);
    }

    @Test
    @DisplayName("non-sensitive params pass through verbatim")
    void nonSensitiveParamsPreserved() {
        String input = "page=0&size=50";
        assertEquals("page=0&size=50",
                RequestLoggingInterceptor.redactQueryString(input));
    }

    @Test
    @DisplayName("mixed params: redact only the token, keep ordering")
    void mixedParamsOnlyRedactToken() {
        String input = "page=0&token=secret-jwt&size=50";
        String out = RequestLoggingInterceptor.redactQueryString(input);
        assertEquals("page=0&token=REDACTED&size=50", out);
    }

    @Test
    @DisplayName("param without =value is preserved")
    void valuelessParamPreserved() {
        String input = "flag&token=secret";
        String out = RequestLoggingInterceptor.redactQueryString(input);
        assertEquals("flag&token=REDACTED", out);
    }
}
