package com.genesis.infra;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.infra")
@Import(CommonModuleConfig.class)
public class InfraModuleConfig {
    // Infra module specific beans and configurations
}
