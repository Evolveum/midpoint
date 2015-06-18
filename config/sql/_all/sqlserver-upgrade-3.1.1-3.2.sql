CREATE TABLE m_acc_cert_definition (
    name_norm NVARCHAR(255) COLLATE database_default,
    name_orig NVARCHAR(255) COLLATE database_default,
    oid NVARCHAR(36) COLLATE database_default NOT NULL,
    PRIMARY KEY (oid)
);

CREATE TABLE m_acc_cert_campaign (
    definitionRef_relation NVARCHAR(157) COLLATE database_default,
    definitionRef_targetOid NVARCHAR(36) COLLATE database_default,
    definitionRef_type INT,
    name_norm NVARCHAR(255) COLLATE database_default,
    name_orig NVARCHAR(255) COLLATE database_default,
    oid NVARCHAR(36) COLLATE database_default NOT NULL,
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
