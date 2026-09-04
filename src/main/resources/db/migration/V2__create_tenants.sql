CREATE TABLE tenants (
    tenant_id       VARCHAR(50) PRIMARY KEY,
    commercial_name VARCHAR(150) NOT NULL,
    tenant_status   VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    membership_id      UUID PRIMARY KEY,
    tenant_id          VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    email              VARCHAR(150) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    role               VARCHAR(30) NOT NULL,
    membership_status  VARCHAR(20) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_records (
    audit_record_id       UUID PRIMARY KEY,
    tenant_id             VARCHAR(50),
    actor_id              VARCHAR(150) NOT NULL,
    action                VARCHAR(50) NOT NULL,
    affected_record_id    VARCHAR(100) NOT NULL,
    reason                VARCHAR(500),
    recorded_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX idx_audit_records_tenant ON audit_records(tenant_id);
