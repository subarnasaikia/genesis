package com.genesis.infra.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraModuleHealthCheckTest {

    @Test
    void shouldReportCorrectModuleName() {
        InfraModuleHealthCheck healthCheck = new InfraModuleHealthCheck();
        assertEquals("Infra Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() {
        InfraModuleHealthCheck healthCheck = new InfraModuleHealthCheck();
        assertTrue(healthCheck.isHealthy());
    }
}
