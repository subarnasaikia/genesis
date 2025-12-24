package com.genesis.editor;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module configuration for genesis-editor.
 * Enables component scanning for all editor module components.
 */
@Configuration
@ComponentScan(basePackages = "com.genesis.editor")
public class EditorModuleConfig {
}
