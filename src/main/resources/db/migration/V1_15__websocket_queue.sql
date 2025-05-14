CREATE TABLE websocket_queue (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                       DATETIME NOT NULL,
  command                   VARCHAR(64) NOT NULL,
  target                    VARCHAR(255) NULL,
  payload                   VARCHAR(255) NULL
);