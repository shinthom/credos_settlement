CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_key UUID NOT NULL UNIQUE,
    transfer_key UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_outbox_events_transfer FOREIGN KEY (transfer_key) REFERENCES transfers (transfer_key)
);
