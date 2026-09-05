ALTER TABLE reservations
    ADD COLUMN refund_decision_status VARCHAR(30),
    ADD COLUMN refund_authorized_by VARCHAR(255),
    ADD COLUMN refund_authorized_at TIMESTAMP,
    ADD COLUMN refund_authorization_note VARCHAR(500),
    ADD COLUMN refund_rejected_by VARCHAR(255),
    ADD COLUMN refund_rejected_at TIMESTAMP,
    ADD COLUMN refund_rejection_reason VARCHAR(500);
