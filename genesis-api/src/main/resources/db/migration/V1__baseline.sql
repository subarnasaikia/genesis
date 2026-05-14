--
-- PostgreSQL database dump
--


-- Dumped from database version 15.17
-- Dumped by pg_dump version 15.17

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: annotation_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.annotation_log (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    action_type character varying(30) NOT NULL,
    entity_id uuid,
    payload_json text,
    "timestamp" timestamp(6) with time zone NOT NULL,
    user_id character varying(100) NOT NULL,
    workspace_id uuid NOT NULL,
    CONSTRAINT annotation_log_action_type_check CHECK (((action_type)::text = ANY ((ARRAY['MENTION_CREATED'::character varying, 'MENTION_DELETED'::character varying, 'MENTION_ASSIGNED'::character varying, 'CLUSTER_CREATED'::character varying, 'CLUSTER_MERGED'::character varying, 'POS_TAGGED'::character varying, 'WSD_ANNOTATED'::character varying])::text[])))
);


--
-- Name: coref_clusters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coref_clusters (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    cluster_number integer NOT NULL,
    color character varying(20),
    label character varying(500),
    mention_count integer,
    representative_text character varying(1000),
    workspace_id uuid NOT NULL
);


--
-- Name: coref_mentions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coref_mentions (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    cluster_id uuid,
    document_id uuid NOT NULL,
    end_token_index integer NOT NULL,
    global_end_index integer,
    global_start_index integer,
    mention_type character varying(50),
    sentence_index integer NOT NULL,
    start_token_index integer NOT NULL,
    text character varying(2000),
    workspace_id uuid NOT NULL
);


--
-- Name: dismissed_recommendations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dismissed_recommendations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    accepted boolean NOT NULL,
    accepted_at timestamp(6) with time zone,
    dismissed_at timestamp(6) with time zone NOT NULL,
    recommendation_hash character varying(64) NOT NULL,
    user_id uuid NOT NULL,
    workspace_id uuid
);


--
-- Name: documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documents (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    file_size bigint,
    name character varying(255) NOT NULL,
    order_index integer NOT NULL,
    processing_error character varying(1000),
    processing_status character varying(20),
    progress double precision,
    status character varying(20) NOT NULL,
    token_end_index integer,
    token_start_index integer,
    stored_file_id uuid,
    workspace_id uuid NOT NULL,
    CONSTRAINT documents_processing_status_check CHECK (((processing_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT documents_status_check CHECK (((status)::text = ANY ((ARRAY['UPLOADED'::character varying, 'IMPORTED'::character varying, 'ANNOTATING'::character varying, 'COMPLETE'::character varying])::text[])))
);


--
-- Name: editor_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.editor_sessions (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    last_accessed_at timestamp(6) with time zone NOT NULL,
    last_document_index integer,
    scroll_position integer,
    user_id uuid NOT NULL,
    workspace_id uuid NOT NULL
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid NOT NULL,
    actor_id uuid,
    created_at timestamp(6) without time zone NOT NULL,
    link character varying(255),
    message character varying(255) NOT NULL,
    read boolean NOT NULL,
    recipient_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    type character varying(255),
    workspace_id uuid,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['INFO'::character varying, 'SUCCESS'::character varying, 'WARNING'::character varying, 'ERROR'::character varying])::text[])))
);


--
-- Name: pos_annotations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pos_annotations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    annotator_id character varying(100) NOT NULL,
    document_id uuid NOT NULL,
    pos_tag character varying(20) NOT NULL,
    "timestamp" timestamp(6) with time zone NOT NULL,
    token_id uuid NOT NULL
);


--
-- Name: pos_tag_definitions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pos_tag_definitions (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    created_by_user_id character varying(100) NOT NULL,
    description character varying(200),
    scope character varying(20) NOT NULL,
    tag character varying(20) NOT NULL,
    workspace_id uuid,
    CONSTRAINT pos_tag_definitions_scope_check CHECK (((scope)::text = ANY ((ARRAY['GLOBAL'::character varying, 'WORKSPACE'::character varying])::text[])))
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    expiry_date timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL,
    token character varying(500) NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: sentences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sentences (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    document_id uuid NOT NULL,
    end_offset integer NOT NULL,
    sentence_index integer NOT NULL,
    start_offset integer NOT NULL,
    text text,
    token_count integer
);


--
-- Name: stored_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stored_files (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    content_type character varying(255),
    file_size bigint,
    folder character varying(255),
    format character varying(255),
    original_filename character varying(255) NOT NULL,
    public_id character varying(255) NOT NULL,
    resource_type character varying(255),
    url character varying(1024) NOT NULL
);


--
-- Name: tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tokens (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    document_id uuid NOT NULL,
    end_offset integer NOT NULL,
    form character varying(500) NOT NULL,
    global_index integer NOT NULL,
    lemma character varying(500),
    ner_tag character varying(100),
    pos character varying(20),
    sentence_index integer NOT NULL,
    start_offset integer NOT NULL,
    token_index integer NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    account_locked boolean NOT NULL,
    auth_provider character varying(20) NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean NOT NULL,
    enabled boolean NOT NULL,
    first_name character varying(100) NOT NULL,
    last_login_at timestamp(6) with time zone,
    last_name character varying(100) NOT NULL,
    organization_name character varying(255),
    password character varying(255) NOT NULL,
    provider_id character varying(255),
    username character varying(50) NOT NULL,
    CONSTRAINT users_auth_provider_check CHECK (((auth_provider)::text = ANY ((ARRAY['LOCAL'::character varying, 'GOOGLE'::character varying])::text[])))
);


--
-- Name: workspace_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workspace_members (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    role character varying(20) NOT NULL,
    user_id uuid NOT NULL,
    workspace_id uuid NOT NULL,
    CONSTRAINT workspace_members_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'ANNOTATOR'::character varying, 'CURATOR'::character varying])::text[])))
);


--
-- Name: workspaces; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workspaces (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    annotation_type character varying(20) NOT NULL,
    description text,
    name character varying(100) NOT NULL,
    status character varying(20) NOT NULL,
    owner_id uuid NOT NULL,
    CONSTRAINT workspaces_annotation_type_check CHECK (((annotation_type)::text = ANY ((ARRAY['COREF'::character varying, 'NER'::character varying, 'POS'::character varying, 'WSD'::character varying])::text[]))),
    CONSTRAINT workspaces_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: wsd_annotation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wsd_annotation (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    annotator_id character varying(100) NOT NULL,
    sense_id uuid NOT NULL,
    "timestamp" timestamp(6) with time zone NOT NULL,
    token_id uuid NOT NULL,
    workspace_id uuid NOT NULL
);


--
-- Name: wsd_sense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wsd_sense (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(255) NOT NULL,
    version bigint,
    description text,
    sense_label character varying(200) NOT NULL,
    word character varying(200) NOT NULL,
    workspace_id uuid NOT NULL
);


--
-- Name: annotation_log annotation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annotation_log
    ADD CONSTRAINT annotation_log_pkey PRIMARY KEY (id);


--
-- Name: coref_clusters coref_clusters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coref_clusters
    ADD CONSTRAINT coref_clusters_pkey PRIMARY KEY (id);


--
-- Name: coref_mentions coref_mentions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coref_mentions
    ADD CONSTRAINT coref_mentions_pkey PRIMARY KEY (id);


--
-- Name: dismissed_recommendations dismissed_recommendations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dismissed_recommendations
    ADD CONSTRAINT dismissed_recommendations_pkey PRIMARY KEY (id);


--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- Name: editor_sessions editor_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.editor_sessions
    ADD CONSTRAINT editor_sessions_pkey PRIMARY KEY (id);


--
-- Name: coref_clusters idx_cluster_workspace_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coref_clusters
    ADD CONSTRAINT idx_cluster_workspace_number UNIQUE (workspace_id, cluster_number);


--
-- Name: dismissed_recommendations idx_dismissed_user_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dismissed_recommendations
    ADD CONSTRAINT idx_dismissed_user_hash UNIQUE (user_id, recommendation_hash);


--
-- Name: editor_sessions idx_editor_sessions_workspace_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.editor_sessions
    ADD CONSTRAINT idx_editor_sessions_workspace_user UNIQUE (workspace_id, user_id);


--
-- Name: pos_tag_definitions idx_pos_tag_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_tag_definitions
    ADD CONSTRAINT idx_pos_tag_unique UNIQUE (tag, scope, workspace_id);


--
-- Name: pos_annotations idx_pos_token_annotator; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_annotations
    ADD CONSTRAINT idx_pos_token_annotator UNIQUE (token_id, annotator_id);


--
-- Name: wsd_annotation idx_wsd_ann_token_annotator; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wsd_annotation
    ADD CONSTRAINT idx_wsd_ann_token_annotator UNIQUE (token_id, annotator_id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: pos_annotations pos_annotations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_annotations
    ADD CONSTRAINT pos_annotations_pkey PRIMARY KEY (id);


--
-- Name: pos_tag_definitions pos_tag_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_tag_definitions
    ADD CONSTRAINT pos_tag_definitions_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: sentences sentences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sentences
    ADD CONSTRAINT sentences_pkey PRIMARY KEY (id);


--
-- Name: stored_files stored_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stored_files
    ADD CONSTRAINT stored_files_pkey PRIMARY KEY (id);


--
-- Name: tokens tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tokens
    ADD CONSTRAINT tokens_pkey PRIMARY KEY (id);


--
-- Name: documents uk3rwx6vb7lrr50dd93wemwfqm7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT uk3rwx6vb7lrr50dd93wemwfqm7 UNIQUE (stored_file_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: workspace_members uk_workspace_members_workspace_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id);


--
-- Name: refresh_tokens ukghpmfn23vmxfu3spu3lfg4r2d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT ukghpmfn23vmxfu3spu3lfg4r2d UNIQUE (token);


--
-- Name: stored_files ukj6q2r19h9ywnu08womnhs6f4y; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stored_files
    ADD CONSTRAINT ukj6q2r19h9ywnu08womnhs6f4y UNIQUE (public_id);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: workspace_members workspace_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_pkey PRIMARY KEY (id);


--
-- Name: workspaces workspaces_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT workspaces_pkey PRIMARY KEY (id);


--
-- Name: wsd_annotation wsd_annotation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wsd_annotation
    ADD CONSTRAINT wsd_annotation_pkey PRIMARY KEY (id);


--
-- Name: wsd_sense wsd_sense_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wsd_sense
    ADD CONSTRAINT wsd_sense_pkey PRIMARY KEY (id);


--
-- Name: idx_cluster_workspace; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cluster_workspace ON public.coref_clusters USING btree (workspace_id);


--
-- Name: idx_dismissed_workspace; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dismissed_workspace ON public.dismissed_recommendations USING btree (workspace_id);


--
-- Name: idx_documents_order_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_order_index ON public.documents USING btree (workspace_id, order_index);


--
-- Name: idx_documents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_status ON public.documents USING btree (status);


--
-- Name: idx_documents_workspace_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_workspace_id ON public.documents USING btree (workspace_id);


--
-- Name: idx_editor_sessions_last_accessed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_editor_sessions_last_accessed ON public.editor_sessions USING btree (last_accessed_at);


--
-- Name: idx_log_action_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_log_action_type ON public.annotation_log USING btree (action_type);


--
-- Name: idx_log_workspace_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_log_workspace_timestamp ON public.annotation_log USING btree (workspace_id, "timestamp");


--
-- Name: idx_log_workspace_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_log_workspace_user ON public.annotation_log USING btree (workspace_id, user_id);


--
-- Name: idx_mention_cluster; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mention_cluster ON public.coref_mentions USING btree (cluster_id);


--
-- Name: idx_mention_doc_sentence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mention_doc_sentence ON public.coref_mentions USING btree (document_id, sentence_index);


--
-- Name: idx_mention_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mention_document ON public.coref_mentions USING btree (document_id);


--
-- Name: idx_mention_workspace; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mention_workspace ON public.coref_mentions USING btree (workspace_id);


--
-- Name: idx_pos_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_document ON public.pos_annotations USING btree (document_id);


--
-- Name: idx_pos_tag_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_tag_scope ON public.pos_tag_definitions USING btree (scope);


--
-- Name: idx_pos_tag_workspace; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_tag_workspace ON public.pos_tag_definitions USING btree (workspace_id);


--
-- Name: idx_pos_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_token ON public.pos_annotations USING btree (token_id);


--
-- Name: idx_refresh_tokens_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_token ON public.refresh_tokens USING btree (token);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_sentence_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sentence_document ON public.sentences USING btree (document_id);


--
-- Name: idx_sentence_document_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sentence_document_index ON public.sentences USING btree (document_id, sentence_index);


--
-- Name: idx_token_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_token_document ON public.tokens USING btree (document_id);


--
-- Name: idx_token_document_sentence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_token_document_sentence ON public.tokens USING btree (document_id, sentence_index);


--
-- Name: idx_token_global; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_token_global ON public.tokens USING btree (document_id, global_index);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_workspace_members_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workspace_members_role ON public.workspace_members USING btree (role);


--
-- Name: idx_workspace_members_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workspace_members_user_id ON public.workspace_members USING btree (user_id);


--
-- Name: idx_workspace_members_workspace_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workspace_members_workspace_id ON public.workspace_members USING btree (workspace_id);


--
-- Name: idx_workspaces_owner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workspaces_owner_id ON public.workspaces USING btree (owner_id);


--
-- Name: idx_workspaces_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workspaces_status ON public.workspaces USING btree (status);


--
-- Name: idx_wsd_ann_sense; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wsd_ann_sense ON public.wsd_annotation USING btree (sense_id);


--
-- Name: idx_wsd_ann_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wsd_ann_token ON public.wsd_annotation USING btree (token_id);


--
-- Name: idx_wsd_ann_workspace; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wsd_ann_workspace ON public.wsd_annotation USING btree (workspace_id);


--
-- Name: idx_wsd_sense_workspace_word; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wsd_sense_workspace_word ON public.wsd_sense USING btree (workspace_id, word);


--
-- Name: refresh_tokens fk1lih5y2npsf8u5o3vhdb9y0os; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: workspaces fk58ks96jjlsbhsh21cen7hr59h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT fk58ks96jjlsbhsh21cen7hr59h FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: workspace_members fk6vtnpc3eexk504u61uepn40p1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT fk6vtnpc3eexk504u61uepn40p1 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: documents fk8qahhj9hi2hsa6vnsf5x4mke; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk8qahhj9hi2hsa6vnsf5x4mke FOREIGN KEY (stored_file_id) REFERENCES public.stored_files(id);


--
-- Name: documents fkp8lrsv9kvf60o04juy6uj3c1j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fkp8lrsv9kvf60o04juy6uj3c1j FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id);


--
-- Name: workspace_members fkw9hq87n3rvq2c4j47qo78i5r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT fkw9hq87n3rvq2c4j47qo78i5r FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id);


--
-- PostgreSQL database dump complete
--


