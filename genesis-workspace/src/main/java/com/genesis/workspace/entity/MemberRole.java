package com.genesis.workspace.entity;

/**
 * Roles for workspace members.
 */
public enum MemberRole {
    /**
     * Administrator with full access to workspace settings.
     */
    ADMIN,

    /**
     * Annotator who can create and edit annotations.
     */
    ANNOTATOR,

    /**
     * Curator who can review and approve annotations.
     */
    CURATOR
}
