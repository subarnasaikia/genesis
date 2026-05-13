package com.genesis.recommend;

import com.genesis.common.CommonModuleConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.genesis.recommend")
@Import(CommonModuleConfig.class)
public class RecommendModuleConfig {
    // Recommendation rules, dismissals, JWT share token endpoints.
}
