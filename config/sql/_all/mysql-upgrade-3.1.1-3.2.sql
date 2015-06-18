CREATE TABLE m1_a6_c13_definition (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE=InnoDB;

CREATE TABLE m1_a6_certification_campaign (
    definitionRef_relation VARCHAR(157),
    definitionRef_targetOid VARCHAR(36),
    definitionRef_type INTEGER,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE=InnoDB;

ALTER TABLE m1_a6_c13_definition
    ADD CONSTRAINT uc_a6_c13_definition_name  UNIQUE (name_norm);

ALTER TABLE m1_a6_certification_campaign
    ADD CONSTRAINT uc_a6_c13_campaign_name  UNIQUE (name_norm);

ALTER TABLE m1_a6_c13_definition
    ADD CONSTRAINT fk_a6_c13_definition
    FOREIGN KEY (oid)
    REFERENCES m_object (oid);

ALTER TABLE m1_a6_certification_campaign
    ADD CONSTRAINT fk_a6_c13_campaign
    FOREIGN KEY (oid)
    REFERENCES m_object (oid);
