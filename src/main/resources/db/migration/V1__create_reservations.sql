CREATE TABLE reservations (
    reservation_id     UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    customer_id        VARCHAR(100) NOT NULL,
    projected_value    NUMERIC(12,2) NOT NULL,
    final_value        NUMERIC(12,2) NOT NULL,
    pending_balance    NUMERIC(12,2) NOT NULL,
    credit_balance     NUMERIC(12,2) NOT NULL DEFAULT 0,
    reservation_status VARCHAR(30) NOT NULL,
    payment_status     VARCHAR(30) NOT NULL,
    payment_method     VARCHAR(30),
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reserved_services (
    id                 BIGSERIAL PRIMARY KEY,
    reservation_id     UUID NOT NULL REFERENCES reservations(reservation_id),
    tenant_id          UUID NOT NULL,
    service_reference  VARCHAR(100) NOT NULL,
    party_size         INTEGER,
    scheduled_date     DATE
);

CREATE INDEX idx_reservations_tenant ON reservations(tenant_id);
CREATE INDEX idx_reserved_services_tenant ON reserved_services(tenant_id);
CREATE INDEX idx_reserved_services_reservation ON reserved_services(reservation_id);
