CREATE TABLE orgunit (
  uuid                            VARCHAR(36) NOT NULL PRIMARY KEY,
  name                            VARCHAR(255) NOT NULL,
  can_have_substitutes            BOOLEAN NOT NULL DEFAULT FALSE,
  max_substitute_working_days     BIGINT NOT NULL DEFAULT 0,
  parent_uuid                     VARCHAR(36) NULL
);

CREATE TABLE it_system (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                            VARCHAR(255) NOT NULL,
  it_system_id                    BIGINT NOT NULL
);

CREATE TABLE user_role (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_role_id                    BIGINT NOT NULL,
  name                            VARCHAR(64) NOT NULL,
  description                     TEXT,
  it_system_id                    BIGINT NOT NULL,

  CONSTRAINT fk_user_roles_it_systems FOREIGN KEY (it_system_id) REFERENCES it_system(id) ON DELETE CASCADE
);

CREATE TABLE global_title (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  title                           VARCHAR(255) NOT NULL
);

INSERT INTO global_title (title) VALUES ('Vikar');

CREATE TABLE local_title (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  title                           VARCHAR(255) NOT NULL,
  orgunit_uuid                    VARCHAR(36) NOT NULL,

  CONSTRAINT fk_local_titles_org_unit FOREIGN KEY (orgunit_uuid) REFERENCES orgunit(uuid) ON DELETE CASCADE
);

CREATE TABLE global_role (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_role_id                    BIGINT NOT NULL,
	
  CONSTRAINT fk_global_roles_user_role FOREIGN KEY (user_role_id) REFERENCES user_role(id) ON DELETE CASCADE
);

CREATE TABLE local_role (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  orgunit_uuid                    VARCHAR(36) NOT NULL,
  user_role_id                    BIGINT NOT NULL,

  CONSTRAINT fk_local_roles_user_role FOREIGN KEY (user_role_id) REFERENCES user_role(id) ON DELETE CASCADE,
  CONSTRAINT fk_local_roles_org_unit FOREIGN KEY (orgunit_uuid) REFERENCES orgunit(uuid) ON DELETE CASCADE
);

CREATE TABLE substitute (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  uuid                            VARCHAR(36) NULL,
  cpr                             VARCHAR(10) NOT NULL,
  firstname                       VARCHAR(255) NOT NULL,
  surname                         VARCHAR(255) NOT NULL,
  username                        VARCHAR(255) NULL,
  email                           VARCHAR(255) NULL,
  phone                           VARCHAR(255) NULL,
  agency                          VARCHAR(255) NULL,
  last_updated                    TIMESTAMP NULL,
  created                         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workplace (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  master_id                       VARCHAR(36) NOT NULL,
  start_date                      DATE NOT NULL,
  stop_date                       DATE NOT NULL,
  orgunit_uuid                    VARCHAR(36) NOT NULL,
  title							  VARCHAR(255) NOT NULL,
  substitute_id                   BIGINT NOT NULL,

  CONSTRAINT fk_workplaces_substitute FOREIGN KEY (substitute_id) REFERENCES substitute(id) ON DELETE CASCADE,
  CONSTRAINT fk_workplaces_org_unit FOREIGN KEY (orgunit_uuid) REFERENCES orgunit(uuid) ON DELETE CASCADE
);

CREATE TABLE workplace_assigned_role (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  workplace_id                   BIGINT NOT NULL,
  sync_to_be_added               BOOLEAN NOT NULL DEFAULT FALSE,
  sync_removed                   BOOLEAN NOT NULL DEFAULT FALSE,
  user_role_id                   BIGINT NOT NULL,
  name                           VARCHAR(64) NOT NULL,

  CONSTRAINT fk_workplace_workplaces_global_roles FOREIGN KEY (workplace_id) REFERENCES workplace(id) ON DELETE CASCADE
);

CREATE TABLE auditlog (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip                             VARCHAR(255) NOT NULL,
  user_id                        VARCHAR(255) NOT NULL,
  operation                      VARCHAR(255) NOT NULL,
  details                        TEXT
);
