ALTER TABLE reserved_services
    ADD COLUMN transport_item_id UUID,
    ADD COLUMN transport_cost    NUMERIC(12,2);
