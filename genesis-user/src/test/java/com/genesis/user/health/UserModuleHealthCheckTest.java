package com.genesis.user.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserModuleHealthCheckTest {

    @Test
    void shouldReportCorrectModuleName() {
        UserModuleHealthCheck healthCheck = new UserModuleHealthCheck();
        assertEquals("User Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() {
        UserModuleHealthCheck healthCheck = new UserModuleHealthCheck();
        assertTrue(healthCheck.isHealthy());
    }
}
