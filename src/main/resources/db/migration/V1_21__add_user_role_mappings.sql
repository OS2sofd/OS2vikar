CREATE TABLE global_title_user_role (
   id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   user_role_id              BIGINT NOT NULL,
   global_title_id           BIGINT NOT NULL,

   CONSTRAINT FK_GLOBAL_TITLE_USER_ROLE_ON_GLOBAL_TITLE FOREIGN KEY (global_title_id) REFERENCES global_title (id) ON DELETE CASCADE,
   CONSTRAINT FK_GLOBAL_TITLE_USER_ROLE_ON_USER_ROLE FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE
);

CREATE TABLE org_unit_user_role (
   id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   user_role_id              BIGINT NOT NULL,
   orgunit_uuid              VARCHAR(36) NOT NULL,

   CONSTRAINT FK_ORG_UNIT_USER_ROLE_ON_ORGUNIT_UUID FOREIGN KEY (orgunit_uuid) REFERENCES orgunit (uuid) ON DELETE CASCADE,
   CONSTRAINT FK_ORG_UNIT_USER_ROLE_ON_USER_ROLE FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE
);
