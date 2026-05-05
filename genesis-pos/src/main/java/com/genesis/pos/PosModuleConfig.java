package com.genesis.pos;

import com.genesis.common.CommonModuleConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.genesis.pos")
@Import(CommonModuleConfig.class)
public class PosModuleConfig {
    // POS module specific beans and configurations
}
