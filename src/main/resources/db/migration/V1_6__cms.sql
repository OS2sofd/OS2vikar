CREATE TABLE cms_message (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  last_updated                   DATETIME NULL,
  cms_key                        VARCHAR(255) NOT NULL,
  cms_value                      mediumtext NOT NULL
);