package com.genesis.common.audit;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration to enable JPA auditing for automatic population of audit
 * fields.
 *
 * <p>
 * Enables {@code @CreatedDate}, {@code @LastModifiedDate}, {@code @CreatedBy},
 * and {@code @LastModifiedBy} annotations on entity fields.
 *
 * <p>
 * This configuration is only active when JPA EntityManagerFactory is available.
 */
@Configuration
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
