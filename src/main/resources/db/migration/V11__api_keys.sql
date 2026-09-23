CREATE TABLE api_keys (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id        BIGINT NOT NULL REFERENCES organizations(id),
    name          VARCHAR(120) NOT NULL,
    key_prefix    VARCHAR(16) NOT NULL,
    hashed_secret CHAR(64) NOT NULL UNIQUE,
    scopes        VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_org_id ON api_keys (org_id);
