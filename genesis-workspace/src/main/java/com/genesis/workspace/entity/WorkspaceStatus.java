package com.genesis.workspace.entity;

/**
 * Workspace lifecycle status.
 */
public enum WorkspaceStatus {
    /**
     * Workspace is in draft mode, not yet active.
     */
    DRAFT,

    /**
     * Workspace is active and accepting annotations.
     */
    ACTIVE,

    /**
     * Workspace is archived and read-only.
     */
    ARCHIVED
}
