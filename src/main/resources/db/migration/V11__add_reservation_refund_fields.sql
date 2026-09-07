ALTER TABLE reservations
    ADD COLUMN refunded_amount NUMERIC(12,2),
    ADD COLUMN refund_reason VARCHAR(500),
    ADD COLUMN refunded_by VARCHAR(255),
    ADD COLUMN refund_method VARCHAR(100),
    ADD COLUMN refunded_at TIMESTAMP;
