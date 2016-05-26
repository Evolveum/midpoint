CREATE TABLE m_service (
  displayOrder  INTEGER,
  locality_norm VARCHAR(255),
  locality_orig VARCHAR(255),
  name_norm     VARCHAR(255),
  name_orig     VARCHAR(255),
  oid           VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_service_type (
  service_oid VARCHAR(36) NOT NULL,
  serviceType VARCHAR(255)
);

ALTER TABLE m_service
  ADD CONSTRAINT fk_service
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type
FOREIGN KEY (service_oid)
REFERENCES m_service;

ALTER TABLE m_task ADD wfEndTimestamp           TIMESTAMP;
ALTER TABLE m_task ADD wfObjectRef_relation     VARCHAR(157);
ALTER TABLE m_task ADD wfObjectRef_targetOid    VARCHAR(36);
ALTER TABLE m_task ADD wfObjectRef_type         INTEGER;
ALTER TABLE m_task ADD wfProcessInstanceId      VARCHAR(255);
ALTER TABLE m_task ADD wfRequesterRef_relation  VARCHAR(157);
ALTER TABLE m_task ADD wfRequesterRef_targetOid VARCHAR(36);
ALTER TABLE m_task ADD wfRequesterRef_type      INTEGER;
ALTER TABLE m_task ADD wfStartTimestamp         TIMESTAMP;
ALTER TABLE m_task ADD wfTargetRef_relation     VARCHAR(157);
ALTER TABLE m_task ADD wfTargetRef_targetOid    VARCHAR(36);
ALTER TABLE m_task ADD wfTargetRef_type         INTEGER;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

ALTER TABLE m_abstract_role ADD ownerRef_relation VARCHAR(157);
ALTER TABLE m_abstract_role ADD ownerRef_targetOid VARCHAR(36);
ALTER TABLE m_abstract_role ADD ownerRef_type INTEGER;

ALTER TABLE m_acc_cert_campaign ADD endTimestamp TIMESTAMP;
ALTER TABLE m_acc_cert_campaign ADD handlerUri VARCHAR(255);
ALTER TABLE m_acc_cert_campaign ADD ownerRef_relation VARCHAR(157);
ALTER TABLE m_acc_cert_campaign ADD ownerRef_targetOid VARCHAR(36);
ALTER TABLE m_acc_cert_campaign ADD ownerRef_type INTEGER;
ALTER TABLE m_acc_cert_campaign ADD stageNumber INTEGER;
ALTER TABLE m_acc_cert_campaign ADD startTimestamp TIMESTAMP;
ALTER TABLE m_acc_cert_campaign ADD state INTEGER;

ALTER TABLE m_acc_cert_definition ADD handlerUri VARCHAR(255);
ALTER TABLE m_acc_cert_definition ADD lastCampaignClosedTimestamp TIMESTAMP;
ALTER TABLE m_acc_cert_definition ADD lastCampaignStartedTimestamp TIMESTAMP;
ALTER TABLE m_acc_cert_definition ADD ownerRef_relation VARCHAR(157);
ALTER TABLE m_acc_cert_definition ADD ownerRef_targetOid VARCHAR(36);
ALTER TABLE m_acc_cert_definition ADD ownerRef_type INTEGER;

CREATE TABLE m_acc_cert_case (
  id                       INTEGER     NOT NULL,
  owner_oid                VARCHAR(36) NOT NULL,
  administrativeStatus     INTEGER,
  archiveTimestamp         TIMESTAMP,
  disableReason            VARCHAR(255),
  disableTimestamp         TIMESTAMP,
  effectiveStatus          INTEGER,
  enableTimestamp          TIMESTAMP,
  validFrom                TIMESTAMP,
  validTo                  TIMESTAMP,
  validityChangeTimestamp  TIMESTAMP,
  validityStatus           INTEGER,
  currentStageNumber       INTEGER,
  currentStageOutcome      INTEGER,
  fullObject               BLOB,
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INTEGER,
  orgRef_relation          VARCHAR(157),
  orgRef_targetOid         VARCHAR(36),
  orgRef_type              INTEGER,
  overallOutcome           INTEGER,
  remediedTimestamp        TIMESTAMP,
  reviewDeadline           TIMESTAMP,
  reviewRequestedTimestamp TIMESTAMP,
  targetRef_relation       VARCHAR(157),
  targetRef_targetOid      VARCHAR(36),
  targetRef_type           INTEGER,
  tenantRef_relation       VARCHAR(157),
  tenantRef_targetOid      VARCHAR(36),
  tenantRef_type           INTEGER,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_acc_cert_case_reference (
  owner_id        INTEGER      NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INTEGER      NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  containerType   INTEGER,
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
);

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
  timestamp             TIMESTAMP,
  PRIMARY KEY (id, owner_id, owner_owner_oid)
);

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
REFERENCES m_object;

ALTER TABLE m_acc_cert_case_reference
ADD CONSTRAINT fk_acc_cert_case_ref_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case;

ALTER TABLE m_acc_cert_decision
ADD CONSTRAINT fk_acc_cert_decision_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case;

ALTER TABLE m_lookup_table_row
DROP CONSTRAINT uc_row_key;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);

ALTER TABLE m_abstract_role ADD displayName_norm   VARCHAR(255);
ALTER TABLE m_abstract_role ADD displayName_orig   VARCHAR(255);
ALTER TABLE m_abstract_role ADD identifier         VARCHAR(255);
ALTER TABLE m_abstract_role ADD riskLevel          VARCHAR(255);

ALTER TABLE m_org DROP COLUMN displayName_norm;
ALTER TABLE m_org DROP COLUMN displayName_orig;
ALTER TABLE m_org DROP COLUMN identifier;

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);
