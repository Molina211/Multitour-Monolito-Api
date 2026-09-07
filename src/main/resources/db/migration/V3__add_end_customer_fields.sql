ALTER TABLE memberships
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100),
    ADD COLUMN phone      VARCHAR(30);

CREATE UNIQUE INDEX uq_memberships_tenant_email ON memberships(tenant_id, email);
