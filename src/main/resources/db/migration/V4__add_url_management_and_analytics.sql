ALTER TABLE urls ADD COLUMN disabled_at TIMESTAMPTZ;

CREATE TABLE click_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code  VARCHAR(20) NOT NULL,
    clicked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    referrer    TEXT
);

CREATE INDEX idx_click_events_short_code ON click_events (short_code);
CREATE INDEX idx_click_events_clicked_at ON click_events (clicked_at);
