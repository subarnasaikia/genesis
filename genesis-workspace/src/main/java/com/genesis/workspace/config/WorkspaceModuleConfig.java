package com.genesis.workspace.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.config.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.workspace")
@Import(CommonModuleConfig.class)
public class WorkspaceModuleConfig {
    // Workspace module specific beans and configurations
}
