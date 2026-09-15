CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transfer_key UUID NOT NULL,
    account_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_ledger_entries_transfer FOREIGN KEY (transfer_key) REFERENCES transfers (transfer_key),
    CONSTRAINT chk_ledger_entry_amount_nonzero CHECK (amount <> 0)
);
