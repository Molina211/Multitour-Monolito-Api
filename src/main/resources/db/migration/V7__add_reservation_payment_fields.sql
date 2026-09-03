ALTER TABLE reservations
    ADD COLUMN pending_transfer_amount NUMERIC(14, 2),
    ADD COLUMN transfer_support_reference VARCHAR(255);
