-- 2026-05-14: allow WSD as a workspace annotation type
--
-- Background: AnnotationType enum gained WSD in PR #18 (issue #14), but
-- Hibernate ddl-auto=update only adds new columns/tables, it does NOT
-- update existing CHECK constraints. The constraint generated when the
-- workspaces table was first created still only listed (COREF, NER, POS),
-- so INSERTs with annotation_type='WSD' fail with constraint violation
-- 23514 and the request returns 500.
--
-- Run this on every environment whose `workspaces` table predates the
-- WSD enum addition. Safe to re-run: DROP CONSTRAINT IF EXISTS handles
-- the case where it's already been applied or where the constraint name
-- differs across deployments.
--
-- TODO: adopt Flyway/Liquibase so this kind of schema drift is handled
-- automatically. Tracked separately. Until then, ops must apply this
-- SQL by hand when promoting backend builds that change AnnotationType
-- or any other @Enumerated-as-STRING column.

ALTER TABLE workspaces
    DROP CONSTRAINT IF EXISTS workspaces_annotation_type_check;

ALTER TABLE workspaces
    ADD CONSTRAINT workspaces_annotation_type_check
    CHECK (annotation_type IN ('COREF', 'NER', 'POS', 'WSD'));
