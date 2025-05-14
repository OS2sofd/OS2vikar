ALTER TABLE orgunit ADD column default_substitute_working_days BIGINT NOT NULL DEFAULT 0;
UPDATE orgunit o SET o.default_substitute_working_days = o.max_substitute_working_days;