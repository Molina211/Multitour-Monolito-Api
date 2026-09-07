ALTER TABLE reservations
    ADD COLUMN cancellation_reason VARCHAR(500),
    ADD COLUMN cancelled_by VARCHAR(255),
    ADD COLUMN cancelled_at TIMESTAMP;
