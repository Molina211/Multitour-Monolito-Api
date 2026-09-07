CREATE TABLE discounts (
    discount_id       UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    catalog_item_id   UUID NOT NULL REFERENCES catalog_items(catalog_item_id),
    percentage        INTEGER NOT NULL,
    valid_from        DATE,
    valid_to          DATE,
    priority          INTEGER NOT NULL DEFAULT 0,
    stackable         BOOLEAN NOT NULL DEFAULT FALSE,
    cap               NUMERIC(12,2),
    base              VARCHAR(20) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_discounts_tenant ON discounts(tenant_id);
CREATE INDEX idx_discounts_catalog_item ON discounts(catalog_item_id);
