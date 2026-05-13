package com.genesis.wsd.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A word-sense definition for word-sense-disambiguation (WSD) annotation.
 *
 * <p>Senses are workspace-scoped (each workspace defines its own sense
 * inventory) and admin-managed. Annotators pick a sense for each token via
 * {@code WsdAnnotationEntity}.
 */
@Entity
@Table(name = "wsd_sense", indexes = {
        @Index(name = "idx_wsd_sense_workspace_word", columnList = "workspace_id, word")
})
public class WsdSenseEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "word", nullable = false, length = 200)
    private String word;

    @Column(name = "sense_label", nullable = false, length = 200)
    private String senseLabel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Override
    public UUID getId() {
        return super.getId();
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getSenseLabel() {
        return senseLabel;
    }

    public void setSenseLabel(String senseLabel) {
        this.senseLabel = senseLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
