CREATE TABLE orgunit_constraint_it_system (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  it_system                      VARCHAR(64) NOT NULL,
  orgunit_uuid                   VARCHAR(36) NOT NULL,

  CONSTRAINT fk_orgunit_constraint_it_system_org_unit FOREIGN KEY (orgunit_uuid) REFERENCES orgunit(uuid) ON DELETE CASCADE
);

CREATE TABLE global_title_constraint_it_system (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  it_system                      VARCHAR(64) NOT NULL,
  title_id                       BIGINT NOT NULL,

  CONSTRAINT fk_global_title_constraint_it_system_title FOREIGN KEY (title_id) REFERENCES global_title(id) ON DELETE CASCADE
);