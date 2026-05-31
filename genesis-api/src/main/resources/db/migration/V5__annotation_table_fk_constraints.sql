-- V5__annotation_table_fk_constraints.sql
-- Closes SYSTEM_DESIGN_AUDIT F-DB-01: every "logical FK" column on annotation
-- and notification tables gets a real foreign key constraint with the right
-- ON DELETE behaviour. Without these, deleting a document or workspace
-- silently leaves orphaned rows in tokens/sentences/mentions/POS/WSD/NER and
-- the audit log — the app-layer cleanup loops in DocumentService /
-- WorkspaceService are the only guard today.

-- Pre-flight orphan cleanup: legacy rows can carry user/workspace ids that no
-- longer exist — notably editor_sessions written by the pre-MEDIUM-2
-- EditorController, which hashed the username into a synthetic UUID instead of
-- the real user id. Those orphans make the FK ADDs below fail and abort app
-- startup. We scrub them here, before the constraints, for the three user-facing
-- tables where orphan rows are non-critical (ephemeral editor session state,
-- recommendation dismissals, notifications). CASCADE-target orphans are deleted;
-- SET NULL-target orphans (workspace_id) are nulled to match the FK's intent.
--
-- Annotation tables (mentions/tokens/sentences/pos/wsd/ner/clusters/log) are
-- intentionally NOT auto-scrubbed: those columns always held real structural ids,
-- so an orphan there signals genuine corruption that should fail loudly for a
-- human rather than be silently deleted by a migration.
--
-- NOTE: this edits V5 in place. Safe only because V5 had not yet applied
-- successfully in any environment (it aborted at schema v4, rolled back with no
-- history row). If V5 ever applied elsewhere, this checksum change requires
-- `flyway repair` there.
DELETE FROM editor_sessions es
  WHERE NOT EXISTS (SELECT 1 FROM users u      WHERE u.id = es.user_id)
     OR NOT EXISTS (SELECT 1 FROM workspaces w WHERE w.id = es.workspace_id);

DELETE FROM dismissed_recommendations dr
  WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = dr.user_id);
UPDATE dismissed_recommendations dr
  SET workspace_id = NULL
  WHERE dr.workspace_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM workspaces w WHERE w.id = dr.workspace_id);

DELETE FROM notifications n
  WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = n.recipient_id);
UPDATE notifications n
  SET workspace_id = NULL
  WHERE n.workspace_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM workspaces w WHERE w.id = n.workspace_id);

ALTER TABLE coref_mentions
  ADD CONSTRAINT fk_mention_document  FOREIGN KEY (document_id)  REFERENCES documents(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_mention_cluster   FOREIGN KEY (cluster_id)   REFERENCES coref_clusters(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_mention_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE coref_clusters
  ADD CONSTRAINT fk_cluster_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE tokens    ADD CONSTRAINT fk_token_document    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
ALTER TABLE sentences ADD CONSTRAINT fk_sentence_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE pos_annotations
  ADD CONSTRAINT fk_pos_token    FOREIGN KEY (token_id)    REFERENCES tokens(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_pos_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE wsd_annotation
  ADD CONSTRAINT fk_wsd_token     FOREIGN KEY (token_id)     REFERENCES tokens(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_wsd_sense     FOREIGN KEY (sense_id)     REFERENCES wsd_sense(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_wsd_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE wsd_sense
  ADD CONSTRAINT fk_wsd_sense_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE ner_annotations
  ADD CONSTRAINT fk_ner_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE notifications
  ADD CONSTRAINT fk_notif_recipient  FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_notif_workspace  FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

ALTER TABLE editor_sessions
  ADD CONSTRAINT fk_editor_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_editor_user      FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE dismissed_recommendations
  ADD CONSTRAINT fk_dismissed_user      FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_dismissed_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

ALTER TABLE annotation_log
  ADD CONSTRAINT fk_log_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
