ALTER TABLE outbox_events
ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN next_attempt_at TIMESTAMPTZ,
ADD COLUMN last_error TEXT;

ALTER TABLE outbox_events
ADD CONSTRAINT chk_outbox_attempts_nonnegative CHECK (attempts >= 0);

CREATE INDEX idx_outbox_events_pending_next_attempt ON outbox_events (status, next_attempt_at, created_at);
