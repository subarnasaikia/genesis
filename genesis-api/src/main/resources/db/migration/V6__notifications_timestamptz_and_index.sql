-- V6__notifications_timestamptz_and_index.sql
-- Closes SYSTEM_DESIGN_AUDIT F-DB-02 (composite index for unread-notifications
-- queries + sorted listing) and F-DB-04 (created_at type alignment with the
-- rest of the audit columns).

ALTER TABLE notifications
  ALTER COLUMN created_at TYPE timestamp(6) with time zone
  USING created_at AT TIME ZONE 'UTC';

CREATE INDEX idx_notifications_recipient_read_created
  ON notifications (recipient_id, read, created_at DESC);
