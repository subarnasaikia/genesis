package com.genesis.wsd;

import com.genesis.common.CommonModuleConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.genesis.wsd")
@Import(CommonModuleConfig.class)
public class WsdModuleConfig {
    // WSD module: sense + annotation entities, services, controllers.
}
