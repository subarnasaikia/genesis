package com.genesis.infra.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DataSourceTest.TestConfig.class)
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

    /**
     * Minimal test configuration that includes both genesis-infra and genesis-user
     * entity packages to satisfy JPA relationships between RefreshToken and User.
     */
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "com.genesis.infra", "com.genesis.user" })
    @EnableJpaRepositories(basePackages = { "com.genesis.infra", "com.genesis.user" })
    static class TestConfig {
    }
}
