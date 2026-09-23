-- starts_at enables scheduled (not-yet-active) links; password_hash enables optional password protection.
ALTER TABLE urls ADD COLUMN starts_at TIMESTAMPTZ;
ALTER TABLE urls ADD COLUMN password_hash VARCHAR(100);
