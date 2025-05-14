ALTER TABLE substitute ADD COLUMN authorization_code VARCHAR(64) NULL;
ALTER TABLE substitute ADD COLUMN authorization_code_checked BOOLEAN NOT NULL DEFAULT FALSE;
