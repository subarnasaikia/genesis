package com.genesis.infra.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

import com.genesis.infra.config.InfraConfiguration;

@SpringBootTest(classes = InfraConfiguration.class)
public class DataSourceTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void dataSourceShouldBeAvailable() {
        assertThat(context.getBean(DataSource.class)).isNotNull();
    }

    @Test
    void shouldConnectToDatabase() throws SQLException {
        DataSource dataSource = context.getBean(DataSource.class);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
    }
}
