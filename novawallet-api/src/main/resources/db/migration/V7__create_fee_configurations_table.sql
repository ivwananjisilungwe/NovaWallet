CREATE TABLE fee_configurations (
    id UUID NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    percentage_fee DECIMAL(5, 4) NOT NULL DEFAULT 0,
    flat_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    min_fee DECIMAL(12, 2),
    max_fee DECIMAL(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fee_configurations PRIMARY KEY (id),
    CONSTRAINT uq_fee_config_transaction_type UNIQUE (transaction_type)
);

INSERT INTO fee_configurations (id, transaction_type, percentage_fee, flat_fee, min_fee, max_fee, active)
VALUES ('a0000000-0000-0000-0000-000000000001', 'TRANSFER', 0.0100, 0, 1.00, 50.00, true);

INSERT INTO fee_configurations (id, transaction_type, percentage_fee, flat_fee, min_fee, max_fee, active)
VALUES ('a0000000-0000-0000-0000-000000000002', 'WITHDRAWAL', 0.0050, 0, 0.50, 30.00, true);

INSERT INTO fee_configurations (id, transaction_type, percentage_fee, flat_fee, min_fee, max_fee, active)
VALUES ('a0000000-0000-0000-0000-000000000003', 'DEPOSIT', 0, 0, NULL, NULL, true);
