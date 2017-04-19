CREATE TABLE m_form (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE TABLE m_audit_prop_value (
  id        BIGINT NOT NULL,
  name      NVARCHAR(255) COLLATE database_default,
  record_id BIGINT,
  value     NVARCHAR(1024) COLLATE database_default,
  PRIMARY KEY (id)
);

CREATE TABLE m_audit_ref_value (
  id              BIGINT NOT NULL,
  name            NVARCHAR(255) COLLATE database_default,
  oid             NVARCHAR(255) COLLATE database_default,
  record_id       BIGINT,
  targetName_norm NVARCHAR(255) COLLATE database_default,
  targetName_orig NVARCHAR(255) COLLATE database_default,
  type            NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (id)
);

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id);

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id);

ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

CREATE TABLE m_object_text_info (
  owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  text      NVARCHAR(255) COLLATE database_default NOT NULL,
  PRIMARY KEY (owner_oid, text)
);

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE TABLE m_case (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);

ALTER TABLE m_case
  ADD CONSTRAINT fk_case
FOREIGN KEY (oid)
REFERENCES m_object;

EXEC sp_rename 'm_assignment_reference.containerType', 'targetType', 'COLUMN';
EXEC sp_rename 'm_reference.containerType', 'targetType', 'COLUMN';

DROP TABLE m_acc_cert_case_reference;

EXEC sp_rename 'm_acc_cert_case.currentStageNumber', 'stageNumber', 'COLUMN';
ALTER TABLE m_acc_cert_case DROP COLUMN currentStageOutcome;
ALTER TABLE m_acc_cert_case DROP COLUMN overallOutcome;
ALTER TABLE m_acc_cert_case ADD currentStageOutcome NVARCHAR(255);
ALTER TABLE m_acc_cert_case ADD outcome NVARCHAR(255);

DROP TABLE m_acc_cert_decision;

CREATE TABLE m_acc_cert_wi (
  id                     INT                                   NOT NULL,
  owner_id               INT                                   NOT NULL,
  owner_owner_oid        NVARCHAR(36) COLLATE database_default NOT NULL,
  closeTimestamp         DATETIME2,
  outcome                NVARCHAR(255) COLLATE database_default,
  outputChangeTimestamp  DATETIME2,
  performerRef_relation  NVARCHAR(157) COLLATE database_default,
  performerRef_targetOid NVARCHAR(36) COLLATE database_default,
  performerRef_type      INT,
  stageNumber            INT,
  PRIMARY KEY (id, owner_id, owner_owner_oid)
);

CREATE TABLE m_acc_cert_wi_reference (
  owner_id              INT                                    NOT NULL,
  owner_owner_id        INT                                    NOT NULL,
  owner_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  relation              NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid             NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType            INT,
  PRIMARY KEY (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)
);

CREATE INDEX iCertWorkItemRefTargetOid ON m_acc_cert_wi_reference (targetOid);

ALTER TABLE m_acc_cert_case DROP CONSTRAINT fk_acc_cert_case_owner;

ALTER TABLE m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner
FOREIGN KEY (owner_oid)
REFERENCES m_acc_cert_campaign;

ALTER TABLE m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case;

ALTER TABLE m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner
FOREIGN KEY (owner_id, owner_owner_id, owner_owner_owner_oid)
REFERENCES m_acc_cert_wi;
