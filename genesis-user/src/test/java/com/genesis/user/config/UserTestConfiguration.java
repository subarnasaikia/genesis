package com.genesis.user.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import java.util.Optional;

/**
 * Test configuration for User module integration tests.
 * Uses H2 in-memory database for isolation.
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackages = { "com.genesis.user.entity", "com.genesis.common.entity" })
@EnableJpaRepositories(basePackages = "com.genesis.user.repository")
@EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
public class UserTestConfiguration {

    /**
     * Returns a test auditor for JPA auditing.
     */
    @Bean
    public AuditorAware<String> testAuditorAware() {
        return () -> Optional.of("test-user");
    }
}
