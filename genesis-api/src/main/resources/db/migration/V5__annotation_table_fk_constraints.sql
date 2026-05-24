-- V5__annotation_table_fk_constraints.sql
-- Closes SYSTEM_DESIGN_AUDIT F-DB-01: every "logical FK" column on annotation
-- and notification tables gets a real foreign key constraint with the right
-- ON DELETE behaviour. Without these, deleting a document or workspace
-- silently leaves orphaned rows in tokens/sentences/mentions/POS/WSD/NER and
-- the audit log — the app-layer cleanup loops in DocumentService /
-- WorkspaceService are the only guard today.

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
