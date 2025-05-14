ALTER TABLE substitute DROP COLUMN new_authorization_code;

CREATE TABLE nexus_sync (
  id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  substitute_id                     BIGINT NOT NULL,
  create_pending                    BOOL NOT NULL DEFAULT FALSE,
  sync_authorization_code           BOOL NOT NULL DEFAULT FALSE,
  
  CONSTRAINT fk_nexus_sync_substitute FOREIGN KEY (substitute_id) REFERENCES substitute(id) ON DELETE CASCADE
);