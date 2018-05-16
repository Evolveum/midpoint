DROP TABLE m_acc_cert_definition;
DROP TABLE m_acc_cert_wi_reference;
DROP TABLE m_acc_cert_wi;
DROP TABLE m_acc_cert_case;
DROP TABLE m_acc_cert_campaign;
DROP TABLE m_assignment_ext_boolean;
DROP TABLE m_assignment_ext_date;
DROP TABLE m_assignment_ext_long;
DROP TABLE m_assignment_ext_poly;
DROP TABLE m_assignment_ext_reference;
DROP TABLE m_assignment_ext_string;
DROP TABLE m_assignment_extension;
DROP TABLE m_assignment_policy_situation;
DROP TABLE m_assignment_reference;
DROP TABLE m_assignment;
-- Don't touch audit tables
-- DROP TABLE m_audit_delta;
-- DROP TABLE m_audit_item;
-- DROP TABLE m_audit_prop_value;
-- DROP TABLE m_audit_ref_value;
-- DROP TABLE m_audit_event;
DROP TABLE m_case;
DROP TABLE m_connector_host;
DROP TABLE m_connector_target_system;
DROP TABLE m_connector;
DROP TABLE m_exclusion;
DROP TABLE m_focus_photo;
DROP TABLE m_focus_policy_situation;
DROP TABLE m_form;
DROP TABLE m_function_library;
DROP TABLE m_generic_object;
DROP TABLE m_lookup_table_row;
DROP TABLE m_lookup_table;
DROP TABLE m_node;
DROP TABLE m_object_ext_boolean;
DROP TABLE m_object_ext_date;
DROP TABLE m_object_ext_long;
DROP TABLE m_object_ext_poly;
DROP TABLE m_object_ext_reference;
DROP TABLE m_object_ext_string;
DROP TABLE m_object_template;
DROP TABLE m_object_text_info;
DROP TABLE m_operation_execution;
DROP TABLE m_org_closure;
DROP TABLE m_org_org_type;
DROP TABLE m_org;
DROP TABLE m_reference;
DROP TABLE m_report;
DROP TABLE m_report_output;
DROP TABLE m_resource;
DROP TABLE m_role;
DROP TABLE m_security_policy;
DROP TABLE m_sequence;
DROP TABLE m_service_type;
DROP TABLE m_service;
DROP TABLE m_abstract_role;
DROP TABLE m_shadow;
DROP TABLE m_system_configuration;
DROP TABLE m_task_dependent;
DROP TABLE m_task;
DROP TABLE m_trigger;
DROP TABLE m_user_employee_type;
DROP TABLE m_user_organization;
DROP TABLE m_user_organizational_unit;
DROP TABLE m_value_policy;
DROP TABLE m_user;
DROP TABLE m_focus;
DROP TABLE m_object;

CREATE SEQUENCE m_audit_event_id_seq;
ALTER TABLE m_audit_event ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_event ALTER COLUMN id SET DEFAULT nextval('m_audit_event_id_seq');
ALTER SEQUENCE m_audit_event_id_seq OWNED BY m_audit_event.id;

CREATE SEQUENCE m_audit_prop_value_id_seq;
ALTER TABLE m_audit_prop_value ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_prop_value ALTER COLUMN id SET DEFAULT nextval('m_audit_prop_value_id_seq');
ALTER SEQUENCE m_audit_prop_value_id_seq OWNED BY m_audit_prop_value.id;

CREATE SEQUENCE m_audit_ref_value_id_seq;
ALTER TABLE m_audit_ref_value ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_ref_value ALTER COLUMN id SET DEFAULT nextval('m_audit_ref_value_id_seq');
ALTER SEQUENCE m_audit_ref_value_id_seq OWNED BY m_audit_ref_value.id;

SELECT setval('m_audit_event_id_seq', (SELECT max(id) + 1 FROM m_audit_event), FALSE);
SELECT setval('m_audit_prop_value_id_seq', (SELECT max(id) + 1 FROM m_audit_prop_value), FALSE);
SELECT setval('m_audit_ref_value_id_seq', (SELECT max(id) + 1 FROM m_audit_ref_value), FALSE);

ALTER TABLE m_audit_delta
  ALTER COLUMN delta TYPE BYTEA USING delta::bytea,
  ALTER COLUMN fullResult TYPE BYTEA USING fullResult::bytea;

ALTER TABLE m_audit_event
  ADD COLUMN targetOwnerType INT4;

ALTER TABLE m_audit_item
    ALTER COLUMN changedItemPath TYPE VARCHAR(255);

CREATE TABLE m_acc_cert_campaign (
  definitionRef_relation  VARCHAR(157),
  definitionRef_targetOid VARCHAR(36),
  definitionRef_type      INT4,
  endTimestamp            TIMESTAMP,
  handlerUri              VARCHAR(255),
  name_norm               VARCHAR(255),
  name_orig               VARCHAR(255),
  ownerRef_relation       VARCHAR(157),
  ownerRef_targetOid      VARCHAR(36),
  ownerRef_type           INT4,
  stageNumber             INT4,
  startTimestamp          TIMESTAMP,
  state                   INT4,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
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
  currentStageOutcome      VARCHAR(255),
  fullObject               BYTEA,
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INT4,
  orgRef_relation          VARCHAR(157),
  orgRef_targetOid         VARCHAR(36),
  orgRef_type              INT4,
  outcome                  VARCHAR(255),
  remediedTimestamp        TIMESTAMP,
  reviewDeadline           TIMESTAMP,
  reviewRequestedTimestamp TIMESTAMP,
  stageNumber              INT4,
  targetRef_relation       VARCHAR(157),
  targetRef_targetOid      VARCHAR(36),
  targetRef_type           INT4,
  tenantRef_relation       VARCHAR(157),
  tenantRef_targetOid      VARCHAR(36),
  tenantRef_type           INT4,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_acc_cert_definition (
  handlerUri                   VARCHAR(255),
  lastCampaignClosedTimestamp  TIMESTAMP,
  lastCampaignStartedTimestamp TIMESTAMP,
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  ownerRef_relation            VARCHAR(157),
  ownerRef_targetOid           VARCHAR(36),
  ownerRef_type                INT4,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
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
  PRIMARY KEY (owner_owner_oid, owner_id, id)
);
CREATE TABLE m_acc_cert_wi_reference (
  owner_id              INT4         NOT NULL,
  owner_owner_id        INT4         NOT NULL,
  owner_owner_owner_oid VARCHAR(36)  NOT NULL,
  relation              VARCHAR(157) NOT NULL,
  targetOid             VARCHAR(36)  NOT NULL,
  targetType            INT4,
  PRIMARY KEY (owner_owner_owner_oid, owner_owner_id, owner_id, relation, targetOid)
);
CREATE TABLE m_assignment (
  id                      INT4        NOT NULL,
  owner_oid               VARCHAR(36) NOT NULL,
  administrativeStatus    INT4,
  archiveTimestamp        TIMESTAMP,
  disableReason           VARCHAR(255),
  disableTimestamp        TIMESTAMP,
  effectiveStatus         INT4,
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          INT4,
  assignmentOwner         INT4,
  createChannel           VARCHAR(255),
  createTimestamp         TIMESTAMP,
  creatorRef_relation     VARCHAR(157),
  creatorRef_targetOid    VARCHAR(36),
  creatorRef_type         INT4,
  lifecycleState          VARCHAR(255),
  modifierRef_relation    VARCHAR(157),
  modifierRef_targetOid   VARCHAR(36),
  modifierRef_type        INT4,
  modifyChannel           VARCHAR(255),
  modifyTimestamp         TIMESTAMP,
  orderValue              INT4,
  orgRef_relation         VARCHAR(157),
  orgRef_targetOid        VARCHAR(36),
  orgRef_type             INT4,
  resourceRef_relation    VARCHAR(157),
  resourceRef_targetOid   VARCHAR(36),
  resourceRef_type        INT4,
  targetRef_relation      VARCHAR(157),
  targetRef_targetOid     VARCHAR(36),
  targetRef_type          INT4,
  tenantRef_relation      VARCHAR(157),
  tenantRef_targetOid     VARCHAR(36),
  tenantRef_type          INT4,
  extId                   INT4,
  extOid                  VARCHAR(36),
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_assignment_ext_boolean (
  item_id                      INT4        NOT NULL,
  anyContainer_owner_id        INT4        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  booleanValue                 BOOLEAN     NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)
);
CREATE TABLE m_assignment_ext_date (
  item_id                      INT4        NOT NULL,
  anyContainer_owner_id        INT4        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  dateValue                    TIMESTAMP   NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)
);
CREATE TABLE m_assignment_ext_long (
  item_id                      INT4        NOT NULL,
  anyContainer_owner_id        INT4        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  longValue                    INT8        NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)
);
CREATE TABLE m_assignment_ext_poly (
  item_id                      INT4         NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  norm                         VARCHAR(255),
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)
);
CREATE TABLE m_assignment_ext_reference (
  item_id                      INT4        NOT NULL,
  anyContainer_owner_id        INT4        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  targetoid                    VARCHAR(36) NOT NULL,
  relation                     VARCHAR(157),
  targetType                   INT4,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)
);
CREATE TABLE m_assignment_ext_string (
  item_id                      INT4         NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)
);
CREATE TABLE m_assignment_extension (
  owner_id        INT4        NOT NULL,
  owner_owner_oid VARCHAR(36) NOT NULL,
  booleansCount   INT2,
  datesCount      INT2,
  longsCount      INT2,
  polysCount      INT2,
  referencesCount INT2,
  stringsCount    INT2,
  PRIMARY KEY (owner_owner_oid, owner_id)
);
CREATE TABLE m_assignment_policy_situation (
  assignment_id   INT4        NOT NULL,
  assignment_oid  VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
);
CREATE TABLE m_assignment_reference (
  owner_id        INT4         NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INT4         NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INT4,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, relation, targetOid)
);
CREATE TABLE m_case_wi (
  id                            INT4        NOT NULL,
  owner_oid                     VARCHAR(36) NOT NULL,
  closeTimestamp                TIMESTAMP,
  deadline                      TIMESTAMP,
  originalAssigneeRef_relation  VARCHAR(157),
  originalAssigneeRef_targetOid VARCHAR(36),
  originalAssigneeRef_type      INT4,
  outcome                       VARCHAR(255),
  performerRef_relation         VARCHAR(157),
  performerRef_targetOid        VARCHAR(36),
  performerRef_type             INT4,
  stageNumber                   INT4,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_case_wi_reference (
  owner_id        INT4         NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INT4,
  PRIMARY KEY (owner_owner_oid, owner_id, targetOid, relation)
);
CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
);
CREATE TABLE m_ext_item (
  id       SERIAL NOT NULL,
  kind     INT4,
  itemName VARCHAR(157),
  itemType VARCHAR(157),
  PRIMARY KEY (id)
);
CREATE TABLE m_focus_photo (
  owner_oid VARCHAR(36) NOT NULL,
  photo     BYTEA,
  PRIMARY KEY (owner_oid)
);
CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
);
CREATE TABLE m_object (
  oid                   VARCHAR(36) NOT NULL,
  booleansCount         INT2,
  createChannel         VARCHAR(255),
  createTimestamp       TIMESTAMP,
  creatorRef_relation   VARCHAR(157),
  creatorRef_targetOid  VARCHAR(36),
  creatorRef_type       INT4,
  datesCount            INT2,
  fullObject            BYTEA,
  lifecycleState        VARCHAR(255),
  longsCount            INT2,
  modifierRef_relation  VARCHAR(157),
  modifierRef_targetOid VARCHAR(36),
  modifierRef_type      INT4,
  modifyChannel         VARCHAR(255),
  modifyTimestamp       TIMESTAMP,
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  objectTypeClass       INT4,
  polysCount            INT2,
  referencesCount       INT2,
  stringsCount          INT2,
  tenantRef_relation    VARCHAR(157),
  tenantRef_targetOid   VARCHAR(36),
  tenantRef_type        INT4,
  version               INT4        NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_object_ext_boolean (
  item_id      INT4        NOT NULL,
  owner_oid    VARCHAR(36) NOT NULL,
  ownerType    INT4        NOT NULL,
  booleanValue BOOLEAN     NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, booleanValue)
);
CREATE TABLE m_object_ext_date (
  item_id   INT4        NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INT4        NOT NULL,
  dateValue TIMESTAMP   NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, dateValue)
);
CREATE TABLE m_object_ext_long (
  item_id   INT4        NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INT4        NOT NULL,
  longValue INT8        NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, longValue)
);
CREATE TABLE m_object_ext_poly (
  item_id   INT4         NOT NULL,
  owner_oid VARCHAR(36)  NOT NULL,
  ownerType INT4         NOT NULL,
  orig      VARCHAR(255) NOT NULL,
  norm      VARCHAR(255),
  PRIMARY KEY (owner_oid, ownerType, item_id, orig)
);
CREATE TABLE m_object_ext_reference (
  item_id    INT4        NOT NULL,
  owner_oid  VARCHAR(36) NOT NULL,
  ownerType  INT4        NOT NULL,
  targetoid  VARCHAR(36) NOT NULL,
  relation   VARCHAR(157),
  targetType INT4,
  PRIMARY KEY (owner_oid, ownerType, item_id, targetoid)
);
CREATE TABLE m_object_ext_string (
  item_id     INT4         NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INT4         NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, stringValue)
);
CREATE TABLE m_object_subtype (
  object_oid VARCHAR(36) NOT NULL,
  subType    VARCHAR(255)
);
CREATE TABLE m_object_text_info (
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
);
CREATE TABLE m_operation_execution (
  id                     INT4        NOT NULL,
  owner_oid              VARCHAR(36) NOT NULL,
  initiatorRef_relation  VARCHAR(157),
  initiatorRef_targetOid VARCHAR(36),
  initiatorRef_type      INT4,
  status                 INT4,
  taskRef_relation       VARCHAR(157),
  taskRef_targetOid      VARCHAR(36),
  taskRef_type           INT4,
  timestampValue         TIMESTAMP,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_org_closure (
  ancestor_oid   VARCHAR(36) NOT NULL,
  descendant_oid VARCHAR(36) NOT NULL,
  val            INT4,
  PRIMARY KEY (ancestor_oid, descendant_oid)
);
CREATE TABLE m_org_org_type (
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
);
CREATE TABLE m_reference (
  owner_oid      VARCHAR(36)  NOT NULL,
  reference_type INT4         NOT NULL,
  relation       VARCHAR(157) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  targetType     INT4,
  PRIMARY KEY (owner_oid, reference_type, relation, targetOid)
);
CREATE TABLE m_service_type (
  service_oid VARCHAR(36) NOT NULL,
  serviceType VARCHAR(255)
);
CREATE TABLE m_shadow (
  attemptNumber                INT4,
  dead                         BOOLEAN,
  exist                        BOOLEAN,
  failedOperationType          INT4,
  fullSynchronizationTimestamp TIMESTAMP,
  intent                       VARCHAR(255),
  kind                         INT4,
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  objectClass                  VARCHAR(157),
  pendingOperationCount        INT4,
  resourceRef_relation         VARCHAR(157),
  resourceRef_targetOid        VARCHAR(36),
  resourceRef_type             INT4,
  status                       INT4,
  synchronizationSituation     INT4,
  synchronizationTimestamp     TIMESTAMP,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_task (
  binding                  INT4,
  canRunOnNode             VARCHAR(255),
  category                 VARCHAR(255),
  completionTimestamp      TIMESTAMP,
  executionStatus          INT4,
  fullResult               BYTEA,
  handlerUri               VARCHAR(255),
  lastRunFinishTimestamp   TIMESTAMP,
  lastRunStartTimestamp    TIMESTAMP,
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  node                     VARCHAR(255),
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INT4,
  ownerRef_relation        VARCHAR(157),
  ownerRef_targetOid       VARCHAR(36),
  ownerRef_type            INT4,
  parent                   VARCHAR(255),
  recurrence               INT4,
  status                   INT4,
  taskIdentifier           VARCHAR(255),
  threadStopAction         INT4,
  waitingReason            INT4,
  wfEndTimestamp           TIMESTAMP,
  wfObjectRef_relation     VARCHAR(157),
  wfObjectRef_targetOid    VARCHAR(36),
  wfObjectRef_type         INT4,
  wfProcessInstanceId      VARCHAR(255),
  wfRequesterRef_relation  VARCHAR(157),
  wfRequesterRef_targetOid VARCHAR(36),
  wfRequesterRef_type      INT4,
  wfStartTimestamp         TIMESTAMP,
  wfTargetRef_relation     VARCHAR(157),
  wfTargetRef_targetOid    VARCHAR(36),
  wfTargetRef_type         INT4,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_task_dependent (
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
);
CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
);
CREATE TABLE m_user_organization (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);
CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);
CREATE TABLE m_abstract_role (
  approvalProcess    VARCHAR(255),
  autoassign_enabled BOOLEAN,
  displayName_norm   VARCHAR(255),
  displayName_orig   VARCHAR(255),
  identifier         VARCHAR(255),
  ownerRef_relation  VARCHAR(157),
  ownerRef_targetOid VARCHAR(36),
  ownerRef_type      INT4,
  requestable        BOOLEAN,
  riskLevel          VARCHAR(255),
  oid                VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_case (
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  objectRef_relation  VARCHAR(157),
  objectRef_targetOid VARCHAR(36),
  objectRef_type      INT4,
  state               VARCHAR(255),
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector (
  connectorBundle            VARCHAR(255),
  connectorHostRef_relation  VARCHAR(157),
  connectorHostRef_targetOid VARCHAR(36),
  connectorHostRef_type      INT4,
  connectorType              VARCHAR(255),
  connectorVersion           VARCHAR(255),
  framework                  VARCHAR(255),
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector_host (
  hostname  VARCHAR(255),
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  port      VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_focus (
  administrativeStatus    INT4,
  archiveTimestamp        TIMESTAMP,
  disableReason           VARCHAR(255),
  disableTimestamp        TIMESTAMP,
  effectiveStatus         INT4,
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          INT4,
  costCenter              VARCHAR(255),
  emailAddress            VARCHAR(255),
  hasPhoto                BOOLEAN DEFAULT FALSE NOT NULL,
  locale                  VARCHAR(255),
  locality_norm           VARCHAR(255),
  locality_orig           VARCHAR(255),
  preferredLanguage       VARCHAR(255),
  telephoneNumber         VARCHAR(255),
  timezone                VARCHAR(255),
  oid                     VARCHAR(36)           NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_function_library (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_lookup_table (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_lookup_table_row (
  id                  INT4        NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  row_key             VARCHAR(255),
  label_norm          VARCHAR(255),
  label_orig          VARCHAR(255),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR(255),
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_node (
  name_norm      VARCHAR(255),
  name_orig      VARCHAR(255),
  nodeIdentifier VARCHAR(255),
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INT4,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_org (
  displayOrder INT4,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  tenant       BOOLEAN,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report (
  export              INT4,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  orientation         INT4,
  parent              BOOLEAN,
  useHibernateSession BOOLEAN,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report_output (
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  reportRef_relation  VARCHAR(157),
  reportRef_targetOid VARCHAR(36),
  reportRef_type      INT4,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_resource (
  administrativeState        INT4,
  connectorRef_relation      VARCHAR(157),
  connectorRef_targetOid     VARCHAR(36),
  connectorRef_type          INT4,
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  o16_lastAvailabilityStatus INT4,
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_security_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_sequence (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_service (
  displayOrder INT4,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_system_configuration (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_trigger (
  id             INT4        NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue TIMESTAMP,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_user (
  additionalName_norm  VARCHAR(255),
  additionalName_orig  VARCHAR(255),
  employeeNumber       VARCHAR(255),
  familyName_norm      VARCHAR(255),
  familyName_orig      VARCHAR(255),
  fullName_norm        VARCHAR(255),
  fullName_orig        VARCHAR(255),
  givenName_norm       VARCHAR(255),
  givenName_orig       VARCHAR(255),
  honorificPrefix_norm VARCHAR(255),
  honorificPrefix_orig VARCHAR(255),
  honorificSuffix_norm VARCHAR(255),
  honorificSuffix_orig VARCHAR(255),
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  nickName_norm        VARCHAR(255),
  nickName_orig        VARCHAR(255),
  title_norm           VARCHAR(255),
  title_orig           VARCHAR(255),
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE INDEX iCertCampaignNameOrig
  ON m_acc_cert_campaign (name_orig);
ALTER TABLE IF EXISTS m_acc_cert_campaign
  ADD CONSTRAINT uc_acc_cert_campaign_name UNIQUE (name_norm);
CREATE INDEX iCaseObjectRefTargetOid
  ON m_acc_cert_case (objectRef_targetOid);
CREATE INDEX iCaseTargetRefTargetOid
  ON m_acc_cert_case (targetRef_targetOid);
CREATE INDEX iCaseTenantRefTargetOid
  ON m_acc_cert_case (tenantRef_targetOid);
CREATE INDEX iCaseOrgRefTargetOid
  ON m_acc_cert_case (orgRef_targetOid);
CREATE INDEX iCertDefinitionNameOrig
  ON m_acc_cert_definition (name_orig);
ALTER TABLE IF EXISTS m_acc_cert_definition
  ADD CONSTRAINT uc_acc_cert_definition_name UNIQUE (name_norm);
CREATE INDEX iCertWorkItemRefTargetOid
  ON m_acc_cert_wi_reference (targetOid);
CREATE INDEX iAssignmentAdministrative
  ON m_assignment (administrativeStatus);
CREATE INDEX iAssignmentEffective
  ON m_assignment (effectiveStatus);
CREATE INDEX iAssignmentValidFrom
  ON m_assignment (validFrom);
CREATE INDEX iAssignmentValidTo
  ON m_assignment (validTo);
CREATE INDEX iTargetRefTargetOid
  ON m_assignment (targetRef_targetOid);
CREATE INDEX iTenantRefTargetOid
  ON m_assignment (tenantRef_targetOid);
CREATE INDEX iOrgRefTargetOid
  ON m_assignment (orgRef_targetOid);
CREATE INDEX iResourceRefTargetOid
  ON m_assignment (resourceRef_targetOid);
CREATE INDEX iAExtensionBoolean
  ON m_assignment_ext_boolean (booleanValue);
CREATE INDEX iAExtensionDate
  ON m_assignment_ext_date (dateValue);
CREATE INDEX iAExtensionLong
  ON m_assignment_ext_long (longValue);
CREATE INDEX iAExtensionPolyString
  ON m_assignment_ext_poly (orig);
CREATE INDEX iAExtensionReference
  ON m_assignment_ext_reference (targetoid);
CREATE INDEX iAExtensionString
  ON m_assignment_ext_string (stringValue);
CREATE INDEX iAssignmentReferenceTargetOid
  ON m_assignment_reference (targetOid);
CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);
CREATE INDEX iObjectNameOrig
  ON m_object (name_orig);
CREATE INDEX iObjectNameNorm
  ON m_object (name_norm);
CREATE INDEX iObjectTypeClass
  ON m_object (objectTypeClass);
CREATE INDEX iObjectCreateTimestamp
  ON m_object (createTimestamp);
CREATE INDEX iObjectLifecycleState
  ON m_object (lifecycleState);
CREATE INDEX iExtensionBoolean
  ON m_object_ext_boolean (booleanValue);
CREATE INDEX iExtensionDate
  ON m_object_ext_date (dateValue);
CREATE INDEX iExtensionLong
  ON m_object_ext_long (longValue);
CREATE INDEX iExtensionPolyString
  ON m_object_ext_poly (orig);
CREATE INDEX iExtensionReference
  ON m_object_ext_reference (targetoid);
CREATE INDEX iExtensionString
  ON m_object_ext_string (stringValue);
CREATE INDEX iOpExecTaskOid
  ON m_operation_execution (taskRef_targetOid);
CREATE INDEX iOpExecInitiatorOid
  ON m_operation_execution (initiatorRef_targetOid);
CREATE INDEX iOpExecStatus
  ON m_operation_execution (status);
CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);
CREATE INDEX iAncestor
  ON m_org_closure (ancestor_oid);
CREATE INDEX iDescendant
  ON m_org_closure (descendant_oid);
CREATE INDEX iDescendantAncestor
  ON m_org_closure (descendant_oid, ancestor_oid);
CREATE INDEX iReferenceTargetTypeRelation
  ON m_reference (targetOid, reference_type, relation);
CREATE INDEX iShadowResourceRef
  ON m_shadow (resourceRef_targetOid);
CREATE INDEX iShadowDead
  ON m_shadow (dead);
CREATE INDEX iShadowKind
  ON m_shadow (kind);
CREATE INDEX iShadowIntent
  ON m_shadow (intent);
CREATE INDEX iShadowObjectClass
  ON m_shadow (objectClass);
CREATE INDEX iShadowFailedOperationType
  ON m_shadow (failedOperationType);
CREATE INDEX iShadowSyncSituation
  ON m_shadow (synchronizationSituation);
CREATE INDEX iShadowPendingOperationCount
  ON m_shadow (pendingOperationCount);
CREATE INDEX iShadowNameOrig
  ON m_shadow (name_orig);
CREATE INDEX iShadowNameNorm
  ON m_shadow (name_norm);
CREATE INDEX iParent
  ON m_task (parent);
CREATE INDEX iTaskWfProcessInstanceId
  ON m_task (wfProcessInstanceId);
CREATE INDEX iTaskWfStartTimestamp
  ON m_task (wfStartTimestamp);
CREATE INDEX iTaskWfEndTimestamp
  ON m_task (wfEndTimestamp);
CREATE INDEX iTaskWfRequesterOid
  ON m_task (wfRequesterRef_targetOid);
CREATE INDEX iTaskWfObjectOid
  ON m_task (wfObjectRef_targetOid);
CREATE INDEX iTaskWfTargetOid
  ON m_task (wfTargetRef_targetOid);
CREATE INDEX iTaskNameOrig
  ON m_task (name_orig);
ALTER TABLE IF EXISTS m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);
CREATE INDEX iAbstractRoleIdentifier
  ON m_abstract_role (identifier);
CREATE INDEX iRequestable
  ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);
CREATE INDEX iCaseNameOrig
  ON m_case (name_orig);
ALTER TABLE IF EXISTS m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);
CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig);
CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm);
CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig);
ALTER TABLE IF EXISTS m_connector_host
  ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm);
CREATE INDEX iFocusAdministrative
  ON m_focus (administrativeStatus);
CREATE INDEX iFocusEffective
  ON m_focus (effectiveStatus);
CREATE INDEX iLocality
  ON m_focus (locality_orig);
CREATE INDEX iFocusValidFrom
  ON m_focus (validFrom);
CREATE INDEX iFocusValidTo
  ON m_focus (validTo);
CREATE INDEX iFormNameOrig
  ON m_form (name_orig);
ALTER TABLE IF EXISTS m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);
CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig);
ALTER TABLE IF EXISTS m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);
CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig);
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm);
CREATE INDEX iLookupTableNameOrig
  ON m_lookup_table (name_orig);
ALTER TABLE IF EXISTS m_lookup_table
  ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);
ALTER TABLE IF EXISTS m_lookup_table_row
  ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);
CREATE INDEX iNodeNameOrig
  ON m_node (name_orig);
ALTER TABLE IF EXISTS m_node
  ADD CONSTRAINT uc_node_name UNIQUE (name_norm);
CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig);
ALTER TABLE IF EXISTS m_object_template
  ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm);
CREATE INDEX iDisplayOrder
  ON m_org (displayOrder);
CREATE INDEX iOrgNameOrig
  ON m_org (name_orig);
ALTER TABLE IF EXISTS m_org
  ADD CONSTRAINT uc_org_name UNIQUE (name_norm);
CREATE INDEX iReportParent
  ON m_report (parent);
CREATE INDEX iReportNameOrig
  ON m_report (name_orig);
ALTER TABLE IF EXISTS m_report
  ADD CONSTRAINT uc_report_name UNIQUE (name_norm);
CREATE INDEX iReportOutputNameOrig
  ON m_report_output (name_orig);
CREATE INDEX iReportOutputNameNorm
  ON m_report_output (name_norm);
CREATE INDEX iResourceNameOrig
  ON m_resource (name_orig);
ALTER TABLE IF EXISTS m_resource
  ADD CONSTRAINT uc_resource_name UNIQUE (name_norm);
CREATE INDEX iRoleNameOrig
  ON m_role (name_orig);
ALTER TABLE IF EXISTS m_role
  ADD CONSTRAINT uc_role_name UNIQUE (name_norm);
CREATE INDEX iSecurityPolicyNameOrig
  ON m_security_policy (name_orig);
ALTER TABLE IF EXISTS m_security_policy
  ADD CONSTRAINT uc_security_policy_name UNIQUE (name_norm);
CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig);
ALTER TABLE IF EXISTS m_sequence
  ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);
CREATE INDEX iServiceNameOrig
  ON m_service (name_orig);
CREATE INDEX iServiceNameNorm
  ON m_service (name_norm);
CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig);
ALTER TABLE IF EXISTS m_system_configuration
  ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm);
CREATE INDEX iTriggerTimestamp
  ON m_trigger (timestampValue);
CREATE INDEX iFullName
  ON m_user (fullName_orig);
CREATE INDEX iFamilyName
  ON m_user (familyName_orig);
CREATE INDEX iGivenName
  ON m_user (givenName_orig);
CREATE INDEX iEmployeeNumber
  ON m_user (employeeNumber);
CREATE INDEX iUserNameOrig
  ON m_user (name_orig);
ALTER TABLE IF EXISTS m_user
  ADD CONSTRAINT uc_user_name UNIQUE (name_norm);
CREATE INDEX iValuePolicyNameOrig
  ON m_value_policy (name_orig);
ALTER TABLE IF EXISTS m_value_policy
  ADD CONSTRAINT uc_value_policy_name UNIQUE (name_norm);
ALTER TABLE IF EXISTS m_acc_cert_campaign
  ADD CONSTRAINT fk_acc_cert_campaign FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner FOREIGN KEY (owner_oid) REFERENCES m_acc_cert_campaign;
ALTER TABLE IF EXISTS m_acc_cert_definition
  ADD CONSTRAINT fk_acc_cert_definition FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_acc_cert_case;
ALTER TABLE IF EXISTS m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner FOREIGN KEY (owner_owner_owner_oid, owner_owner_id, owner_id) REFERENCES m_acc_cert_wi;
ALTER TABLE IF EXISTS m_assignment
  ADD CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_boolean_reference FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation FOREIGN KEY (assignment_oid, assignment_id) REFERENCES m_assignment;
ALTER TABLE IF EXISTS m_assignment_reference
  ADD CONSTRAINT fk_assignment_reference FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_assignment;
ALTER TABLE IF EXISTS m_case_wi
  ADD CONSTRAINT fk_case_wi_owner FOREIGN KEY (owner_oid) REFERENCES m_case;
ALTER TABLE IF EXISTS m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi;
ALTER TABLE IF EXISTS m_connector_target_system
  ADD CONSTRAINT fk_connector_target_system FOREIGN KEY (connector_oid) REFERENCES m_connector;
ALTER TABLE IF EXISTS m_focus_photo
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (owner_oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation FOREIGN KEY (focus_oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_long
  ADD CONSTRAINT fk_object_ext_long FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_long
  ADD CONSTRAINT fk_o_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_string
  ADD CONSTRAINT fk_object_ext_string FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_string
  ADD CONSTRAINT fk_o_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_subtype
  ADD CONSTRAINT fk_object_subtype FOREIGN KEY (object_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_closure
  ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_closure
  ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_org_type
  ADD CONSTRAINT fk_org_org_type FOREIGN KEY (org_oid) REFERENCES m_org;
ALTER TABLE IF EXISTS m_reference
  ADD CONSTRAINT fk_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_service_type
  ADD CONSTRAINT fk_service_type FOREIGN KEY (service_oid) REFERENCES m_service;
ALTER TABLE IF EXISTS m_shadow
  ADD CONSTRAINT fk_shadow FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_task
  ADD CONSTRAINT fk_task FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_task_dependent
  ADD CONSTRAINT fk_task_dependent FOREIGN KEY (task_oid) REFERENCES m_task;
ALTER TABLE IF EXISTS m_user_employee_type
  ADD CONSTRAINT fk_user_employee_type FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_abstract_role
  ADD CONSTRAINT fk_abstract_role FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_case
  ADD CONSTRAINT fk_case FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_connector
  ADD CONSTRAINT fk_connector FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_connector_host
  ADD CONSTRAINT fk_connector_host FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_focus
  ADD CONSTRAINT fk_focus FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_form
  ADD CONSTRAINT fk_form FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_lookup_table
  ADD CONSTRAINT fk_lookup_table FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_lookup_table_row
  ADD CONSTRAINT fk_lookup_table_owner FOREIGN KEY (owner_oid) REFERENCES m_lookup_table;
ALTER TABLE IF EXISTS m_node
  ADD CONSTRAINT fk_node FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_template
  ADD CONSTRAINT fk_object_template FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org
  ADD CONSTRAINT fk_org FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_report
  ADD CONSTRAINT fk_report FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_report_output
  ADD CONSTRAINT fk_report_output FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_resource
  ADD CONSTRAINT fk_resource FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_role
  ADD CONSTRAINT fk_role FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_security_policy
  ADD CONSTRAINT fk_security_policy FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_sequence
  ADD CONSTRAINT fk_sequence FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_service
  ADD CONSTRAINT fk_service FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_system_configuration
  ADD CONSTRAINT fk_system_configuration FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_trigger
  ADD CONSTRAINT fk_trigger_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_user
  ADD CONSTRAINT fk_user FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_value_policy
  ADD CONSTRAINT fk_value_policy FOREIGN KEY (oid) REFERENCES m_object;
