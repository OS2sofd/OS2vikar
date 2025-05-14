ALTER TABLE substitute ADD COLUMN assign_employee_signature BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE substitute ADD COLUMN authorization_code VARCHAR(255) NULL;
ALTER TABLE substitute ADD COLUMN new_authorization_code BOOLEAN NOT NULL DEFAULT FALSE;