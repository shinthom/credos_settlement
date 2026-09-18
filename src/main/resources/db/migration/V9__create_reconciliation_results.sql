CREATE TABLE reconciliation_results (
    id BIGSERIAL PRIMARY KEY,
    transfer_key UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    transfer_amount NUMERIC(19, 2) NOT NULL,
    debit_amount NUMERIC(19, 2) NOT NULL,
    credit_amount NUMERIC(19, 2) NOT NULL,
    ledger_sum NUMERIC(19, 2) NOT NULL,
    ledger_entry_count INTEGER NOT NULL,
    checked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_reconciliation_results_transfer FOREIGN KEY (transfer_key) REFERENCES transfers (transfer_key),
    CONSTRAINT uk_reconciliation_results_transfer_key UNIQUE (transfer_key)
);
