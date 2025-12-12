package com.genesis.infra.health;

import com.genesis.common.health.ModuleHealthCheck;
import org.springframework.stereotype.Component;

/**
 * Health check implementation for the Infra (Infrastructure) module.
 * 
 * <p>
 * <strong>What it does:</strong>
 * </p>
 * This class provides a mechanism to verify the operational status of the
 * Infrastructure module.
 * It identifies itself as "Infra Module" and reports whether it is healthy or
 * not.
 * 
 * <p>
 * <strong>How it does it:</strong>
 * </p>
 * It implements the {@link ModuleHealthCheck} interface.
 * Currently, it returns a hardcoded {@code true} to simulate a healthy state.
 * This should eventually check low-level infrastructure concerns like messaging
 * queues,
 * caches, or third-party client initializations.
 * 
 * <p>
 * <strong>Why we are doing it:</strong>
 * </p>
 * To facilitate a centralized health monitoring system (via the Health API).
 * By having each module self-report, we decouple the central monitoring logic
 * from
 * module-specific implementation details.
 */
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class InfraModuleHealthCheck implements ModuleHealthCheck {

    private final DataSource dataSource;

    public InfraModuleHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getModuleName() {
        return "Infra Module";
    }

    @Override
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }
}
