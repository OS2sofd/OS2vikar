CREATE TABLE nexus_calendar (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                            VARCHAR(255) NOT NULL,
  nexus_id                        BIGINT NOT NULL
);

CREATE TABLE nexus_calendar_resource (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                            VARCHAR(255) NOT NULL,
  initials                        VARCHAR(255) NOT NULL,
  nexus_resource_id               VARCHAR(255) NOT NULL,
  type                            VARCHAR(36) NOT NULL,
  nexus_calendar_id               BIGINT NOT NULL,

  CONSTRAINT fk_nexus_calendar_resource_nexus_calendar FOREIGN KEY (nexus_calendar_id) REFERENCES nexus_calendar(id) ON DELETE CASCADE
);

CREATE TABLE nexus_sync_calendar (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  nexus_calendar_name             VARCHAR(255) NOT NULL,
  nexus_calendar_id               BIGINT NOT NULL,
  nexus_resource_id               VARCHAR(255) NOT NULL,
  nexus_resource_name             VARCHAR(255) NOT NULL,
  workplace_start_date            VARCHAR(255) NOT NULL,
  change_strategy                 VARCHAR(255) NOT NULL,
  nexus_sync_id                   BIGINT NOT NULL,

  CONSTRAINT fk_nexus_sync_calendar_nexus_sync FOREIGN KEY (nexus_sync_id) REFERENCES nexus_sync(id) ON DELETE CASCADE
);

ALTER TABLE workplace ADD COLUMN nexus_calendar_name VARCHAR(255) NULL;
ALTER TABLE workplace ADD COLUMN nexus_calendar_resource_name VARCHAR(255) NULL;