package com.genesis.workspace.health;

import com.genesis.common.health.ModuleHealthCheck;
import org.springframework.stereotype.Component;

/**
 * Health check implementation for the Workspace module.
 * 
 * <p>
 * <strong>What it does:</strong>
 * </p>
 * This class provides a mechanism to verify the operational status of the
 * Workspace module.
 * It identifies itself as "Workspace Module" and reports whether it is healthy
 * or not.
 * 
 * <p>
 * <strong>How it does it:</strong>
 * </p>
 * It implements the {@link ModuleHealthCheck} interface.
 * Currently, it returns a hardcoded {@code true} to simulate a healthy state.
 * In a production environment, this method would be expanded to check critical
 * dependencies
 * such as database connections specific to workspaces or file system
 * accessibility.
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
public class WorkspaceModuleHealthCheck implements ModuleHealthCheck {

    @Override
    public String getModuleName() {
        return "Workspace Module";
    }

    @Override
    public boolean isHealthy() {
        // Simulation: Always healthy.
        // Future: Check database or specific workspace service availability.
        return true;
    }
}
