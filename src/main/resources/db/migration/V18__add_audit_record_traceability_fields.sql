ALTER TABLE audit_records
    ADD COLUMN previous_value VARCHAR(500),
    ADD COLUMN new_value VARCHAR(500),
    ADD COLUMN channel_or_module VARCHAR(150),
    ADD COLUMN functional_process_reference VARCHAR(255);
