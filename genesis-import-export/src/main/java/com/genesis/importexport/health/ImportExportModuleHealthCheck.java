package com.genesis.importexport.health;

import com.genesis.common.health.ModuleHealthCheck;
import org.springframework.stereotype.Component;

/**
 * Health check implementation for the Import-Export module.
 * 
 * <p>
 * <strong>What it does:</strong>
 * </p>
 * This class provides a mechanism to verify the operational status of the
 * Import-Export module.
 * It identifies itself as "Import-Export Module" and reports whether it is
 * healthy or not.
 * 
 * <p>
 * <strong>How it does it:</strong>
 * </p>
 * It implements the {@link ModuleHealthCheck} interface.
 * Currently, it returns a hardcoded {@code true} to simulate a healthy state.
 * In a real application, this would verify that IO subsystems or external
 * integration points
 * are responsive.
 * 
 * <p>
 * <strong>Why we are doing it:</strong>
 * </p>
 * To facilitate a centralized health monitoring system (via the Health API).
 * By having each module self-report, we decouple the central monitoring logic
 * from
 * module-specific implementation details.
 */
@Component
public class ImportExportModuleHealthCheck implements ModuleHealthCheck {

    @Override
    public String getModuleName() {
        return "Import-Export Module";
    }

    @Override
    public boolean isHealthy() {
        // Simulation: Always healthy.
        // Future: Check IO subsystems.
        return true;
    }
}
