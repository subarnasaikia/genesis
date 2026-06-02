-- Denormalize document_id onto wsd_annotation so the document-level reads and
-- the per-annotator/consensus exports no longer JOIN com.genesis.importexport's
-- TokenEntity from inside genesis-wsd (cross-module data coupling that ArchUnit
-- cannot see because the references live in JPQL string literals).
--
-- Three steps so existing rows survive: add nullable, backfill from tokens,
-- then enforce NOT NULL + add the FK and the composite index Hibernate
-- declares on the entity.

ALTER TABLE wsd_annotation ADD COLUMN document_id uuid;

UPDATE wsd_annotation a
   SET document_id = t.document_id
  FROM tokens t
 WHERE t.id = a.token_id
   AND a.document_id IS NULL;

-- Any annotation whose token row vanished cannot be recovered; surface it as a
-- migration failure rather than carry a NULL forward, matching the V5 stance on
-- orphans in annotation tables (those columns hold structural ids; NULLs there
-- signal corruption a human should look at).
ALTER TABLE wsd_annotation ALTER COLUMN document_id SET NOT NULL;

ALTER TABLE wsd_annotation
    ADD CONSTRAINT fk_wsd_document FOREIGN KEY (document_id)
        REFERENCES documents(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_wsd_ann_workspace_document
    ON wsd_annotation (workspace_id, document_id);
