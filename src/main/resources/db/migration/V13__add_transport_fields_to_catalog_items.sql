ALTER TABLE catalog_items
    ADD COLUMN route             VARCHAR(200),
    ADD COLUMN operational_cost  NUMERIC(12,2);
