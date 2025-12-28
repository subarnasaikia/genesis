package com.genesis.notification;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module configuration for genesis-notification.
 * Enables component scanning for all notification module components.
 */
@Configuration
@ComponentScan(basePackages = "com.genesis.notification")
public class NotificationModuleConfig {
}
