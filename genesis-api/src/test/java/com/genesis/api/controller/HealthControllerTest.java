package com.genesis.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genesis.common.health.ModuleHealthCheck;
import java.util.List;

/**
 * Unit test for HealthController using a standalone test configuration.
 * This avoids loading the full application context with JPA dependencies.
 */
@WebMvcTest(controllers = HealthController.class)
@ContextConfiguration(classes = { HealthControllerTest.TestConfig.class, HealthController.class })
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.checks").isMap())
                .andExpect(jsonPath("$.checks['User Module']").value(true))
                .andExpect(jsonPath("$.checks['Workspace Module']").value(true))
                .andExpect(jsonPath("$.checks['CoreF Module']").value(true))
                .andExpect(jsonPath("$.checks['Import-Export Module']").value(true))
                .andExpect(jsonPath("$.checks['Infra Module']").value(true));
    }

    /**
     * Standalone test configuration that provides mock health checks
     * and security configuration without loading the full application context.
     */
    @Configuration
    static class TestConfig {

        @Bean
        public List<ModuleHealthCheck> healthChecks() {
            return List.of(
                    createMockHealthCheck("User Module", true),
                    createMockHealthCheck("Workspace Module", true),
                    createMockHealthCheck("CoreF Module", true),
                    createMockHealthCheck("Import-Export Module", true),
                    createMockHealthCheck("Infra Module", true));
        }

        private ModuleHealthCheck createMockHealthCheck(String name, boolean healthy) {
            return new ModuleHealthCheck() {
                @Override
                public String getModuleName() {
                    return name;
                }

                @Override
                public boolean isHealthy() {
                    return healthy;
                }
            };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
}
