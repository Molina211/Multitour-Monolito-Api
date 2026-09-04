CREATE TABLE reservation_executions (
    execution_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(reservation_id),
    served BOOLEAN NOT NULL,
    executed INTEGER NOT NULL,
    causal VARCHAR(500),
    actor_id VARCHAR(255),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE operation_costs (
    cost_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    reservation_id UUID NOT NULL REFERENCES reservations(reservation_id),
    concept VARCHAR(255) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    actor_id VARCHAR(255),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_operation_costs_reservation ON operation_costs(reservation_id);
