ALTER TABLE reservations ADD COLUMN holder_document VARCHAR(50);

CREATE TABLE reservation_companions (
    id                 BIGSERIAL PRIMARY KEY,
    reservation_id     UUID NOT NULL REFERENCES reservations(reservation_id),
    tenant_id          VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    name               VARCHAR(200) NOT NULL,
    document           VARCHAR(50) NOT NULL,
    birth_date         DATE NOT NULL
);

CREATE INDEX idx_reservation_companions_tenant ON reservation_companions(tenant_id);
CREATE INDEX idx_reservation_companions_reservation ON reservation_companions(reservation_id);
