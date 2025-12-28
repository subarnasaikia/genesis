package com.genesis.importexport.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

public class ExportGeneratedEvent extends ApplicationEvent {
    private final UUID userId;
    private final String fileName;
    private final String downloadUrl; // Or maybe just a message saying it's ready if download happens immediately?
    // Usually exports are async. If so, we need a way to download it.
    // For now let's assume we just notify the user "Export X is ready".

    public ExportGeneratedEvent(Object source, UUID userId, String fileName, String downloadUrl) {
        super(source);
        this.userId = userId;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
