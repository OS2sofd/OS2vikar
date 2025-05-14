DROP TABLE nexus_sync_calendar;
DROP TABLE nexus_sync;
ALTER TABLE substitute DROP COLUMN authorization_code;
ALTER TABLE substitute DROP COLUMN nexus_organization_supplier_id;

CREATE TABLE nexus_sync_calendar_queue (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  tries                           BIGINT NOT NULL DEFAULT 0,
  nexus_calendar_name             VARCHAR(255) NOT NULL,
  nexus_calendar_id               BIGINT NOT NULL,
  nexus_resource_id               VARCHAR(255) NOT NULL,
  nexus_resource_name             VARCHAR(255) NOT NULL,
  workplace_start_date            VARCHAR(255) NOT NULL,
  change_strategy                 VARCHAR(255) NOT NULL,
  substitute_id                   BIGINT NOT NULL,

  CONSTRAINT fk_nexus_sync_calendar_queue_substitute FOREIGN KEY (substitute_id) REFERENCES substitute(id) ON DELETE CASCADE
);