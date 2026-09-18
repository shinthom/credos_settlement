CREATE INDEX idx_ledger_entries_transfer_key ON ledger_entries (transfer_key);

CREATE INDEX idx_transfers_created_at_id ON transfers (created_at, id);
