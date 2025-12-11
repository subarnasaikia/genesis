package com.genesis.workspace.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceModuleHealthCheckTest {

    @Test
    void shouldReportCorrectModuleName() {
        WorkspaceModuleHealthCheck healthCheck = new WorkspaceModuleHealthCheck();
        assertEquals("Workspace Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() {
        WorkspaceModuleHealthCheck healthCheck = new WorkspaceModuleHealthCheck();
        assertTrue(healthCheck.isHealthy());
    }
}
