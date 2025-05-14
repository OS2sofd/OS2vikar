ALTER TABLE substitute ADD COLUMN latest_stop_date DATE NULL;
UPDATE substitute s SET s.latest_stop_date = (SELECT MAX(stop_date) FROM workplace WHERE substitute_id = s.id);
UPDATE substitute SET latest_stop_date = NOW() WHERE latest_stop_date IS NULL;
