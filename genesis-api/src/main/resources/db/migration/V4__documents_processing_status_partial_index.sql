-- Partial index on documents.processing_status for in-flight rows.
--
-- The async tokenization pipeline (ImportService listening for
-- DocumentUploadedEvent / DocumentTokenizedEvent) filters the documents
-- table by processing_status looking for PENDING and PROCESSING rows.
-- Without an index Postgres falls back to a sequential scan, which
-- degrades as the corpus grows.
--
-- This is a *partial* index: it only covers rows whose status is in
-- the pre-terminal set (PENDING, PROCESSING). The vast majority of
-- rows eventually settle on COMPLETED (or FAILED), so a full-column
-- index would waste storage and bloat write paths. The partial form
-- stays small while still giving the planner an obvious path for the
-- in-flight polling query.
--
-- Refs SYSTEM_DESIGN_AUDIT F-DB-03.

CREATE INDEX idx_documents_processing_status_active
    ON documents (processing_status)
    WHERE processing_status IN ('PENDING', 'PROCESSING');
