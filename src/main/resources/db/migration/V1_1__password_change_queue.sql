CREATE TABLE password_change_queue (
  id                               BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  samaccount_name                  VARCHAR(255) NOT NULL,
  password                         VARCHAR(255) NOT NULL,
  tts                              DATETIME NULL,
  status                           VARCHAR(255) NULL,
  message                          TEXT
);
