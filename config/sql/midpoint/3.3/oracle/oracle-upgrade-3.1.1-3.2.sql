CREATE TABLE m_acc_cert_definition (
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
    oid VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_campaign (
    definitionRef_relation VARCHAR2(157 CHAR),
    definitionRef_targetOid VARCHAR2(36 CHAR),
    definitionRef_type NUMBER(10,0),
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
    oid VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT uc_acc_cert_definition_name  UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT uc_acc_cert_campaign_name  UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT fk_acc_cert_definition
    FOREIGN KEY (oid)
    REFERENCES m_object;

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT fk_acc_cert_campaign
    FOREIGN KEY (oid)
    REFERENCES m_object;
