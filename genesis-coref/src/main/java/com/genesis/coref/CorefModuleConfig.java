package com.genesis.coref;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.coref")
@Import(CommonModuleConfig.class)
public class CorefModuleConfig {
    // Coref module specific beans and configurations
}
