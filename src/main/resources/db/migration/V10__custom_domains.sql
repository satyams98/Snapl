CREATE TABLE custom_domains (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id              BIGINT NOT NULL REFERENCES organizations(id),
    domain              VARCHAR(255) NOT NULL UNIQUE,
    verification_token  VARCHAR(100) NOT NULL,
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_custom_domains_org_id ON custom_domains (org_id);
