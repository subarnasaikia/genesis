package com.genesis.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.CommonModuleConfig;
import com.genesis.infra.security.SecurityProperties;
import com.genesis.infra.storage.CloudinaryProperties;
import com.genesis.user.UserModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.infra")
@Import({ CommonModuleConfig.class, UserModuleConfig.class })
@EnableConfigurationProperties({ SecurityProperties.class, CloudinaryProperties.class })
public class InfraModuleConfig {
    // Infra module specific beans and configurations
}
