-- NER module schema: tag registry + span annotations.
--
-- ner_tag_definitions mirrors pos_tag_definitions (workspace/global custom
-- tags layered on top of an in-code OntoNotes 18 set held by
-- NerTagDefinitionService.UNIVERSAL_NER_TAGS).
--
-- ner_annotations stores one row per (annotator, span). Spans may nest and
-- overlap freely; the only span-shape constraint is end >= start, enforced
-- by ner_annotations_end_ge_start_check.

CREATE TABLE IF NOT EXISTS ner_tag_definitions (
    id                   uuid                          NOT NULL,
    created_at           timestamp(6) with time zone   NOT NULL,
    created_by           character varying(255)        NOT NULL,
    updated_at           timestamp(6) with time zone   NOT NULL,
    updated_by           character varying(255)        NOT NULL,
    version              bigint,
    created_by_user_id   character varying(100)        NOT NULL,
    description          character varying(200),
    scope                character varying(20)         NOT NULL,
    tag                  character varying(20)         NOT NULL,
    workspace_id         uuid,
    CONSTRAINT ner_tag_definitions_pkey PRIMARY KEY (id),
    CONSTRAINT ner_tag_definitions_scope_check
        CHECK ((scope)::text = ANY ((ARRAY['GLOBAL'::character varying,
                                           'WORKSPACE'::character varying])::text[])),
    CONSTRAINT idx_ner_tag_unique UNIQUE (tag, scope, workspace_id)
);

CREATE INDEX IF NOT EXISTS idx_ner_tag_workspace
    ON ner_tag_definitions (workspace_id);

CREATE INDEX IF NOT EXISTS idx_ner_tag_scope
    ON ner_tag_definitions (scope);

CREATE TABLE IF NOT EXISTS ner_annotations (
    id                   uuid                          NOT NULL,
    created_at           timestamp(6) with time zone   NOT NULL,
    created_by           character varying(255)        NOT NULL,
    updated_at           timestamp(6) with time zone   NOT NULL,
    updated_by           character varying(255)        NOT NULL,
    version              bigint,
    annotator_id         character varying(100)        NOT NULL,
    document_id          uuid                          NOT NULL,
    start_token_index    integer                       NOT NULL,
    end_token_index      integer                       NOT NULL,
    label                character varying(20)         NOT NULL,
    "timestamp"          timestamp(6) with time zone   NOT NULL,
    CONSTRAINT ner_annotations_pkey PRIMARY KEY (id),
    CONSTRAINT ner_annotations_end_ge_start_check
        CHECK (end_token_index >= start_token_index),
    CONSTRAINT ner_annotations_start_nonneg_check
        CHECK (start_token_index >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ner_ann_document
    ON ner_annotations (document_id);

CREATE INDEX IF NOT EXISTS idx_ner_ann_doc_annotator
    ON ner_annotations (document_id, annotator_id);
