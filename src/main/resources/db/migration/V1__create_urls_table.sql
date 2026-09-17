CREATE TABLE urls (
                      id              BIGINT PRIMARY KEY,
                      short_code      VARCHAR(10) NOT NULL,
                      long_url        TEXT NOT NULL,
                      created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                      expires_at      TIMESTAMPTZ,
                      is_custom_slug  BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX idx_urls_short_code ON urls (short_code);