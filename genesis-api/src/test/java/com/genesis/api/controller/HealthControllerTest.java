package com.genesis.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genesis.infra.health.InfraModuleHealthCheck;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.BDDMockito.given;

@WebMvcTest(HealthController.class)
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InfraModuleHealthCheck infraModuleHealthCheck;

    @Test
    public void shouldReturnHealthStatus() throws Exception {
        // Prepare Mock
        given(infraModuleHealthCheck.getModuleName()).willReturn("Infra Module");
        given(infraModuleHealthCheck.isHealthy()).willReturn(true);

        // Act & Assert
        // This validates that the HealthController is up and returning the expected
        // structure.
        // It relies on the Spring Context injecting available health checks from all
        // modules.
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
}
