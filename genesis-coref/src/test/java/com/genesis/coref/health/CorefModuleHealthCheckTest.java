package com.genesis.coref.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorefModuleHealthCheckTest {

    @Test
    void shouldReportCorrectModuleName() {
        CorefModuleHealthCheck healthCheck = new CorefModuleHealthCheck();
        assertEquals("CoreF Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() {
        CorefModuleHealthCheck healthCheck = new CorefModuleHealthCheck();
        assertTrue(healthCheck.isHealthy());
    }
}
