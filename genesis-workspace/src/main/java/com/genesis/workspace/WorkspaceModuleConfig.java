package com.genesis.workspace;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.workspace")
@Import(CommonModuleConfig.class)
public class WorkspaceModuleConfig {
    // Workspace module specific beans and configurations
}
