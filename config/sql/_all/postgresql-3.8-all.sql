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
CREATE TABLE m_audit_delta (
  checksum          VARCHAR(32) NOT NULL,
  record_id         INT8        NOT NULL,
  delta             BYTEA,
  deltaOid          VARCHAR(36),
  deltaType         INT4,
  fullResult        BYTEA,
  objectName_norm   VARCHAR(255),
  objectName_orig   VARCHAR(255),
  resourceName_norm VARCHAR(255),
  resourceName_orig VARCHAR(255),
  resourceOid       VARCHAR(36),
  status            INT4,
  PRIMARY KEY (record_id, checksum)
);
CREATE TABLE m_audit_event (
  id                BIGSERIAL NOT NULL,
  attorneyName      VARCHAR(255),
  attorneyOid       VARCHAR(36),
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INT4,
  eventType         INT4,
  hostIdentifier    VARCHAR(255),
  initiatorName     VARCHAR(255),
  initiatorOid      VARCHAR(36),
  initiatorType     INT4,
  message           VARCHAR(1024),
  nodeIdentifier    VARCHAR(255),
  outcome           INT4,
  parameter         VARCHAR(255),
  remoteHostAddress VARCHAR(255),
  result            VARCHAR(255),
  sessionIdentifier VARCHAR(255),
  targetName        VARCHAR(255),
  targetOid         VARCHAR(36),
  targetOwnerName   VARCHAR(255),
  targetOwnerOid    VARCHAR(36),
  targetOwnerType   INT4,
  targetType        INT4,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_item (
  changedItemPath VARCHAR(255) NOT NULL,
  record_id       INT8         NOT NULL,
  PRIMARY KEY (record_id, changedItemPath)
);
CREATE TABLE m_audit_prop_value (
  id        BIGSERIAL NOT NULL,
  name      VARCHAR(255),
  record_id INT8,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_ref_value (
  id              BIGSERIAL NOT NULL,
  name            VARCHAR(255),
  oid             VARCHAR(36),
  record_id       INT8,
  targetName_norm VARCHAR(255),
  targetName_orig VARCHAR(255),
  type            VARCHAR(255),
  PRIMARY KEY (id)
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
  subtype    VARCHAR(255)
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
CREATE INDEX iAuditDeltaRecordId
  ON m_audit_delta (record_id);
CREATE INDEX iTimestampValue
  ON m_audit_event (timestampValue);
CREATE INDEX iChangedItemPath
  ON m_audit_item (changedItemPath);
CREATE INDEX iAuditItemRecordId
  ON m_audit_item (record_id);
CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id);
CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id);
CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);

ALTER TABLE IF EXISTS m_ext_item
  ADD CONSTRAINT iExtItemDefinition UNIQUE (itemName, itemType, kind);
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
ALTER TABLE IF EXISTS m_audit_delta
  ADD CONSTRAINT fk_audit_delta FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_item
  ADD CONSTRAINT fk_audit_item FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
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

-- Thanks to Patrick Lightbody for submitting this...
--
-- In your Quartz properties file, you'll need to set
-- org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.PostgreSQLDelegate

drop table if exists qrtz_fired_triggers;
DROP TABLE if exists QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE if exists QRTZ_SCHEDULER_STATE;
DROP TABLE if exists QRTZ_LOCKS;
drop table if exists qrtz_simple_triggers;
drop table if exists qrtz_cron_triggers;
drop table if exists qrtz_simprop_triggers;
DROP TABLE if exists QRTZ_BLOB_TRIGGERS;
drop table if exists qrtz_triggers;
drop table if exists qrtz_job_details;
drop table if exists qrtz_calendars;

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_NONCONCURRENT BOOL NOT NULL,
    IS_UPDATE_DATA BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    EXECUTION_GROUP VARCHAR(200) NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOL NULL,
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);


CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    EXECUTION_GROUP VARCHAR(200) NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);

create table ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('schema.version', '5.22.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.22.0.0)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ bytea,
    GENERATED_ boolean,
    primary key (ID_)
);

create table ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    DEPLOY_TIME_ timestamp,
    primary key (ID_)
);

create table ACT_RE_MODEL (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    KEY_ varchar(255),
    CATEGORY_ varchar(255),
    CREATE_TIME_ timestamp,
    LAST_UPDATE_TIME_ timestamp,
    VERSION_ integer,
    META_INFO_ varchar(4000),
    DEPLOYMENT_ID_ varchar(64),
    EDITOR_SOURCE_VALUE_ID_ varchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ boolean,
    IS_CONCURRENT_ boolean,
    IS_SCOPE_ boolean,
    IS_EVENT_SCOPE_ boolean,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    LOCK_TIME_ timestamp,
    primary key (ID_)
);

create table ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_RE_PROCDEF (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255) NOT NULL,
    VERSION_ integer NOT NULL,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ varchar(4000),
    HAS_START_FORM_KEY_ boolean,
    HAS_GRAPHICAL_NOTATION_ boolean,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp,
    DUE_DATE_ timestamp,
    CATEGORY_ varchar(255),
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    FORM_KEY_ varchar(255),
    primary key (ID_)
);

create table ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar (64),
    primary key (ID_)
);

create table ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double precision,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
);

create table ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp not null,
    PROC_DEF_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_EVT_LOG (
    LOG_NR_ SERIAL PRIMARY KEY,
    TYPE_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    TIME_STAMP_ timestamp not null,
    USER_ID_ varchar(255),
    DATA_ bytea,
    LOCK_OWNER_ varchar(255),
    LOCK_TIME_ timestamp null,
    IS_PROCESSED_ smallint default 0
);

create table ACT_PROCDEF_INFO (
	ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);

create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);

create index ACT_IDX_EXE_PROCINST on ACT_RU_EXECUTION(PROC_INST_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT
    foreign key (PARENT_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER
    foreign key (SUPER_EXEC_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_PROCDEF on ACT_RU_EXECUTION(PROC_DEF_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_TSKASS_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK
    foreign key (TASK_ID_)
    references ACT_RU_TASK (ID_);

create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_IDL_PROCINST on ACT_RU_IDENTITYLINK(PROC_INST_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_PROCDEF on ACT_RU_TASK(PROC_DEF_ID_);
alter table ACT_RU_TASK
  	add constraint ACT_FK_TASK_PROCDEF
  	foreign key (PROC_DEF_ID_)
  	references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_VAR_EXE on ACT_RU_VARIABLE(EXECUTION_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_VAR_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

create index ACT_IDX_VAR_BYTEARRAY on ACT_RU_VARIABLE(BYTEARRAY_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_BYTEARRAY
    foreign key (BYTEARRAY_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_EXCEPTION on ACT_RU_JOB(EXCEPTION_STACK_ID_);
alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_EVENT_SUBSCR on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);

create index ACT_IDX_MODEL_SOURCE on ACT_RE_MODEL(EDITOR_SOURCE_VALUE_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE
    foreign key (EDITOR_SOURCE_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_MODEL_SOURCE_EXTRA on ACT_RE_MODEL(EDITOR_SOURCE_EXTRA_VALUE_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE_EXTRA
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_MODEL_DEPLOYMENT on ACT_RE_MODEL(DEPLOYMENT_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_DEPLOYMENT
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

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

create table ACT_HI_PROCINST (
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255),
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ timestamp not null,
    END_TIME_ timestamp,
    DURATION_ bigint,
    START_USER_ID_ varchar(255),
    START_ACT_ID_ varchar(255),
    END_ACT_ID_ varchar(255),
    SUPER_PROCESS_INSTANCE_ID_ varchar(64),
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    START_TIME_ timestamp not null,
    END_TIME_ timestamp,
    DURATION_ bigint,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_KEY_ varchar(255),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    START_TIME_ timestamp not null,
    CLAIM_TIME_ timestamp,
    END_TIME_ timestamp,
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    PRIORITY_ integer,
    DUE_DATE_ timestamp,
    FORM_KEY_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table ACT_HI_VARINST (
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100),
    REV_ integer,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double precision,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    CREATE_TIME_ timestamp,
    LAST_UPDATED_TIME_ timestamp,
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    ACT_INST_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(64),
    REV_ integer,
    TIME_ timestamp not null,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double precision,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TIME_ timestamp not null,
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTION_ varchar(255),
    MESSAGE_ varchar(4000),
    FULL_MSG_ bytea,
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(255),
    NAME_ varchar(255),
    DESCRIPTION_ varchar(4000),
    TYPE_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    URL_ varchar(4000),
    CONTENT_ID_ varchar(64),
    TIME_ timestamp,
    primary key (ID_)
);

create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    primary key (ID_)
);


create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);
create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);
create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);
create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);
create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);
create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);
create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);
create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_);
create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_ID_GROUP (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    TYPE_ varchar(255),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64),
    GROUP_ID_ varchar(64),
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ varchar(64),
    REV_ integer,
    FIRST_ varchar(255),
    LAST_ varchar(255),
    EMAIL_ varchar(255),
    PWD_ varchar(255),
    PICTURE_ID_ varchar(64),
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ varchar(64),
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(255),
    VALUE_ varchar(255),
    PASSWORD_ bytea,
    PARENT_ID_ varchar(255),
    primary key (ID_)
);

create index ACT_IDX_MEMB_GROUP on ACT_ID_MEMBERSHIP(GROUP_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

create index ACT_IDX_MEMB_USER on ACT_ID_MEMBERSHIP(USER_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

commit;
