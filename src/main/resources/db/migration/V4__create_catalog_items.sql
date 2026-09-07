CREATE TABLE catalog_items (
    catalog_item_id   UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    item_type         VARCHAR(20) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    capacity          INTEGER,
    restrictions      VARCHAR(500),
    valid_from        DATE,
    valid_to          DATE,
    policy            VARCHAR(500),
    image             VARCHAR(500),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_catalog_items_tenant ON catalog_items(tenant_id);
