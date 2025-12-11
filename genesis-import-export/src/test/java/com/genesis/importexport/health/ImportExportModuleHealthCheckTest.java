package com.genesis.importexport.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportExportModuleHealthCheckTest {

    @Test
    void shouldReportCorrectModuleName() {
        ImportExportModuleHealthCheck healthCheck = new ImportExportModuleHealthCheck();
        assertEquals("Import-Export Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() {
        ImportExportModuleHealthCheck healthCheck = new ImportExportModuleHealthCheck();
        assertTrue(healthCheck.isHealthy());
    }
}
