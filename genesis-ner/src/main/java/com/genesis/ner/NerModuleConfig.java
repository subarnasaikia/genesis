package com.genesis.ner;

import com.genesis.common.CommonModuleConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.genesis.ner")
@Import(CommonModuleConfig.class)
public class NerModuleConfig {
    // NER module specific beans and configurations
}
