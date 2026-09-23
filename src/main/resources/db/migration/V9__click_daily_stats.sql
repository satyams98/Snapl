-- Daily click rollup so time-series dashboard queries don't scan raw click_events on every request.
CREATE TABLE click_daily_stats (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code  VARCHAR(20) NOT NULL,
    day         DATE NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    UNIQUE (short_code, day)
);

CREATE INDEX idx_click_daily_stats_short_code_day ON click_daily_stats (short_code, day);
