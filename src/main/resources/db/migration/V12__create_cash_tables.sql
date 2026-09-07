CREATE TABLE cash_registers (
    cash_register_id   UUID PRIMARY KEY,
    tenant_id           VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    business_date       DATE NOT NULL,
    base_amount         NUMERIC(12,2) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    closed_by            VARCHAR(255),
    closed_at            TIMESTAMPTZ,
    total_amount         NUMERIC(12,2),
    UNIQUE (tenant_id, business_date)
);

CREATE TABLE cash_movements (
    id                   BIGSERIAL PRIMARY KEY,
    cash_register_id    UUID NOT NULL REFERENCES cash_registers(cash_register_id),
    type                 VARCHAR(20) NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    concept              VARCHAR(255) NOT NULL,
    actor_id             VARCHAR(255) NOT NULL,
    recorded_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE cash_corrections (
    id                   BIGSERIAL PRIMARY KEY,
    cash_register_id    UUID NOT NULL REFERENCES cash_registers(cash_register_id),
    justification        VARCHAR(500) NOT NULL,
    applied_by            VARCHAR(255) NOT NULL,
    applied_at            TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cash_movements_register ON cash_movements(cash_register_id);
CREATE INDEX idx_cash_corrections_register ON cash_corrections(cash_register_id);
