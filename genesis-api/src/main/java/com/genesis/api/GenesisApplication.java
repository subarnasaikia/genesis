package com.genesis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.genesis.common.CommonModuleConfig;
import com.genesis.user.UserModuleConfig;
import com.genesis.workspace.WorkspaceModuleConfig;
import com.genesis.coref.CorefModuleConfig;
import com.genesis.importexport.ImportExportModuleConfig;
import com.genesis.infra.InfraModuleConfig;

import java.util.Optional;

@SpringBootApplication
@EntityScan(basePackages = {
                "com.genesis.common.entity",
                "com.genesis.user.entity",
                "com.genesis.infra.security"
})
@EnableJpaRepositories(basePackages = {
                "com.genesis.user.repository",
                "com.genesis.infra.security"
})
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Import({
                CommonModuleConfig.class,
                UserModuleConfig.class,
                WorkspaceModuleConfig.class,
                CorefModuleConfig.class,
                ImportExportModuleConfig.class,
                InfraModuleConfig.class
})
public class GenesisApplication {

        public static void main(String[] args) {
                SpringApplication.run(GenesisApplication.class, args);
        }

        @Bean
        public AuditorAware<String> auditorProvider() {
                return () -> {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                                return Optional.of("system");
                        }
                        return Optional.of(auth.getName());
                };
        }
}
