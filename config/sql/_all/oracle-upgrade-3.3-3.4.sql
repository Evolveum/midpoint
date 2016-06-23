CREATE TABLE m_service (
  displayOrder  NUMBER(10, 0),
  locality_norm VARCHAR2(255 CHAR),
  locality_orig VARCHAR2(255 CHAR),
  name_norm     VARCHAR2(255 CHAR),
  name_orig     VARCHAR2(255 CHAR),
  oid           VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_service_type (
  service_oid VARCHAR2(36 CHAR) NOT NULL,
  serviceType VARCHAR2(255 CHAR)
) INITRANS 30;

ALTER TABLE m_service
  ADD CONSTRAINT fk_service
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type
FOREIGN KEY (service_oid)
REFERENCES m_service;

ALTER TABLE m_task ADD (
  wfEndTimestamp           TIMESTAMP,
  wfObjectRef_relation     VARCHAR2(157 CHAR),
  wfObjectRef_targetOid    VARCHAR2(36 CHAR),
  wfObjectRef_type         NUMBER(10, 0),
  wfProcessInstanceId      VARCHAR2(255 CHAR),
  wfRequesterRef_relation  VARCHAR2(157 CHAR),
  wfRequesterRef_targetOid VARCHAR2(36 CHAR),
  wfRequesterRef_type      NUMBER(10, 0),
  wfStartTimestamp         TIMESTAMP,
  wfTargetRef_relation     VARCHAR2(157 CHAR),
  wfTargetRef_targetOid    VARCHAR2(36 CHAR),
  wfTargetRef_type         NUMBER(10, 0)
);

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId) INITRANS 30;

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp) INITRANS 30;

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp) INITRANS 30;

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid) INITRANS 30;

ALTER TABLE m_abstract_role ADD (
  ownerRef_relation  VARCHAR2(157 CHAR),
  ownerRef_targetOid VARCHAR2(36 CHAR),
  ownerRef_type      NUMBER(10, 0)
);

ALTER TABLE m_acc_cert_campaign ADD (
endTimestamp TIMESTAMP,
handlerUri VARCHAR2(255 CHAR),
ownerRef_relation VARCHAR2(157 CHAR),
ownerRef_targetOid VARCHAR2(36 CHAR),
ownerRef_type NUMBER(10, 0),
stageNumber NUMBER(10, 0),
startTimestamp TIMESTAMP,
state NUMBER(10, 0));

ALTER TABLE m_acc_cert_definition ADD (
handlerUri VARCHAR2(255 CHAR),
lastCampaignClosedTimestamp TIMESTAMP,
lastCampaignStartedTimestamp TIMESTAMP,
ownerRef_relation VARCHAR2(157 CHAR),
ownerRef_targetOid VARCHAR2(36 CHAR),
ownerRef_type NUMBER(10, 0));

CREATE TABLE m_acc_cert_case (
  id                       NUMBER(10, 0)     NOT NULL,
  owner_oid                VARCHAR2(36 CHAR) NOT NULL,
  administrativeStatus     NUMBER(10, 0),
  archiveTimestamp         TIMESTAMP,
  disableReason            VARCHAR2(255 CHAR),
  disableTimestamp         TIMESTAMP,
  effectiveStatus          NUMBER(10, 0),
  enableTimestamp          TIMESTAMP,
  validFrom                TIMESTAMP,
  validTo                  TIMESTAMP,
  validityChangeTimestamp  TIMESTAMP,
  validityStatus           NUMBER(10, 0),
  currentStageNumber       NUMBER(10, 0),
  currentStageOutcome      NUMBER(10, 0),
  fullObject               BLOB,
  objectRef_relation       VARCHAR2(157 CHAR),
  objectRef_targetOid      VARCHAR2(36 CHAR),
  objectRef_type           NUMBER(10, 0),
  orgRef_relation          VARCHAR2(157 CHAR),
  orgRef_targetOid         VARCHAR2(36 CHAR),
  orgRef_type              NUMBER(10, 0),
  overallOutcome           NUMBER(10, 0),
  remediedTimestamp        TIMESTAMP,
  reviewDeadline           TIMESTAMP,
  reviewRequestedTimestamp TIMESTAMP,
  targetRef_relation       VARCHAR2(157 CHAR),
  targetRef_targetOid      VARCHAR2(36 CHAR),
  targetRef_type           NUMBER(10, 0),
  tenantRef_relation       VARCHAR2(157 CHAR),
  tenantRef_targetOid      VARCHAR2(36 CHAR),
  tenantRef_type           NUMBER(10, 0),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_case_reference (
  owner_id        NUMBER(10, 0)      NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  reference_type  NUMBER(10, 0)      NOT NULL,
  relation        VARCHAR2(157 CHAR) NOT NULL,
  targetOid       VARCHAR2(36 CHAR)  NOT NULL,
  containerType   NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
) INITRANS 30;

CREATE TABLE m_acc_cert_decision (
  id                    NUMBER(10, 0)     NOT NULL,
  owner_id              NUMBER(10, 0)     NOT NULL,
  owner_owner_oid       VARCHAR2(36 CHAR) NOT NULL,
  reviewerComment       VARCHAR2(255 CHAR),
  response              NUMBER(10, 0),
  reviewerRef_relation  VARCHAR2(157 CHAR),
  reviewerRef_targetOid VARCHAR2(36 CHAR),
  reviewerRef_type      NUMBER(10, 0),
  stageNumber           NUMBER(10, 0)     NOT NULL,
  timestamp             TIMESTAMP,
  PRIMARY KEY (id, owner_id, owner_owner_oid)
) INITRANS 30;

CREATE INDEX iCaseObjectRefTargetOid ON m_acc_cert_case (objectRef_targetOid) INITRANS 30;

CREATE INDEX iCaseTargetRefTargetOid ON m_acc_cert_case (targetRef_targetOid) INITRANS 30;

CREATE INDEX iCaseTenantRefTargetOid ON m_acc_cert_case (tenantRef_targetOid) INITRANS 30;

CREATE INDEX iCaseOrgRefTargetOid ON m_acc_cert_case (orgRef_targetOid) INITRANS 30;

CREATE INDEX iCaseReferenceTargetOid ON m_acc_cert_case_reference (targetOid) INITRANS 30;

ALTER TABLE m_acc_cert_decision
ADD CONSTRAINT uc_case_stage_reviewer UNIQUE (owner_owner_oid, owner_id, stageNumber, reviewerRef_targetOid) INITRANS 30;

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
ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key) INITRANS 30;

ALTER TABLE m_abstract_role ADD (
  displayName_norm   VARCHAR2(255 CHAR),
  displayName_orig   VARCHAR2(255 CHAR),
  identifier         VARCHAR2(255 CHAR),
  riskLevel          VARCHAR2(255 CHAR));

ALTER TABLE m_org DROP (displayName_norm, displayName_orig, identifier);

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier) INITRANS 30;

create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_PROCDEF_INFO (
	ID_ NVARCHAR2(64) not null,
    PROC_DEF_ID_ NVARCHAR2(64) not null,
    REV_ integer,
    INFO_JSON_ID_ NVARCHAR2(64),
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

commit;
