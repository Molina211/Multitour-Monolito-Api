ALTER TABLE tenants
    ADD COLUMN allow_collaborator_support_validation BOOLEAN NOT NULL DEFAULT false;
