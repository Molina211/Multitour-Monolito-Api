CREATE TABLE establishments (
    establishment_id  UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    kind              VARCHAR(20) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    description       VARCHAR(500),
    image             VARCHAR(500),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_establishments_tenant ON establishments(tenant_id);
