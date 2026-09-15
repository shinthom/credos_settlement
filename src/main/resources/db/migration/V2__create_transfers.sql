CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    transfer_key UUID NOT NULL UNIQUE,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transfers_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_transfers_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_transfer_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_transfer_accounts_different
        CHECK (from_account_id <> to_account_id)
);

