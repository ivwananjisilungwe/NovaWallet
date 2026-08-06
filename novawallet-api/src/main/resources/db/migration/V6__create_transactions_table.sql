CREATE TABLE transactions (
    id UUID NOT NULL,
    reference VARCHAR(30) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_before DECIMAL(15, 2),
    balance_after DECIMAL(15, 2),
    status VARCHAR(15) NOT NULL DEFAULT 'SUCCESSFUL',
    description VARCHAR(255),
    sender_wallet_id UUID,
    receiver_wallet_id UUID,
    related_transaction_id UUID,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT uq_transactions_reference UNIQUE (reference),
    CONSTRAINT fk_transactions_sender FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transactions_receiver FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_transactions_sender_wallet ON transactions(sender_wallet_id);
CREATE INDEX idx_transactions_receiver_wallet ON transactions(receiver_wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
