package com.genesis.api.controller;

import com.genesis.common.health.ModuleHealthCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final List<ModuleHealthCheck> healthChecks;

    @Autowired
    public HealthController(List<ModuleHealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @GetMapping
    public Map<String, Object> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");

        Map<String, Boolean> checks = healthChecks.stream()
                .collect(Collectors.toMap(
                        ModuleHealthCheck::getModuleName,
                        ModuleHealthCheck::isHealthy));

        response.put("checks", checks);
        return response;
    }
}
