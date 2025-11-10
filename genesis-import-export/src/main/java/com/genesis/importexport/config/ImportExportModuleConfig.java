package com.genesis.importexport.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.genesis.common.config.CommonModuleConfig;

@Configuration
@ComponentScan(basePackages = "com.genesis.importexport")
@Import(CommonModuleConfig.class)
public class ImportExportModuleConfig {
    // Import-Export module specific beans and configurations
}
