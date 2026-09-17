ALTER TABLE urls ADD COLUMN long_url_hash CHAR(64) NOT NULL DEFAULT '';
ALTER TABLE urls ALTER COLUMN long_url_hash DROP DEFAULT;

CREATE INDEX idx_urls_long_url_hash ON urls (long_url_hash);
