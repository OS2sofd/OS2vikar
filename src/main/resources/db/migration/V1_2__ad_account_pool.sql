CREATE TABLE ad_account_pool (
  id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  username                          VARCHAR(255) NOT NULL,
  with_o365_license                 BOOLEAN NOT NULL DEFAULT FALSE,
  created                           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status                            VARCHAR(255) NOT NULL,
  status_message                    TEXT
);

CREATE TABLE settings (
  id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  setting_key                       VARCHAR(255) NOT NULL,
  setting_value                     VARCHAR(255) NOT NULL
);