package com.genesis.workspace.config;

import com.genesis.common.audit.AuditorAwareImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test configuration for workspace repository tests.
 * Enables JPA auditing and scans necessary entity/repository packages.
 */
@Configuration
@EnableAutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan(basePackages = {
        "com.genesis.workspace.entity",
        "com.genesis.user.entity",
        "com.genesis.infra.storage"
})
@EnableJpaRepositories(basePackages = {
        "com.genesis.workspace.repository",
        "com.genesis.user.repository"
})
public class WorkspaceTestConfiguration {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
