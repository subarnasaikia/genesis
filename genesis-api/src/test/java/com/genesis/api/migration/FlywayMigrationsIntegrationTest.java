package com.genesis.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the real Flyway migrations against a real PostgreSQL container, then lets
 * Hibernate validate the entities against the migrated schema.
 *
 * <p>This is the one place the migration chain is actually exercised. The normal
 * unit-test profile uses H2 + {@code ddl-auto=create-drop} with Flyway disabled
 * (see {@code src/test/resources/application.properties}), so a broken or
 * Postgres-specific migration — like the V5 FK constraint failure that aborted a
 * prod boot — would never surface in {@code mvn test}. This test closes that gap:
 *
 * <ul>
 *   <li>{@code spring.flyway.enabled=true} → all V1..Vn migrations run on Postgres 15,
 *       exactly as they do in prod. Any failing migration fails the test.</li>
 *   <li>{@code ddl-auto=validate} → after migrations, Hibernate asserts every entity
 *       maps to the migrated schema. Entity/schema drift fails the test.</li>
 * </ul>
 *
 * <p>{@code disabledWithoutDocker = true} makes the whole class skip cleanly on
 * machines/CI without a Docker daemon, so it never breaks a plain {@code mvn test}.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        // Point the context at the container and flip from the H2/create-drop test
        // defaults to the real prod-shaped setup: Postgres driver + dialect, Flyway on,
        // Hibernate in validate mode. DynamicPropertySource wins over the test
        // application.properties (which sets H2 + ddl-auto=create-drop + flyway off).
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // A fresh container is empty, so there is nothing to adopt — keep baseline-on-migrate
        // off (matching prod) so V1 runs as a normal migration rather than a baseline.
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    }

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("all migrations apply cleanly on Postgres and the schema validates")
    void allMigrationsApplyCleanlyOnPostgres() {
        // If the context loaded at all, Flyway already migrated and Hibernate already
        // validated the entities against the result (ddl-auto=validate). These assertions
        // make the success explicit and give a clear signal rather than a context-load error.
        var info = flyway.info();

        assertThat(info.applied())
                .as("at least one migration must have been applied")
                .isNotEmpty();

        assertThat(info.pending())
                .as("no migration may be left pending after startup")
                .isEmpty();

        assertThat(Arrays.stream(info.applied()).allMatch(m -> m.getState().isApplied()))
                .as("every applied migration must be in an applied (non-failed) state")
                .isTrue();
    }
}
