CREATE TABLE ad_group (
   id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   name                           VARCHAR(255) NOT NULL,
   object_guid                    VARCHAR(36) NOT NULL
);

CREATE TABLE org_unit_ad_group (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  orgunit_uuid                    VARCHAR(36) NOT NULL,
  ad_group_id                     BIGINT NOT NULL,

  CONSTRAINT fk_org_unit_ad_group_org_unit FOREIGN KEY (orgunit_uuid) REFERENCES orgunit(uuid) ON DELETE CASCADE,
  CONSTRAINT fk_org_unit_ad_group_ad_group FOREIGN KEY (ad_group_id) REFERENCES ad_group(id) ON DELETE CASCADE
);

CREATE TABLE global_title_ad_group (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  global_title_id                 BIGINT NOT NULL,
  ad_group_id                     BIGINT NOT NULL,

  CONSTRAINT fk_global_title_ad_group_global_title_id FOREIGN KEY (global_title_id) REFERENCES global_title(id) ON DELETE CASCADE,
  CONSTRAINT fk_global_title_ad_group_ad_group FOREIGN KEY (ad_group_id) REFERENCES ad_group(id) ON DELETE CASCADE
);