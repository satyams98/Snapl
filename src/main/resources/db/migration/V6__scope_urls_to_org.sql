ALTER TABLE urls ADD COLUMN org_id BIGINT REFERENCES organizations(id);

CREATE INDEX idx_urls_org_id_created_at ON urls (org_id, created_at);
