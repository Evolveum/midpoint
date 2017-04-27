CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE TABLE m_audit_prop_value (
  id        BIGINT NOT NULL,
  name      VARCHAR(255),
  record_id BIGINT,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_audit_ref_value (
  id              BIGINT NOT NULL,
  name            VARCHAR(255),
  oid             VARCHAR(255),
  record_id       BIGINT,
  targetName_norm VARCHAR(255),
  targetName_orig VARCHAR(255),
  type            VARCHAR(255),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id);

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id);

ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

CREATE TABLE m_object_text_info (
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE TABLE m_case (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);

ALTER TABLE m_case
  ADD CONSTRAINT fk_case
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_assignment_reference CHANGE containerType targetType INTEGER;
ALTER TABLE m_reference CHANGE containerType targetType INTEGER;

DROP TABLE m_acc_cert_case_reference;

ALTER TABLE m_acc_cert_case CHANGE currentStageNumber stageNumber INTEGER;
ALTER TABLE m_acc_cert_case DROP COLUMN currentStageOutcome;
ALTER TABLE m_acc_cert_case DROP COLUMN overallOutcome;
ALTER TABLE m_acc_cert_case ADD COLUMN currentStageOutcome VARCHAR(255);
ALTER TABLE m_acc_cert_case ADD COLUMN outcome VARCHAR(255);

DROP TABLE m_acc_cert_decision;

CREATE TABLE m_acc_cert_wi (
  id                     INTEGER     NOT NULL,
  owner_id               INTEGER     NOT NULL,
  owner_owner_oid        VARCHAR(36) NOT NULL,
  closeTimestamp         DATETIME(6),
  outcome                VARCHAR(255),
  outputChangeTimestamp  DATETIME(6),
  performerRef_relation  VARCHAR(157),
  performerRef_targetOid VARCHAR(36),
  performerRef_type      INTEGER,
  stageNumber            INTEGER,
  PRIMARY KEY (id, owner_id, owner_owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_acc_cert_wi_reference (
  owner_id              INTEGER      NOT NULL,
  owner_owner_id        INTEGER      NOT NULL,
  owner_owner_owner_oid VARCHAR(36)  NOT NULL,
  relation              VARCHAR(157) NOT NULL,
  targetOid             VARCHAR(36)  NOT NULL,
  targetType            INTEGER,
  PRIMARY KEY (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iCertWorkItemRefTargetOid ON m_acc_cert_wi_reference (targetOid);

ALTER TABLE m_acc_cert_case DROP FOREIGN KEY fk_acc_cert_case_owner;

ALTER TABLE m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner
FOREIGN KEY (owner_oid)
REFERENCES m_acc_cert_campaign (oid);

ALTER TABLE m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case (id, owner_oid);

ALTER TABLE m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner
FOREIGN KEY (owner_id, owner_owner_id, owner_owner_owner_oid)
REFERENCES m_acc_cert_wi (id, owner_id, owner_owner_oid);

ALTER TABLE m_shadow ADD COLUMN pendingOperationCount INTEGER;

CREATE INDEX iShadowKind ON m_shadow (kind);

CREATE INDEX iShadowIntent ON m_shadow (intent);

CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);

CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);

CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);

CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);

CREATE TABLE m_operation_execution (
  id                     INTEGER     NOT NULL,
  owner_oid              VARCHAR(36) NOT NULL,
  initiatorRef_relation  VARCHAR(157),
  initiatorRef_targetOid VARCHAR(36),
  initiatorRef_type      INTEGER,
  status                 INTEGER,
  taskRef_relation       VARCHAR(157),
  taskRef_targetOid      VARCHAR(36),
  taskRef_type           INTEGER,
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iOpExecTaskOid
  ON m_operation_execution (taskRef_targetOid);

CREATE INDEX iOpExecInitiatorOid
  ON m_operation_execution (initiatorRef_targetOid);

CREATE INDEX iOpExecStatus
  ON m_operation_execution (status);

ALTER TABLE m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);
