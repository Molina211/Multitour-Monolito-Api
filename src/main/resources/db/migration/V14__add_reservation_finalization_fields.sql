ALTER TABLE reservations
    ADD COLUMN finalized_by VARCHAR(255),
    ADD COLUMN finalized_at TIMESTAMP;
