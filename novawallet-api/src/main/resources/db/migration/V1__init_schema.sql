CREATE TABLE IF NOT EXISTS schema_version_test (
                                                   id UUID PRIMARY KEY,
                                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);