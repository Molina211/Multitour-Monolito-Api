ALTER TABLE reservations
    ADD COLUMN modification_reason VARCHAR(500),
    ADD COLUMN modified_by VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMPTZ;
