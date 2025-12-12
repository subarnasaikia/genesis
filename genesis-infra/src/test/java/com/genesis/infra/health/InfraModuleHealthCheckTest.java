package com.genesis.infra.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfraModuleHealthCheckTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Test
    void shouldReportCorrectModuleName() {
        InfraModuleHealthCheck healthCheck = new InfraModuleHealthCheck(dataSource);
        assertEquals("Infra Module", healthCheck.getModuleName());
    }

    @Test
    void shouldBeHealthy() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);

        InfraModuleHealthCheck healthCheck = new InfraModuleHealthCheck(dataSource);
        assertTrue(healthCheck.isHealthy());
    }
}
