package com.genesis.logging;

import com.genesis.common.CommonModuleConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.genesis.logging")
@Import(CommonModuleConfig.class)
public class LoggingModuleConfig {
    // Logging module: annotation_log entity, audit listener, admin query service.
}
