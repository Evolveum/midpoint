CREATE TABLE m_acc_cert_definition (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
);

CREATE TABLE m_acc_cert_campaign (
    definitionRef_relation VARCHAR(157),
    definitionRef_targetOid VARCHAR(36),
    definitionRef_type int4,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
);

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT uc_acc_cert_definition_name  UNIQUE (name_norm);

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT uc_acc_cert_campaign_name  UNIQUE (name_norm);

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT fk_acc_cert_definition
    FOREIGN KEY (oid)
    REFERENCES m_object;

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT fk_acc_cert_campaign
    FOREIGN KEY (oid)
    REFERENCES m_object;
