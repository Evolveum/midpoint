CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE TABLE m_audit_prop_value (
  id        INT8 NOT NULL,
  name      VARCHAR(255),
  record_id INT8,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);

CREATE TABLE m_audit_ref_value (
  id              INT8 NOT NULL,
  name            VARCHAR(255),
  oid             VARCHAR(255),
  record_id       INT8,
  targetName_norm VARCHAR(255),
  targetName_orig VARCHAR(255),
  type            VARCHAR(255),
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
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
);

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE TABLE m_case (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);

ALTER TABLE m_case
  ADD CONSTRAINT fk_case
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_assignment_reference RENAME COLUMN containerType TO targetType;
ALTER TABLE m_reference RENAME COLUMN containerType TO targetType;

DROP TABLE m_acc_cert_case_reference;

ALTER TABLE m_acc_cert_case RENAME COLUMN currentStageNumber TO stageNumber;
ALTER TABLE m_acc_cert_case DROP COLUMN currentStageOutcome;
ALTER TABLE m_acc_cert_case DROP COLUMN overallOutcome;
ALTER TABLE m_acc_cert_case ADD COLUMN currentStageOutcome VARCHAR(255);
ALTER TABLE m_acc_cert_case ADD COLUMN outcome VARCHAR(255);

DROP TABLE m_acc_cert_decision;

CREATE TABLE m_acc_cert_wi (
  id                     INT4        NOT NULL,
  owner_id               INT4        NOT NULL,
  owner_owner_oid        VARCHAR(36) NOT NULL,
  closeTimestamp         TIMESTAMP,
  outcome                VARCHAR(255),
  outputChangeTimestamp  TIMESTAMP,
  performerRef_relation  VARCHAR(157),
  performerRef_targetOid VARCHAR(36),
  performerRef_type      INT4,
  stageNumber            INT4,
  PRIMARY KEY (id, owner_id, owner_owner_oid)
);

CREATE TABLE m_acc_cert_wi_reference (
  owner_id              INT4         NOT NULL,
  owner_owner_id        INT4         NOT NULL,
  owner_owner_owner_oid VARCHAR(36)  NOT NULL,
  relation              VARCHAR(157) NOT NULL,
  targetOid             VARCHAR(36)  NOT NULL,
  targetType            INT4,
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

ALTER TABLE m_shadow ADD COLUMN pendingOperationCount INT4;

CREATE INDEX iShadowKind ON m_shadow (kind);

CREATE INDEX iShadowIntent ON m_shadow (intent);

CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);

CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);

CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);

CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);
