ALTER TABLE m_task ADD
  wfEndTimestamp           DATETIME2,
  wfObjectRef_relation     NVARCHAR(157) COLLATE database_default,
  wfObjectRef_targetOid    NVARCHAR(36) COLLATE database_default,
  wfObjectRef_type         INT,
  wfProcessInstanceId      NVARCHAR(255) COLLATE database_default,
  wfRequesterRef_relation  NVARCHAR(157) COLLATE database_default,
  wfRequesterRef_targetOid NVARCHAR(36) COLLATE database_default,
  wfRequesterRef_type      INT,
  wfStartTimestamp         DATETIME2,
  wfTargetRef_relation     NVARCHAR(157) COLLATE database_default,
  wfTargetRef_targetOid    NVARCHAR(36) COLLATE database_default,
  wfTargetRef_type         INT;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

ALTER TABLE m_abstract_role ADD
  ownerRef_relation  NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid NVARCHAR(36) COLLATE database_default,
  ownerRef_type      INT;

ALTER TABLE m_acc_cert_campaign ADD
endTimestamp            DATETIME2,
handlerUri              NVARCHAR(255) COLLATE database_default,
ownerRef_relation       NVARCHAR(157) COLLATE database_default,
ownerRef_targetOid      NVARCHAR(36) COLLATE database_default,
ownerRef_type           INT,
stageNumber             INT,
startTimestamp          DATETIME2,
state                   INT;

ALTER TABLE m_acc_cert_definition ADD
handlerUri NVARCHAR(255) COLLATE database_default,
lastCampaignClosedTimestamp DATETIME2,
lastCampaignStartedTimestamp DATETIME2,
ownerRef_relation NVARCHAR(157) COLLATE database_default,
ownerRef_targetOid NVARCHAR(36) COLLATE database_default,
ownerRef_type INT;

CREATE TABLE m_acc_cert_case (
  id                       INT                                   NOT NULL,
  owner_oid                NVARCHAR(36) COLLATE database_default NOT NULL,
  administrativeStatus     INT,
  archiveTimestamp         DATETIME2,
  disableReason            NVARCHAR(255) COLLATE database_default,
  disableTimestamp         DATETIME2,
  effectiveStatus          INT,
  enableTimestamp          DATETIME2,
  validFrom                DATETIME2,
  validTo                  DATETIME2,
  validityChangeTimestamp  DATETIME2,
  validityStatus           INT,
  currentStageNumber       INT,
  currentStageOutcome      INT,
  fullObject               VARBINARY(MAX),
  objectRef_relation       NVARCHAR(157) COLLATE database_default,
  objectRef_targetOid      NVARCHAR(36) COLLATE database_default,
  objectRef_type           INT,
  orgRef_relation          NVARCHAR(157) COLLATE database_default,
  orgRef_targetOid         NVARCHAR(36) COLLATE database_default,
  orgRef_type              INT,
  overallOutcome           INT,
  remediedTimestamp        DATETIME2,
  reviewDeadline           DATETIME2,
  reviewRequestedTimestamp DATETIME2,
  targetRef_relation       NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid      NVARCHAR(36) COLLATE database_default,
  targetRef_type           INT,
  tenantRef_relation       NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid      NVARCHAR(36) COLLATE database_default,
  tenantRef_type           INT,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_acc_cert_case_reference (
  owner_id        INT                                    NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  reference_type  INT                                    NOT NULL,
  relation        NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid       NVARCHAR(36) COLLATE database_default  NOT NULL,
  containerType   INT,
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
);

CREATE TABLE m_acc_cert_decision (
  id                    INT                                   NOT NULL,
  owner_id              INT                                   NOT NULL,
  owner_owner_oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  reviewerComment       NVARCHAR(255) COLLATE database_default,
  response              INT,
  reviewerRef_relation  NVARCHAR(157) COLLATE database_default,
  reviewerRef_targetOid NVARCHAR(36) COLLATE database_default,
  reviewerRef_type      INT,
  stageNumber           INT                                   NOT NULL,
  timestamp             DATETIME2,
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
