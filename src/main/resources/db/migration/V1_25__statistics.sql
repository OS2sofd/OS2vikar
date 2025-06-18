CREATE TABLE statistic (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  workplace_id                    BIGINT NOT NULL,
  start_date                      DATE NOT NULL,
  stop_date                       DATE NOT NULL,
  orgunit_uuid                    VARCHAR(255) NOT NULL,
  orgunit_name                    VARCHAR(255) NOT NULL,
  substitute_name                 VARCHAR(255) NOT NULL,
  substitute_user_id              VARCHAR(64) NOT NULL,
  
  UNIQUE uc_statistic_workplace (workplace_id)
);
