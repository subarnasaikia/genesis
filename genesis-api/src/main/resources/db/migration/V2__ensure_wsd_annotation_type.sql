-- Re-applies the workspaces.annotation_type CHECK so it includes WSD.
--
-- V1__baseline.sql captures the schema after the WSD enum was added, but
-- environments that were baselined from a pre-WSD schema (e.g. a prod DB
-- that never got the manual db/migrations/2026-05-14__workspaces_allow_wsd.sql
-- applied) still carry the old (COREF,NER,POS) constraint. This migration
-- is idempotent: dropping with IF EXISTS, then recreating, leaves every
-- environment in the same correct end state.

ALTER TABLE workspaces
    DROP CONSTRAINT IF EXISTS workspaces_annotation_type_check;

ALTER TABLE workspaces
    ADD CONSTRAINT workspaces_annotation_type_check
    CHECK (annotation_type IN ('COREF', 'NER', 'POS', 'WSD'));
