CREATE TABLE m_service (
  displayOrder  INT4,
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

ALTER TABLE m_task
 ADD wfEndTimestamp           TIMESTAMP,
 ADD wfObjectRef_relation     VARCHAR(157),
 ADD wfObjectRef_targetOid    VARCHAR(36),
 ADD wfObjectRef_type         INT4,
 ADD wfProcessInstanceId      VARCHAR(255),
 ADD wfRequesterRef_relation  VARCHAR(157),
 ADD wfRequesterRef_targetOid VARCHAR(36),
 ADD wfRequesterRef_type      INT4,
 ADD wfStartTimestamp         TIMESTAMP,
 ADD wfTargetRef_relation     VARCHAR(157),
 ADD wfTargetRef_targetOid    VARCHAR(36),
 ADD wfTargetRef_type         INT4;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

ALTER TABLE m_abstract_role
  ADD ownerRef_relation VARCHAR(157),
  ADD ownerRef_targetOid VARCHAR(36),
  ADD ownerRef_type INT4;

ALTER TABLE m_acc_cert_campaign
ADD endTimestamp TIMESTAMP,
ADD handlerUri VARCHAR(255),
ADD ownerRef_relation VARCHAR(157),
ADD ownerRef_targetOid VARCHAR(36),
ADD ownerRef_type INT4,
ADD stageNumber INT4,
ADD startTimestamp TIMESTAMP,
ADD state INT4;

ALTER TABLE m_acc_cert_definition
ADD handlerUri VARCHAR(255),
ADD lastCampaignClosedTimestamp TIMESTAMP,
ADD lastCampaignStartedTimestamp TIMESTAMP,
ADD ownerRef_relation VARCHAR(157),
ADD ownerRef_targetOid VARCHAR(36),
ADD ownerRef_type INT4;

CREATE TABLE m_acc_cert_case (
  id                       INT4        NOT NULL,
  owner_oid                VARCHAR(36) NOT NULL,
  administrativeStatus     INT4,
  archiveTimestamp         TIMESTAMP,
  disableReason            VARCHAR(255),
  disableTimestamp         TIMESTAMP,
  effectiveStatus          INT4,
  enableTimestamp          TIMESTAMP,
  validFrom                TIMESTAMP,
  validTo                  TIMESTAMP,
  validityChangeTimestamp  TIMESTAMP,
  validityStatus           INT4,
  currentStageNumber       INT4,
  currentStageOutcome      INT4,
  fullObject               BYTEA,
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INT4,
  orgRef_relation          VARCHAR(157),
  orgRef_targetOid         VARCHAR(36),
  orgRef_type              INT4,
  overallOutcome           INT4,
  remediedTimestamp        TIMESTAMP,
  reviewDeadline           TIMESTAMP,
  reviewRequestedTimestamp TIMESTAMP,
  targetRef_relation       VARCHAR(157),
  targetRef_targetOid      VARCHAR(36),
  targetRef_type           INT4,
  tenantRef_relation       VARCHAR(157),
  tenantRef_targetOid      VARCHAR(36),
  tenantRef_type           INT4,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_acc_cert_case_reference (
  owner_id        INT4         NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INT4         NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  containerType   INT4,
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
);

CREATE TABLE m_acc_cert_decision (
  id                    INT4        NOT NULL,
  owner_id              INT4        NOT NULL,
  owner_owner_oid       VARCHAR(36) NOT NULL,
  reviewerComment       VARCHAR(255),
  response              INT4,
  reviewerRef_relation  VARCHAR(157),
  reviewerRef_targetOid VARCHAR(36),
  reviewerRef_type      INT4,
  stageNumber           INT4        NOT NULL,
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

ALTER TABLE m_abstract_role
  ADD displayName_norm   VARCHAR(255),
  ADD displayName_orig   VARCHAR(255),
  ADD identifier         VARCHAR(255),
  ADD riskLevel          VARCHAR(255);

ALTER TABLE m_org
  DROP COLUMN displayName_norm,
  DROP COLUMN displayName_orig,
  DROP COLUMN identifier;

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);

create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_PROCDEF_INFO (
	ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
);

create index ACT_IDX_PROCDEF_INFO_JSON on ACT_PROCDEF_INFO(INFO_JSON_ID_);
alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_JSON_BA
    foreign key (INFO_JSON_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_PROCDEF_INFO_PROC on ACT_PROCDEF_INFO(PROC_DEF_ID_);
alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

update ACT_GE_PROPERTY set VALUE_ = '5.20.0.1' where NAME_ = 'schema.version';
