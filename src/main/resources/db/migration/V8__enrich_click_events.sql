-- device_type/browser/os are parsed from the User-Agent header at ingest time.
-- country/city are reserved for a future GeoIP lookup and are left null until a data source is wired in.
ALTER TABLE click_events ADD COLUMN device_type VARCHAR(20);
ALTER TABLE click_events ADD COLUMN browser VARCHAR(40);
ALTER TABLE click_events ADD COLUMN os VARCHAR(40);
ALTER TABLE click_events ADD COLUMN country VARCHAR(2);
ALTER TABLE click_events ADD COLUMN city VARCHAR(100);

CREATE INDEX idx_click_events_short_code_device_type ON click_events (short_code, device_type);
