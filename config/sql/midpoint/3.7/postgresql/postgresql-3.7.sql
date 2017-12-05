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
  PRIMARY KEY (id, owner_oid)
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
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_assignment_ext_boolean (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  booleanValue                 BOOLEAN      NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue)
);

CREATE TABLE m_assignment_ext_date (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  dateValue                    TIMESTAMP    NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
);

CREATE TABLE m_assignment_ext_long (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  longValue                    INT8         NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
);

CREATE TABLE m_assignment_ext_poly (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  norm                         VARCHAR(255),
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
);

CREATE TABLE m_assignment_ext_reference (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  targetoid                    VARCHAR(36)  NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  relation                     VARCHAR(157),
  targetType                   INT4,
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
);

CREATE TABLE m_assignment_ext_string (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        INT4         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  extensionType                INT4,
  dynamicDef                   BOOLEAN,
  eType                        VARCHAR(157),
  valueType                    INT4,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
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
  PRIMARY KEY (owner_id, owner_owner_oid)
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
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
);

CREATE TABLE m_audit_delta (
  checksum          VARCHAR(32) NOT NULL,
  record_id         INT8        NOT NULL,
  delta             TEXT,
  deltaOid          VARCHAR(36),
  deltaType         INT4,
  fullResult        TEXT,
  objectName_norm   VARCHAR(255),
  objectName_orig   VARCHAR(255),
  resourceName_norm VARCHAR(255),
  resourceName_orig VARCHAR(255),
  resourceOid       VARCHAR(36),
  status            INT4,
  PRIMARY KEY (checksum, record_id)
);

CREATE TABLE m_audit_event (
  id                INT8 NOT NULL,
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
  targetType        INT4,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE m_audit_item (
  changedItemPath VARCHAR(900) NOT NULL,
  record_id       INT8         NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
);

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

CREATE TABLE m_case (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
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

CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
);

CREATE TABLE m_exclusion (
  id                  INT4        NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  policy              INT4,
  targetRef_relation  VARCHAR(157),
  targetRef_targetOid VARCHAR(36),
  targetRef_type      INT4,
  PRIMARY KEY (id, owner_oid)
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
  hasPhoto                BOOLEAN DEFAULT FALSE NOT NULL,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
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
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_node (
  name_norm      VARCHAR(255),
  name_orig      VARCHAR(255),
  nodeIdentifier VARCHAR(255),
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
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
  eName        VARCHAR(157) NOT NULL,
  owner_oid    VARCHAR(36)  NOT NULL,
  ownerType    INT4         NOT NULL,
  booleanValue BOOLEAN      NOT NULL,
  dynamicDef   BOOLEAN,
  eType        VARCHAR(157),
  valueType    INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, booleanValue)
);

CREATE TABLE m_object_ext_date (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INT4         NOT NULL,
  dateValue  TIMESTAMP    NOT NULL,
  dynamicDef BOOLEAN,
  eType      VARCHAR(157),
  valueType  INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, dateValue)
);

CREATE TABLE m_object_ext_long (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INT4         NOT NULL,
  longValue  INT8         NOT NULL,
  dynamicDef BOOLEAN,
  eType      VARCHAR(157),
  valueType  INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, longValue)
);

CREATE TABLE m_object_ext_poly (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INT4         NOT NULL,
  orig       VARCHAR(255) NOT NULL,
  dynamicDef BOOLEAN,
  norm       VARCHAR(255),
  eType      VARCHAR(157),
  valueType  INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, orig)
);

CREATE TABLE m_object_ext_reference (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INT4         NOT NULL,
  targetoid  VARCHAR(36)  NOT NULL,
  dynamicDef BOOLEAN,
  relation   VARCHAR(157),
  targetType INT4,
  eType      VARCHAR(157),
  valueType  INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, targetoid)
);

CREATE TABLE m_object_ext_string (
  eName       VARCHAR(157) NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INT4         NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  dynamicDef  BOOLEAN,
  eType       VARCHAR(157),
  valueType   INT4,
  PRIMARY KEY (eName, owner_oid, ownerType, stringValue)
);

CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INT4,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
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
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayOrder     INT4,
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  name_norm        VARCHAR(255),
  name_orig        VARCHAR(255),
  tenant           BOOLEAN,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
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

CREATE TABLE m_system_configuration (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_task (
  binding                INT4,
  canRunOnNode           VARCHAR(255),
  category               VARCHAR(255),
  completionTimestamp    TIMESTAMP,
  executionStatus        INT4,
  handlerUri             VARCHAR(255),
  lastRunFinishTimestamp TIMESTAMP,
  lastRunStartTimestamp  TIMESTAMP,
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  node                   VARCHAR(255),
  objectRef_relation     VARCHAR(157),
  objectRef_targetOid    VARCHAR(36),
  objectRef_type         INT4,
  ownerRef_relation      VARCHAR(157),
  ownerRef_targetOid     VARCHAR(36),
  ownerRef_type          INT4,
  parent                 VARCHAR(255),
  recurrence             INT4,
  status                 INT4,
  taskIdentifier         VARCHAR(255),
  threadStopAction       INT4,
  waitingReason          INT4,
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
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_task_dependent (
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
);

CREATE TABLE m_trigger (
  id             INT4        NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue TIMESTAMP,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_user (
  additionalName_norm  VARCHAR(255),
  additionalName_orig  VARCHAR(255),
  costCenter           VARCHAR(255),
  emailAddress         VARCHAR(255),
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
  locale               VARCHAR(255),
  locality_norm        VARCHAR(255),
  locality_orig        VARCHAR(255),
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  nickName_norm        VARCHAR(255),
  nickName_orig        VARCHAR(255),
  preferredLanguage    VARCHAR(255),
  status               INT4,
  telephoneNumber      VARCHAR(255),
  timezone             VARCHAR(255),
  title_norm           VARCHAR(255),
  title_orig           VARCHAR(255),
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
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

CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);

CREATE INDEX iRequestable ON m_abstract_role (requestable);

CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT uc_acc_cert_campaign_name  UNIQUE (name_norm);

CREATE INDEX iCaseObjectRefTargetOid ON m_acc_cert_case (objectRef_targetOid);

CREATE INDEX iCaseTargetRefTargetOid ON m_acc_cert_case (targetRef_targetOid);

CREATE INDEX iCaseTenantRefTargetOid ON m_acc_cert_case (tenantRef_targetOid);

CREATE INDEX iCaseOrgRefTargetOid ON m_acc_cert_case (orgRef_targetOid);

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT uc_acc_cert_definition_name  UNIQUE (name_norm);

CREATE INDEX iCertWorkItemRefTargetOid ON m_acc_cert_wi_reference (targetOid);

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

CREATE INDEX iTargetRefTargetOid ON m_assignment (targetRef_targetOid);

CREATE INDEX iTenantRefTargetOid ON m_assignment (tenantRef_targetOid);

CREATE INDEX iOrgRefTargetOid ON m_assignment (orgRef_targetOid);

CREATE INDEX iResourceRefTargetOid ON m_assignment (resourceRef_targetOid);

CREATE INDEX iAExtensionBoolean ON m_assignment_ext_boolean (extensionType, eName, booleanValue);

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue);

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue);

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig);

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid);

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue);

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid);

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue);

CREATE INDEX iChangedItemPath ON m_audit_item (changedItemPath);

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id);

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id);

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);

ALTER TABLE m_connector_host
ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm);

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);

ALTER TABLE m_generic_object
ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm);

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);

ALTER TABLE m_node
ADD CONSTRAINT uc_node_name UNIQUE (name_norm);

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);

CREATE INDEX iObjectTypeClass ON m_object (objectTypeClass);

CREATE INDEX iObjectCreateTimestamp ON m_object (createTimestamp);

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);

CREATE INDEX iExtensionBoolean ON m_object_ext_boolean (ownerType, eName, booleanValue);

CREATE INDEX iExtensionBooleanDef ON m_object_ext_boolean (owner_oid, ownerType);

CREATE INDEX iExtensionDate ON m_object_ext_date (ownerType, eName, dateValue);

CREATE INDEX iExtensionDateDef ON m_object_ext_date (owner_oid, ownerType);

CREATE INDEX iExtensionLong ON m_object_ext_long (ownerType, eName, longValue);

CREATE INDEX iExtensionLongDef ON m_object_ext_long (owner_oid, ownerType);

CREATE INDEX iExtensionPolyString ON m_object_ext_poly (ownerType, eName, orig);

CREATE INDEX iExtensionPolyStringDef ON m_object_ext_poly (owner_oid, ownerType);

CREATE INDEX iExtensionReference ON m_object_ext_reference (ownerType, eName, targetoid);

CREATE INDEX iExtensionReferenceDef ON m_object_ext_reference (owner_oid, ownerType);

CREATE INDEX iExtensionString ON m_object_ext_string (ownerType, eName, stringValue);

CREATE INDEX iExtensionStringDef ON m_object_ext_string (owner_oid, ownerType);

ALTER TABLE m_object_template
ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm);

CREATE INDEX iOpExecTaskOid
  ON m_operation_execution (taskRef_targetOid);

CREATE INDEX iOpExecInitiatorOid
  ON m_operation_execution (initiatorRef_targetOid);

CREATE INDEX iOpExecStatus
  ON m_operation_execution (status);

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);

ALTER TABLE m_org
ADD CONSTRAINT uc_org_name UNIQUE (name_norm);

CREATE INDEX iDisplayOrder ON m_org (displayOrder);

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid);

CREATE INDEX iDescendant ON m_org_closure (descendant_oid);

CREATE INDEX iDescendantAncestor ON m_org_closure (descendant_oid, ancestor_oid);

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid);

ALTER TABLE m_report
ADD CONSTRAINT uc_report_name UNIQUE (name_norm);

CREATE INDEX iReportParent ON m_report (parent);

ALTER TABLE m_resource
ADD CONSTRAINT uc_resource_name UNIQUE (name_norm);

ALTER TABLE m_role
ADD CONSTRAINT uc_role_name UNIQUE (name_norm);

ALTER TABLE m_security_policy
ADD CONSTRAINT uc_security_policy_name UNIQUE (name_norm);

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowDead ON m_shadow (dead);

CREATE INDEX iShadowKind ON m_shadow (kind);

CREATE INDEX iShadowIntent ON m_shadow (intent);

CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);

CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);

CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);

CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);

ALTER TABLE m_system_configuration
ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm);

ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);

CREATE INDEX iParent ON m_task (parent);

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_user
ADD CONSTRAINT uc_user_name UNIQUE (name_norm);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iFullName ON m_user (fullName_orig);

CREATE INDEX iFamilyName ON m_user (familyName_orig);

CREATE INDEX iGivenName ON m_user (givenName_orig);

CREATE INDEX iLocality ON m_user (locality_orig);

ALTER TABLE m_value_policy
ADD CONSTRAINT uc_value_policy_name UNIQUE (name_norm);

ALTER TABLE m_abstract_role
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (oid)
REFERENCES m_focus;

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT fk_acc_cert_campaign
    FOREIGN KEY (oid)
    REFERENCES m_object;

ALTER TABLE m_acc_cert_case
ADD CONSTRAINT fk_acc_cert_case_owner
FOREIGN KEY (owner_oid)
REFERENCES m_acc_cert_campaign;

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT fk_acc_cert_definition
    FOREIGN KEY (oid)
    REFERENCES m_object;

ALTER TABLE m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_acc_cert_case;

ALTER TABLE m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner
FOREIGN KEY (owner_id, owner_owner_id, owner_owner_owner_oid)
REFERENCES m_acc_cert_wi;

ALTER TABLE m_assignment
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_assignment_ext_boolean
ADD CONSTRAINT fk_assignment_ext_boolean
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_ext_date
ADD CONSTRAINT fk_assignment_ext_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_ext_long
ADD CONSTRAINT fk_assignment_ext_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_ext_poly
ADD CONSTRAINT fk_assignment_ext_poly
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_ext_reference
ADD CONSTRAINT fk_assignment_ext_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_ext_string
ADD CONSTRAINT fk_assignment_ext_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation
FOREIGN KEY (assignment_id, assignment_oid)
REFERENCES m_assignment;

ALTER TABLE m_assignment_reference
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment;

ALTER TABLE m_audit_delta
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_case
  ADD CONSTRAINT fk_case
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_connector
ADD CONSTRAINT fk_connector
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_connector_host
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_connector_target_system
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_oid)
REFERENCES m_connector;

ALTER TABLE m_exclusion
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_focus
ADD CONSTRAINT fk_focus
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_focus_photo
ADD CONSTRAINT fk_focus_photo
FOREIGN KEY (owner_oid)
REFERENCES m_focus;

ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation
FOREIGN KEY (focus_oid)
REFERENCES m_focus;

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_generic_object
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table_owner
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table;

ALTER TABLE m_node
ADD CONSTRAINT fk_node
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_boolean
ADD CONSTRAINT fk_object_ext_boolean
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_date
ADD CONSTRAINT fk_object_ext_date
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_long
ADD CONSTRAINT fk_object_ext_long
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_poly
ADD CONSTRAINT fk_object_ext_poly
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_reference
ADD CONSTRAINT fk_object_ext_reference
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_ext_string
ADD CONSTRAINT fk_object_ext_string
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_template
ADD CONSTRAINT fk_object_template
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_org
ADD CONSTRAINT fk_org
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object;

ALTER TABLE m_org_org_type
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_oid)
REFERENCES m_org;

ALTER TABLE m_reference
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_report
ADD CONSTRAINT fk_report
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_report_output
ADD CONSTRAINT fk_report_output
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_resource
ADD CONSTRAINT fk_resource
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_role
ADD CONSTRAINT fk_role
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_security_policy
ADD CONSTRAINT fk_security_policy
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_sequence
ADD CONSTRAINT fk_sequence
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_service
  ADD CONSTRAINT fk_service
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type
FOREIGN KEY (service_oid)
REFERENCES m_service;

ALTER TABLE m_shadow
ADD CONSTRAINT fk_shadow
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_system_configuration
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_task
ADD CONSTRAINT fk_task
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_task_dependent
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_oid)
REFERENCES m_task;

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_user
ADD CONSTRAINT fk_user
FOREIGN KEY (oid)
REFERENCES m_focus;

ALTER TABLE m_user_employee_type
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organization
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organizational_unit
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_value_policy
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE SEQUENCE hibernate_sequence START 1 INCREMENT 1;
