ALTER TABLE substitute ADD COLUMN nexus_organization_supplier_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE nexus_sync ADD COLUMN sync_default_organization_supplier BOOLEAN NOT NULL DEFAULT FALSE;