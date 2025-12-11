package com.genesis.user.health;

import com.genesis.common.health.ModuleHealthCheck;
import org.springframework.stereotype.Component;

/**
 * Health check implementation for the User module.
 * 
 * <p>
 * <strong>What it does:</strong>
 * </p>
 * This class provides a mechanism to verify the operational status of the User
 * module.
 * It identifies itself as "User Module" and reports whether it is healthy or
 * not.
 * 
 * <p>
 * <strong>How it does it:</strong>
 * </p>
 * It implements the {@link ModuleHealthCheck} interface.
 * Currently, it returns a hardcoded {@code true} to simulate a healthy state.
 * Real-world implementations would perform checks against the user database or
 * auth services.
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
public class UserModuleHealthCheck implements ModuleHealthCheck {

    @Override
    public String getModuleName() {
        return "User Module";
    }

    @Override
    public boolean isHealthy() {
        // In a real scenario, this might check DB connectivity or other dependencies
        return true;
    }
}
