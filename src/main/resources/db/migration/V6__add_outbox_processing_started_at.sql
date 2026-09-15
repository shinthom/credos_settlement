ALTER TABLE outbox_events
ADD COLUMN processing_started_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_events_processing_started_at ON outbox_events (status, processing_started_at);
