ALTER TABLE auditlog ADD COLUMN substitute VARCHAR(255) NULL;
ALTER TABLE auditlog MODIFY user_id VARCHAR(255) NULL;
ALTER TABLE auditlog CHANGE user_id administrator VARCHAR(225) NULL;