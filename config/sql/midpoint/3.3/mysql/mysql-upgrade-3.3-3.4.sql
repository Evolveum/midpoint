ALTER TABLE m_acc_cert_campaign
ADD endTimestamp DATETIME(6),
ADD handlerUri VARCHAR(255),
ADD ownerRef_relation VARCHAR(157),
ADD ownerRef_targetOid VARCHAR(36),
ADD ownerRef_type INTEGER,
ADD stageNumber INTEGER,
ADD startTimestamp DATETIME(6),
ADD state INTEGER;

ALTER TABLE m_acc_cert_definition
ADD handlerUri VARCHAR(255),
ADD lastCampaignClosedTimestamp DATETIME(6),
ADD lastCampaignStartedTimestamp DATETIME(6),
ADD ownerRef_relation VARCHAR(157),
ADD ownerRef_targetOid VARCHAR(36),
ADD ownerRef_type INTEGER;

CREATE TABLE m_acc_cert_case (
  id                       INTEGER     NOT NULL,
  owner_oid                VARCHAR(36) NOT NULL,
  administrativeStatus     INTEGER,
  archiveTimestamp         DATETIME(6),
  disableReason            VARCHAR(255),
  disableTimestamp         DATETIME(6),
  effectiveStatus          INTEGER,
  enableTimestamp          DATETIME(6),
  validFrom                DATETIME(6),
  validTo                  DATETIME(6),
  validityChangeTimestamp  DATETIME(6),
  validityStatus           INTEGER,
  currentResponse          INTEGER,
  currentStageNumber       INTEGER,
  fullObject               LONGBLOB,
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INTEGER,
  orgRef_relation          VARCHAR(157),
  orgRef_targetOid         VARCHAR(36),
  orgRef_type              INTEGER,
  remediedTimestamp        DATETIME(6),
  reviewDeadline           DATETIME(6),
  reviewRequestedTimestamp DATETIME(6),
  targetRef_relation       VARCHAR(157),
  targetRef_targetOid      VARCHAR(36),
  targetRef_type           INTEGER,
  tenantRef_relation       VARCHAR(157),
  tenantRef_targetOid      VARCHAR(36),
  tenantRef_type           INTEGER,
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_acc_cert_case_reference (
  owner_id        INTEGER      NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INTEGER      NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  containerType   INTEGER,
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_acc_cert_decision (
  id                    INTEGER     NOT NULL,
  owner_id              INTEGER     NOT NULL,
  owner_owner_oid       VARCHAR(36) NOT NULL,
  reviewerComment       VARCHAR(255),
  response              INTEGER,
  reviewerRef_relation  VARCHAR(157),
  reviewerRef_targetOid VARCHAR(36),
  reviewerRef_type      INTEGER,
  stageNumber           INTEGER     NOT NULL,
  timestamp             DATETIME(6),
  PRIMARY KEY (id, owner_id, owner_owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iCaseObjectRefTargetOid ON m_acc_cert_case (objectRef_targetOid);

CREATE INDEX iCaseTargetRefTargetOid ON m_acc_cert_case (targetRef_targetOid);

CREATE INDEX iCaseTenantRefTargetOid ON m_acc_cert_case (tenantRef_targetOid);

CREATE INDEX iCaseOrgRefTargetOid ON m_acc_cert_case (orgRef_targetOid);

CREATE INDEX iCaseReferenceTargetOid ON m_acc_cert_case_reference (targetOid);

ALTER TABLE m_acc_cert_decision
ADD CONSTRAINT uc_case_stage_reviewer UNIQUE (owner_owner_oid, owner_id, stageNumber, reviewerRef_targetOid);

ALTER TABLE m_acc_cert_case
ADD CONSTRAINT fk_acc_cert_case_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

ALTER TABLE m_acc_cert_case_reference
ADD CONSTRAINT fk_acc_cert_case_reference_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case (id, owner_oid);

ALTER TABLE m_acc_cert_decision
ADD CONSTRAINT fk_acc_cert_decision_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case (id, owner_oid);

