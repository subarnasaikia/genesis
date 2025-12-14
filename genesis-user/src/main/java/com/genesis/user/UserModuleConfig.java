package com.genesis.user;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.user")
@Import(CommonModuleConfig.class)
public class UserModuleConfig {
    // User module specific beans and configurations
}
