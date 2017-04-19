CREATE TABLE m_form (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE TABLE m_audit_prop_value (
  id        NUMBER(19, 0) NOT NULL,
  name      VARCHAR2(255 CHAR),
  record_id NUMBER(19, 0),
  value     VARCHAR2(1024 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_audit_ref_value (
  id              NUMBER(19, 0) NOT NULL,
  name            VARCHAR2(255 CHAR),
  oid             VARCHAR2(255 CHAR),
  record_id       NUMBER(19, 0),
  targetName_norm VARCHAR2(255 CHAR),
  targetName_orig VARCHAR2(255 CHAR),
  type            VARCHAR2(255 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id) INITRANS 30;

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id) INITRANS 30;


ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

CREATE TABLE m_object_text_info (
  owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  text      VARCHAR2(255 CHAR) NOT NULL,
  PRIMARY KEY (owner_oid, text)
) INITRANS 30;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE TABLE m_case (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm) INITRANS 30;

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
ALTER TABLE m_acc_cert_case ADD (currentStageOutcome VARCHAR2(255 CHAR));
ALTER TABLE m_acc_cert_case ADD (outcome VARCHAR2(255 CHAR));

DROP TABLE m_acc_cert_decision;

CREATE TABLE m_acc_cert_wi (
  id                     NUMBER(10, 0)     NOT NULL,
  owner_id               NUMBER(10, 0)     NOT NULL,
  owner_owner_oid        VARCHAR2(36 CHAR) NOT NULL,
  closeTimestamp         TIMESTAMP,
  outcome                VARCHAR2(255 CHAR),
  outputChangeTimestamp  TIMESTAMP,
  performerRef_relation  VARCHAR2(157 CHAR),
  performerRef_targetOid VARCHAR2(36 CHAR),
  performerRef_type      NUMBER(10, 0),
  stageNumber            NUMBER(10, 0),
  PRIMARY KEY (id, owner_id, owner_owner_oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_wi_reference (
  owner_id              NUMBER(10, 0)      NOT NULL,
  owner_owner_id        NUMBER(10, 0)      NOT NULL,
  owner_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  relation              VARCHAR2(157 CHAR) NOT NULL,
  targetOid             VARCHAR2(36 CHAR)  NOT NULL,
  targetType            NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)
) INITRANS 30;

CREATE INDEX iCertWorkItemRefTargetOid ON m_acc_cert_wi_reference (targetOid) INITRANS 30;

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
