package com.genesis.common.health;

/**
 * Interface for checking the health of a specific module.
 */
public interface ModuleHealthCheck {
    /**
     * @return The name of the module.
     */
    String getModuleName();

    /**
     * @return true if the module is healthy, false otherwise.
     */
    boolean isHealthy();
}
