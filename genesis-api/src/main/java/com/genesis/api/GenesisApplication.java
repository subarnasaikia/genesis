package com.genesis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.genesis.common.config.CommonModuleConfig;
import com.genesis.user.config.UserModuleConfig;
import com.genesis.workspace.config.WorkspaceModuleConfig;
import com.genesis.coref.config.CorefModuleConfig;
import com.genesis.importexport.config.ImportExportModuleConfig;
import com.genesis.infra.config.InfraModuleConfig;

@SpringBootApplication
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
}