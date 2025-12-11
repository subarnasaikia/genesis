package com.genesis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.genesis.common.CommonModuleConfig;
import com.genesis.user.UserModuleConfig;
import com.genesis.workspace.WorkspaceModuleConfig;
import com.genesis.coref.CorefModuleConfig;
import com.genesis.importexport.ImportExportModuleConfig;
import com.genesis.infra.InfraModuleConfig;

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