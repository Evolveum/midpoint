CREATE TABLE m_acc_cert_campaign (
  definitionRef_relation  NVARCHAR(157) COLLATE database_default,
  definitionRef_targetOid NVARCHAR(36) COLLATE database_default,
  definitionRef_type      INT,
  endTimestamp            DATETIME2,
  handlerUri              NVARCHAR(255) COLLATE database_default,
  iteration               INT                                   NOT NULL,
  name_norm               NVARCHAR(255) COLLATE database_default,
  name_orig               NVARCHAR(255) COLLATE database_default,
  ownerRef_relation       NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid      NVARCHAR(36) COLLATE database_default,
  ownerRef_type           INT,
  stageNumber             INT,
  startTimestamp          DATETIME2,
  state                   INT,
  oid                     NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
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
  currentStageOutcome      NVARCHAR(255) COLLATE database_default,
  fullObject               VARBINARY(MAX),
  iteration                INT                                   NOT NULL,
  objectRef_relation       NVARCHAR(157) COLLATE database_default,
  objectRef_targetOid      NVARCHAR(36) COLLATE database_default,
  objectRef_type           INT,
  orgRef_relation          NVARCHAR(157) COLLATE database_default,
  orgRef_targetOid         NVARCHAR(36) COLLATE database_default,
  orgRef_type              INT,
  outcome                  NVARCHAR(255) COLLATE database_default,
  remediedTimestamp        DATETIME2,
  reviewDeadline           DATETIME2,
  reviewRequestedTimestamp DATETIME2,
  stageNumber              INT,
  targetRef_relation       NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid      NVARCHAR(36) COLLATE database_default,
  targetRef_type           INT,
  tenantRef_relation       NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid      NVARCHAR(36) COLLATE database_default,
  tenantRef_type           INT,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_acc_cert_definition (
  handlerUri                   NVARCHAR(255) COLLATE database_default,
  lastCampaignClosedTimestamp  DATETIME2,
  lastCampaignStartedTimestamp DATETIME2,
  name_norm                    NVARCHAR(255) COLLATE database_default,
  name_orig                    NVARCHAR(255) COLLATE database_default,
  ownerRef_relation            NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid           NVARCHAR(36) COLLATE database_default,
  ownerRef_type                INT,
  oid                          NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_acc_cert_wi (
  id                     INT                                   NOT NULL,
  owner_id               INT                                   NOT NULL,
  owner_owner_oid        NVARCHAR(36) COLLATE database_default NOT NULL,
  closeTimestamp         DATETIME2,
  iteration              INT                                   NOT NULL,
  outcome                NVARCHAR(255) COLLATE database_default,
  outputChangeTimestamp  DATETIME2,
  performerRef_relation  NVARCHAR(157) COLLATE database_default,
  performerRef_targetOid NVARCHAR(36) COLLATE database_default,
  performerRef_type      INT,
  stageNumber            INT,
  PRIMARY KEY (owner_owner_oid, owner_id, id)
);
CREATE TABLE m_acc_cert_wi_reference (
  owner_id              INT                                    NOT NULL,
  owner_owner_id        INT                                    NOT NULL,
  owner_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  relation              NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid             NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType            INT,
  PRIMARY KEY (owner_owner_owner_oid, owner_owner_id, owner_id, relation, targetOid)
);
CREATE TABLE m_assignment (
  id                      INT                                   NOT NULL,
  owner_oid               NVARCHAR(36) COLLATE database_default NOT NULL,
  administrativeStatus    INT,
  archiveTimestamp        DATETIME2,
  disableReason           NVARCHAR(255) COLLATE database_default,
  disableTimestamp        DATETIME2,
  effectiveStatus         INT,
  enableTimestamp         DATETIME2,
  validFrom               DATETIME2,
  validTo                 DATETIME2,
  validityChangeTimestamp DATETIME2,
  validityStatus          INT,
  assignmentOwner         INT,
  createChannel           NVARCHAR(255) COLLATE database_default,
  createTimestamp         DATETIME2,
  creatorRef_relation     NVARCHAR(157) COLLATE database_default,
  creatorRef_targetOid    NVARCHAR(36) COLLATE database_default,
  creatorRef_type         INT,
  lifecycleState          NVARCHAR(255) COLLATE database_default,
  modifierRef_relation    NVARCHAR(157) COLLATE database_default,
  modifierRef_targetOid   NVARCHAR(36) COLLATE database_default,
  modifierRef_type        INT,
  modifyChannel           NVARCHAR(255) COLLATE database_default,
  modifyTimestamp         DATETIME2,
  orderValue              INT,
  orgRef_relation         NVARCHAR(157) COLLATE database_default,
  orgRef_targetOid        NVARCHAR(36) COLLATE database_default,
  orgRef_type             INT,
  resourceRef_relation    NVARCHAR(157) COLLATE database_default,
  resourceRef_targetOid   NVARCHAR(36) COLLATE database_default,
  resourceRef_type        INT,
  targetRef_relation      NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid     NVARCHAR(36) COLLATE database_default,
  targetRef_type          INT,
  tenantRef_relation      NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid     NVARCHAR(36) COLLATE database_default,
  tenantRef_type          INT,
  extId                   INT,
  extOid                  NVARCHAR(36) COLLATE database_default,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_assignment_ext_boolean (
  item_id                      INT                                   NOT NULL,
  anyContainer_owner_id        INT                                   NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  booleanValue                 BIT                                   NOT NULL,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)
);
CREATE TABLE m_assignment_ext_date (
  item_id                      INT                                   NOT NULL,
  anyContainer_owner_id        INT                                   NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  dateValue                    DATETIME2                             NOT NULL,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)
);
CREATE TABLE m_assignment_ext_long (
  item_id                      INT                                   NOT NULL,
  anyContainer_owner_id        INT                                   NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  longValue                    BIGINT                                NOT NULL,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)
);
CREATE TABLE m_assignment_ext_poly (
  item_id                      INT                                    NOT NULL,
  anyContainer_owner_id        INT                                    NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  orig                         NVARCHAR(255) COLLATE database_default NOT NULL,
  norm                         NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)
);
CREATE TABLE m_assignment_ext_reference (
  item_id                      INT                                   NOT NULL,
  anyContainer_owner_id        INT                                   NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  targetoid                    NVARCHAR(36) COLLATE database_default NOT NULL,
  relation                     NVARCHAR(157) COLLATE database_default,
  targetType                   INT,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)
);
CREATE TABLE m_assignment_ext_string (
  item_id                      INT                                    NOT NULL,
  anyContainer_owner_id        INT                                    NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  stringValue                  NVARCHAR(255) COLLATE database_default NOT NULL,
  PRIMARY KEY ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)
);
CREATE TABLE m_assignment_extension (
  owner_id        INT                                   NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (owner_owner_oid, owner_id)
);
CREATE TABLE m_assignment_policy_situation (
  assignment_id   INT                                   NOT NULL,
  assignment_oid  NVARCHAR(36) COLLATE database_default NOT NULL,
  policySituation NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_assignment_reference (
  owner_id        INT                                    NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  reference_type  INT                                    NOT NULL,
  relation        NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid       NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType      INT,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, relation, targetOid)
);
CREATE TABLE m_audit_delta (
  checksum          NVARCHAR(32) COLLATE database_default NOT NULL,
  record_id         BIGINT                                NOT NULL,
  delta             VARBINARY(MAX),
  deltaOid          NVARCHAR(36) COLLATE database_default,
  deltaType         INT,
  fullResult        VARBINARY(MAX),
  objectName_norm   NVARCHAR(255) COLLATE database_default,
  objectName_orig   NVARCHAR(255) COLLATE database_default,
  resourceName_norm NVARCHAR(255) COLLATE database_default,
  resourceName_orig NVARCHAR(255) COLLATE database_default,
  resourceOid       NVARCHAR(36) COLLATE database_default,
  status            INT,
  PRIMARY KEY (record_id, checksum)
);
CREATE TABLE m_audit_event (
  id                BIGINT IDENTITY NOT NULL,
  attorneyName      NVARCHAR(255) COLLATE database_default,
  attorneyOid       NVARCHAR(36) COLLATE database_default,
  channel           NVARCHAR(255) COLLATE database_default,
  eventIdentifier   NVARCHAR(255) COLLATE database_default,
  eventStage        INT,
  eventType         INT,
  hostIdentifier    NVARCHAR(255) COLLATE database_default,
  initiatorName     NVARCHAR(255) COLLATE database_default,
  initiatorOid      NVARCHAR(36) COLLATE database_default,
  initiatorType     INT,
  message           NVARCHAR(1024) COLLATE database_default,
  nodeIdentifier    NVARCHAR(255) COLLATE database_default,
  outcome           INT,
  parameter         NVARCHAR(255) COLLATE database_default,
  remoteHostAddress NVARCHAR(255) COLLATE database_default,
  requestIdentifier NVARCHAR(255) COLLATE database_default,
  result            NVARCHAR(255) COLLATE database_default,
  sessionIdentifier NVARCHAR(255) COLLATE database_default,
  targetName        NVARCHAR(255) COLLATE database_default,
  targetOid         NVARCHAR(36) COLLATE database_default,
  targetOwnerName   NVARCHAR(255) COLLATE database_default,
  targetOwnerOid    NVARCHAR(36) COLLATE database_default,
  targetOwnerType   INT,
  targetType        INT,
  taskIdentifier    NVARCHAR(255) COLLATE database_default,
  taskOID           NVARCHAR(255) COLLATE database_default,
  timestampValue    DATETIME2,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_item (
  changedItemPath NVARCHAR(442) COLLATE database_default NOT NULL,    -- because of max. index size of 900 bytes
  record_id       BIGINT                                 NOT NULL,
  PRIMARY KEY (record_id, changedItemPath)
);
CREATE TABLE m_audit_prop_value (
  id        BIGINT IDENTITY NOT NULL,
  name      NVARCHAR(255) COLLATE database_default,
  record_id BIGINT,
  value     NVARCHAR(1024) COLLATE database_default,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_ref_value (
  id              BIGINT IDENTITY NOT NULL,
  name            NVARCHAR(255) COLLATE database_default,
  oid             NVARCHAR(36) COLLATE database_default,
  record_id       BIGINT,
  targetName_norm NVARCHAR(255) COLLATE database_default,
  targetName_orig NVARCHAR(255) COLLATE database_default,
  type            NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_resource (
  resourceOid     NVARCHAR(255) COLLATE database_default NOT NULL,
  record_id       BIGINT                                 NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);
CREATE TABLE m_case_wi (
  id                            INT                                   NOT NULL,
  owner_oid                     NVARCHAR(36) COLLATE database_default NOT NULL,
  closeTimestamp                DATETIME2,
  createTimestamp               DATETIME2,
  deadline                      DATETIME2,
  originalAssigneeRef_relation  NVARCHAR(157) COLLATE database_default,
  originalAssigneeRef_targetOid NVARCHAR(36) COLLATE database_default,
  originalAssigneeRef_type      INT,
  outcome                       NVARCHAR(255) COLLATE database_default,
  performerRef_relation         NVARCHAR(157) COLLATE database_default,
  performerRef_targetOid        NVARCHAR(36) COLLATE database_default,
  performerRef_type             INT,
  stageNumber                   INT,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_case_wi_reference (
  owner_id        INT                                    NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  reference_type  INT                                    NOT NULL,
  relation        NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid       NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType      INT,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, targetOid, relation)
);
CREATE TABLE m_connector_target_system (
  connector_oid    NVARCHAR(36) COLLATE database_default NOT NULL,
  targetSystemType NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_ext_item (
  id       INT IDENTITY NOT NULL,
  kind     INT,
  itemName NVARCHAR(157) COLLATE database_default,
  itemType NVARCHAR(157) COLLATE database_default,
  PRIMARY KEY (id)
);
CREATE TABLE m_focus_photo (
  owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  photo     VARBINARY(MAX),
  PRIMARY KEY (owner_oid)
);
CREATE TABLE m_focus_policy_situation (
  focus_oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  policySituation NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_object (
  oid                   NVARCHAR(36) COLLATE database_default NOT NULL,
  createChannel         NVARCHAR(255) COLLATE database_default,
  createTimestamp       DATETIME2,
  creatorRef_relation   NVARCHAR(157) COLLATE database_default,
  creatorRef_targetOid  NVARCHAR(36) COLLATE database_default,
  creatorRef_type       INT,
  fullObject            VARBINARY(MAX),
  lifecycleState        NVARCHAR(255) COLLATE database_default,
  modifierRef_relation  NVARCHAR(157) COLLATE database_default,
  modifierRef_targetOid NVARCHAR(36) COLLATE database_default,
  modifierRef_type      INT,
  modifyChannel         NVARCHAR(255) COLLATE database_default,
  modifyTimestamp       DATETIME2,
  name_norm             NVARCHAR(255) COLLATE database_default,
  name_orig             NVARCHAR(255) COLLATE database_default,
  objectTypeClass       INT,
  tenantRef_relation    NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid   NVARCHAR(36) COLLATE database_default,
  tenantRef_type        INT,
  version               INT                                   NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_object_ext_boolean (
  item_id      INT                                   NOT NULL,
  owner_oid    NVARCHAR(36) COLLATE database_default NOT NULL,
  ownerType    INT                                   NOT NULL,
  booleanValue BIT                                   NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, booleanValue)
);
CREATE TABLE m_object_ext_date (
  item_id   INT                                   NOT NULL,
  owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  ownerType INT                                   NOT NULL,
  dateValue DATETIME2                             NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, dateValue)
);
CREATE TABLE m_object_ext_long (
  item_id   INT                                   NOT NULL,
  owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  ownerType INT                                   NOT NULL,
  longValue BIGINT                                NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, longValue)
);
CREATE TABLE m_object_ext_poly (
  item_id   INT                                    NOT NULL,
  owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType INT                                    NOT NULL,
  orig      NVARCHAR(255) COLLATE database_default NOT NULL,
  norm      NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (owner_oid, ownerType, item_id, orig)
);
CREATE TABLE m_object_ext_reference (
  item_id    INT                                   NOT NULL,
  owner_oid  NVARCHAR(36) COLLATE database_default NOT NULL,
  ownerType  INT                                   NOT NULL,
  targetoid  NVARCHAR(36) COLLATE database_default NOT NULL,
  relation   NVARCHAR(157) COLLATE database_default,
  targetType INT,
  PRIMARY KEY (owner_oid, ownerType, item_id, targetoid)
);
CREATE TABLE m_object_ext_string (
  item_id     INT                                    NOT NULL,
  owner_oid   NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType   INT                                    NOT NULL,
  stringValue NVARCHAR(255) COLLATE database_default NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, stringValue)
);
CREATE TABLE m_object_subtype (
  object_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  subtype    NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_object_text_info (
  owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  text      NVARCHAR(255) COLLATE database_default NOT NULL,
  PRIMARY KEY (owner_oid, text)
);
CREATE TABLE m_operation_execution (
  id                     INT                                   NOT NULL,
  owner_oid              NVARCHAR(36) COLLATE database_default NOT NULL,
  initiatorRef_relation  NVARCHAR(157) COLLATE database_default,
  initiatorRef_targetOid NVARCHAR(36) COLLATE database_default,
  initiatorRef_type      INT,
  status                 INT,
  taskRef_relation       NVARCHAR(157) COLLATE database_default,
  taskRef_targetOid      NVARCHAR(36) COLLATE database_default,
  taskRef_type           INT,
  timestampValue         DATETIME2,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_org_closure (
  ancestor_oid   NVARCHAR(36) COLLATE database_default NOT NULL,
  descendant_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  val            INT,
  PRIMARY KEY (ancestor_oid, descendant_oid)
);
CREATE TABLE m_org_org_type (
  org_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  orgType NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_reference (
  owner_oid      NVARCHAR(36) COLLATE database_default  NOT NULL,
  reference_type INT                                    NOT NULL,
  relation       NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid      NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType     INT,
  PRIMARY KEY (owner_oid, reference_type, relation, targetOid)
);
CREATE TABLE m_service_type (
  service_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  serviceType NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_shadow (
  attemptNumber                INT,
  dead                         BIT,
  exist                        BIT,
  failedOperationType          INT,
  fullSynchronizationTimestamp DATETIME2,
  intent                       NVARCHAR(255) COLLATE database_default,
  kind                         INT,
  name_norm                    NVARCHAR(255) COLLATE database_default,
  name_orig                    NVARCHAR(255) COLLATE database_default,
  objectClass                  NVARCHAR(157) COLLATE database_default,
  pendingOperationCount        INT,
  primaryIdentifierValue       NVARCHAR(255) COLLATE database_default,
  resourceRef_relation         NVARCHAR(157) COLLATE database_default,
  resourceRef_targetOid        NVARCHAR(36) COLLATE database_default,
  resourceRef_type             INT,
  status                       INT,
  synchronizationSituation     INT,
  synchronizationTimestamp     DATETIME2,
  oid                          NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_task (
  binding                  INT,
  category                 NVARCHAR(255) COLLATE database_default,
  completionTimestamp      DATETIME2,
  executionStatus          INT,
  fullResult               VARBINARY(MAX),
  handlerUri               NVARCHAR(255) COLLATE database_default,
  lastRunFinishTimestamp   DATETIME2,
  lastRunStartTimestamp    DATETIME2,
  name_norm                NVARCHAR(255) COLLATE database_default,
  name_orig                NVARCHAR(255) COLLATE database_default,
  node                     NVARCHAR(255) COLLATE database_default,
  objectRef_relation       NVARCHAR(157) COLLATE database_default,
  objectRef_targetOid      NVARCHAR(36) COLLATE database_default,
  objectRef_type           INT,
  ownerRef_relation        NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid       NVARCHAR(36) COLLATE database_default,
  ownerRef_type            INT,
  parent                   NVARCHAR(255) COLLATE database_default,
  recurrence               INT,
  status                   INT,
  taskIdentifier           NVARCHAR(255) COLLATE database_default,
  threadStopAction         INT,
  waitingReason            INT,
  oid                      NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_task_dependent (
  task_oid  NVARCHAR(36) COLLATE database_default NOT NULL,
  dependent NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_user_employee_type (
  user_oid     NVARCHAR(36) COLLATE database_default NOT NULL,
  employeeType NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_user_organization (
  user_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  norm     NVARCHAR(255) COLLATE database_default,
  orig     NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_user_organizational_unit (
  user_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  norm     NVARCHAR(255) COLLATE database_default,
  orig     NVARCHAR(255) COLLATE database_default
);
CREATE TABLE m_abstract_role (
  approvalProcess    NVARCHAR(255) COLLATE database_default,
  autoassign_enabled BIT,
  displayName_norm   NVARCHAR(255) COLLATE database_default,
  displayName_orig   NVARCHAR(255) COLLATE database_default,
  identifier         NVARCHAR(255) COLLATE database_default,
  ownerRef_relation  NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid NVARCHAR(36) COLLATE database_default,
  ownerRef_type      INT,
  requestable        BIT,
  riskLevel          NVARCHAR(255) COLLATE database_default,
  oid                NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_archetype (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_case (
  closeTimestamp         DATETIME2,
  name_norm           NVARCHAR(255) COLLATE database_default,
  name_orig           NVARCHAR(255) COLLATE database_default,
  objectRef_relation  NVARCHAR(157) COLLATE database_default,
  objectRef_targetOid NVARCHAR(36) COLLATE database_default,
  objectRef_type      INT,
  parentRef_relation  NVARCHAR(157) COLLATE database_default,
  parentRef_targetOid NVARCHAR(36) COLLATE database_default,
  parentRef_type      INT,
  requestorRef_relation  NVARCHAR(157) COLLATE database_default,
  requestorRef_targetOid NVARCHAR(36) COLLATE database_default,
  requestorRef_type      INT,
  state               NVARCHAR(255) COLLATE database_default,
  targetRef_relation  NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid NVARCHAR(36) COLLATE database_default,
  targetRef_type      INT,
  oid                 NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector (
  connectorBundle            NVARCHAR(255) COLLATE database_default,
  connectorHostRef_relation  NVARCHAR(157) COLLATE database_default,
  connectorHostRef_targetOid NVARCHAR(36) COLLATE database_default,
  connectorHostRef_type      INT,
  connectorType              NVARCHAR(255) COLLATE database_default,
  connectorVersion           NVARCHAR(255) COLLATE database_default,
  framework                  NVARCHAR(255) COLLATE database_default,
  name_norm                  NVARCHAR(255) COLLATE database_default,
  name_orig                  NVARCHAR(255) COLLATE database_default,
  oid                        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector_host (
  hostname  NVARCHAR(255) COLLATE database_default,
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  port      NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_dashboard (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_focus (
  administrativeStatus    INT,
  archiveTimestamp        DATETIME2,
  disableReason           NVARCHAR(255) COLLATE database_default,
  disableTimestamp        DATETIME2,
  effectiveStatus         INT,
  enableTimestamp         DATETIME2,
  validFrom               DATETIME2,
  validTo                 DATETIME2,
  validityChangeTimestamp DATETIME2,
  validityStatus          INT,
  costCenter              NVARCHAR(255) COLLATE database_default,
  emailAddress            NVARCHAR(255) COLLATE database_default,
  hasPhoto                BIT DEFAULT 0                     NOT NULL,
  locale                  NVARCHAR(255) COLLATE database_default,
  locality_norm           NVARCHAR(255) COLLATE database_default,
  locality_orig           NVARCHAR(255) COLLATE database_default,
  preferredLanguage       NVARCHAR(255) COLLATE database_default,
  telephoneNumber         NVARCHAR(255) COLLATE database_default,
  timezone                NVARCHAR(255) COLLATE database_default,
  oid                     NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_form (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_function_library (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_generic_object (
  name_norm  NVARCHAR(255) COLLATE database_default,
  name_orig  NVARCHAR(255) COLLATE database_default,
  objectType NVARCHAR(255) COLLATE database_default,
  oid        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_global_metadata (
  name  NVARCHAR(255) COLLATE database_default NOT NULL,
  value NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (name)
);
CREATE TABLE m_lookup_table (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_lookup_table_row (
  id                  INT                                   NOT NULL,
  owner_oid           NVARCHAR(36) COLLATE database_default NOT NULL,
  row_key             NVARCHAR(255) COLLATE database_default,
  label_norm          NVARCHAR(255) COLLATE database_default,
  label_orig          NVARCHAR(255) COLLATE database_default,
  lastChangeTimestamp DATETIME2,
  row_value           NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_node (
  name_norm      NVARCHAR(255) COLLATE database_default,
  name_orig      NVARCHAR(255) COLLATE database_default,
  nodeIdentifier NVARCHAR(255) COLLATE database_default,
  oid            NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_object_collection (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_object_template (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  type      INT,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_org (
  displayOrder INT,
  name_norm    NVARCHAR(255) COLLATE database_default,
  name_orig    NVARCHAR(255) COLLATE database_default,
  tenant       BIT,
  oid          NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report (
  export              INT,
  name_norm           NVARCHAR(255) COLLATE database_default,
  name_orig           NVARCHAR(255) COLLATE database_default,
  orientation         INT,
  parent              BIT,
  useHibernateSession BIT,
  oid                 NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report_output (
  name_norm           NVARCHAR(255) COLLATE database_default,
  name_orig           NVARCHAR(255) COLLATE database_default,
  reportRef_relation  NVARCHAR(157) COLLATE database_default,
  reportRef_targetOid NVARCHAR(36) COLLATE database_default,
  reportRef_type      INT,
  oid                 NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_resource (
  administrativeState        INT,
  connectorRef_relation      NVARCHAR(157) COLLATE database_default,
  connectorRef_targetOid     NVARCHAR(36) COLLATE database_default,
  connectorRef_type          INT,
  name_norm                  NVARCHAR(255) COLLATE database_default,
  name_orig                  NVARCHAR(255) COLLATE database_default,
  o16_lastAvailabilityStatus INT,
  oid                        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_role (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  roleType  NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_security_policy (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_sequence (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_service (
  displayOrder INT,
  name_norm    NVARCHAR(255) COLLATE database_default,
  name_orig    NVARCHAR(255) COLLATE database_default,
  oid          NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_system_configuration (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_trigger (
  id             INT                                   NOT NULL,
  owner_oid      NVARCHAR(36) COLLATE database_default NOT NULL,
  handlerUri     NVARCHAR(255) COLLATE database_default,
  timestampValue DATETIME2,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_user (
  additionalName_norm  NVARCHAR(255) COLLATE database_default,
  additionalName_orig  NVARCHAR(255) COLLATE database_default,
  employeeNumber       NVARCHAR(255) COLLATE database_default,
  familyName_norm      NVARCHAR(255) COLLATE database_default,
  familyName_orig      NVARCHAR(255) COLLATE database_default,
  fullName_norm        NVARCHAR(255) COLLATE database_default,
  fullName_orig        NVARCHAR(255) COLLATE database_default,
  givenName_norm       NVARCHAR(255) COLLATE database_default,
  givenName_orig       NVARCHAR(255) COLLATE database_default,
  honorificPrefix_norm NVARCHAR(255) COLLATE database_default,
  honorificPrefix_orig NVARCHAR(255) COLLATE database_default,
  honorificSuffix_norm NVARCHAR(255) COLLATE database_default,
  honorificSuffix_orig NVARCHAR(255) COLLATE database_default,
  name_norm            NVARCHAR(255) COLLATE database_default,
  name_orig            NVARCHAR(255) COLLATE database_default,
  nickName_norm        NVARCHAR(255) COLLATE database_default,
  nickName_orig        NVARCHAR(255) COLLATE database_default,
  title_norm           NVARCHAR(255) COLLATE database_default,
  title_orig           NVARCHAR(255) COLLATE database_default,
  oid                  NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_value_policy (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);
CREATE INDEX iCertCampaignNameOrig
  ON m_acc_cert_campaign (name_orig);
ALTER TABLE m_acc_cert_campaign
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
ALTER TABLE m_acc_cert_definition
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
CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid);
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id);
CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);

ALTER TABLE m_ext_item
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
-- manually created (the default does not work with null values)
CREATE UNIQUE NONCLUSTERED INDEX iPrimaryIdentifierValueWithOC
    ON m_shadow(primaryIdentifierValue, objectClass, resourceRef_targetOid)
    WHERE primaryIdentifierValue IS NOT NULL AND objectClass IS NOT NULL AND resourceRef_targetOid IS NOT NULL;
CREATE INDEX iParent
  ON m_task (parent);
CREATE INDEX iTaskObjectOid ON m_task(objectRef_targetOid);
CREATE INDEX iTaskNameOrig
  ON m_task (name_orig);
ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);
CREATE INDEX iAbstractRoleIdentifier
  ON m_abstract_role (identifier);
CREATE INDEX iRequestable
  ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled ON m_abstract_role(autoassign_enabled);
CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);
CREATE INDEX iCaseNameOrig
  ON m_case (name_orig);
CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid);
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid);
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid);
CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);
CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig);
CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm);
CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig);
ALTER TABLE m_connector_host
  ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm);
CREATE INDEX iDashboardNameOrig
  ON m_dashboard (name_orig);
ALTER TABLE m_dashboard
  ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);
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
ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);
CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig);
ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);
CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig);
ALTER TABLE m_generic_object
  ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm);
CREATE INDEX iLookupTableNameOrig
  ON m_lookup_table (name_orig);
ALTER TABLE m_lookup_table
  ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);
ALTER TABLE m_lookup_table_row
  ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);
CREATE INDEX iNodeNameOrig
  ON m_node (name_orig);
ALTER TABLE m_node
  ADD CONSTRAINT uc_node_name UNIQUE (name_norm);
CREATE INDEX iObjectCollectionNameOrig
  ON m_object_collection (name_orig);
ALTER TABLE m_object_collection
  ADD CONSTRAINT uc_object_collection_name UNIQUE (name_norm);
CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig);
ALTER TABLE m_object_template
  ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm);
CREATE INDEX iDisplayOrder
  ON m_org (displayOrder);
CREATE INDEX iOrgNameOrig
  ON m_org (name_orig);
ALTER TABLE m_org
  ADD CONSTRAINT uc_org_name UNIQUE (name_norm);
CREATE INDEX iReportParent
  ON m_report (parent);
CREATE INDEX iReportNameOrig
  ON m_report (name_orig);
ALTER TABLE m_report
  ADD CONSTRAINT uc_report_name UNIQUE (name_norm);
CREATE INDEX iReportOutputNameOrig
  ON m_report_output (name_orig);
CREATE INDEX iReportOutputNameNorm
  ON m_report_output (name_norm);
CREATE INDEX iResourceNameOrig
  ON m_resource (name_orig);
ALTER TABLE m_resource
  ADD CONSTRAINT uc_resource_name UNIQUE (name_norm);
CREATE INDEX iRoleNameOrig
  ON m_role (name_orig);
ALTER TABLE m_role
  ADD CONSTRAINT uc_role_name UNIQUE (name_norm);
CREATE INDEX iSecurityPolicyNameOrig
  ON m_security_policy (name_orig);
ALTER TABLE m_security_policy
  ADD CONSTRAINT uc_security_policy_name UNIQUE (name_norm);
CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig);
ALTER TABLE m_sequence
  ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);
CREATE INDEX iServiceNameOrig
  ON m_service (name_orig);
CREATE INDEX iServiceNameNorm
  ON m_service (name_norm);
CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig);
ALTER TABLE m_system_configuration
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
ALTER TABLE m_user
  ADD CONSTRAINT uc_user_name UNIQUE (name_norm);
CREATE INDEX iValuePolicyNameOrig
  ON m_value_policy (name_orig);
ALTER TABLE m_value_policy
  ADD CONSTRAINT uc_value_policy_name UNIQUE (name_norm);
ALTER TABLE m_acc_cert_campaign
  ADD CONSTRAINT fk_acc_cert_campaign FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner FOREIGN KEY (owner_oid) REFERENCES m_acc_cert_campaign;
ALTER TABLE m_acc_cert_definition
  ADD CONSTRAINT fk_acc_cert_definition FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_acc_cert_case;
ALTER TABLE m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner FOREIGN KEY (owner_owner_owner_oid, owner_owner_id, owner_id) REFERENCES m_acc_cert_wi;
ALTER TABLE m_assignment
  ADD CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation FOREIGN KEY (assignment_oid, assignment_id) REFERENCES m_assignment;
ALTER TABLE m_assignment_reference
  ADD CONSTRAINT fk_assignment_reference FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_assignment;

-- These are created manually
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

ALTER TABLE m_audit_delta
  ADD CONSTRAINT fk_audit_delta FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_case_wi
  ADD CONSTRAINT fk_case_wi_owner FOREIGN KEY (owner_oid) REFERENCES m_case;
ALTER TABLE m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi;
ALTER TABLE m_connector_target_system
  ADD CONSTRAINT fk_connector_target_system FOREIGN KEY (connector_oid) REFERENCES m_connector;
ALTER TABLE m_focus_photo
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (owner_oid) REFERENCES m_focus;
ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation FOREIGN KEY (focus_oid) REFERENCES m_focus;
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_object_ext_long FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_object_ext_string FOREIGN KEY (owner_oid) REFERENCES m_object;

-- These are created manually
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_o_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_o_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

ALTER TABLE m_object_subtype
  ADD CONSTRAINT fk_object_subtype FOREIGN KEY (object_oid) REFERENCES m_object;
ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor_oid) REFERENCES m_object;
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant_oid) REFERENCES m_object;
ALTER TABLE m_org_org_type
  ADD CONSTRAINT fk_org_org_type FOREIGN KEY (org_oid) REFERENCES m_org;
ALTER TABLE m_reference
  ADD CONSTRAINT fk_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type FOREIGN KEY (service_oid) REFERENCES m_service;
ALTER TABLE m_shadow
  ADD CONSTRAINT fk_shadow FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_task
  ADD CONSTRAINT fk_task FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_task_dependent
  ADD CONSTRAINT fk_task_dependent FOREIGN KEY (task_oid) REFERENCES m_task;
ALTER TABLE m_user_employee_type
  ADD CONSTRAINT fk_user_employee_type FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_abstract_role
  ADD CONSTRAINT fk_abstract_role FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE m_case
  ADD CONSTRAINT fk_case FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_connector
  ADD CONSTRAINT fk_connector FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_connector_host
  ADD CONSTRAINT fk_connector_host FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_focus
  ADD CONSTRAINT fk_focus FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_form
  ADD CONSTRAINT fk_form FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE m_lookup_table
  ADD CONSTRAINT fk_lookup_table FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_lookup_table_row
  ADD CONSTRAINT fk_lookup_table_owner FOREIGN KEY (owner_oid) REFERENCES m_lookup_table;
ALTER TABLE m_node
  ADD CONSTRAINT fk_node FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_object_collection
  ADD CONSTRAINT fk_object_collection FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_object_template
  ADD CONSTRAINT fk_object_template FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_org
  ADD CONSTRAINT fk_org FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE m_report
  ADD CONSTRAINT fk_report FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_report_output
  ADD CONSTRAINT fk_report_output FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_resource
  ADD CONSTRAINT fk_resource FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_role
  ADD CONSTRAINT fk_role FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE m_security_policy
  ADD CONSTRAINT fk_security_policy FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_sequence
  ADD CONSTRAINT fk_sequence FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_service
  ADD CONSTRAINT fk_service FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE m_system_configuration
  ADD CONSTRAINT fk_system_configuration FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_trigger
  ADD CONSTRAINT fk_trigger_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_user
  ADD CONSTRAINT fk_user FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE m_value_policy
  ADD CONSTRAINT fk_value_policy FOREIGN KEY (oid) REFERENCES m_object;

BEGIN TRANSACTION
INSERT INTO m_global_metadata VALUES ('databaseSchemaVersion', '4.0');
COMMIT;

--# thanks to George Papastamatopoulos for submitting this ... and Marko Lahma for
--# updating it.
--#
--# In your Quartz properties file, you'll need to set
--# org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.MSSQLDelegate
--#
--# you shouse enter your DB instance's name on the next line in place of "enter_db_name_here"
--#
--#
--# From a helpful (but anonymous) Quartz user:
--#
--# Regarding this error message:
--#
--#     [Microsoft][SQLServer 2000 Driver for JDBC]Can't start a cloned connection while in manual transaction mode.
--#
--#
--#     I added "SelectMethod=cursor;" to my Connection URL in the config file.
--#     It Seems to work, hopefully no side effects.
--#
--#        example:
--#        "jdbc:microsoft:sqlserver://dbmachine:1433;SelectMethod=cursor";
--#
--# Another user has pointed out that you will probably need to use the
--# JTDS driver
--#
--#
--# USE [enter_db_name_here]
--# GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_TRIGGERS] DROP CONSTRAINT FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] DROP CONSTRAINT FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] DROP CONSTRAINT FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] DROP CONSTRAINT FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_CALENDARS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_CALENDARS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_CRON_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_CRON_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_BLOB_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_BLOB_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_FIRED_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_FIRED_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_PAUSED_TRIGGER_GRPS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SCHEDULER_STATE]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SCHEDULER_STATE];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_LOCKS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_LOCKS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_JOB_DETAILS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_JOB_DETAILS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SIMPLE_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SIMPROP_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_TRIGGERS];
-- GO

CREATE TABLE [dbo].[QRTZ_CALENDARS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [CALENDAR_NAME] [VARCHAR] (200)  NOT NULL ,
  [CALENDAR] [IMAGE] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_CRON_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [CRON_EXPRESSION] [VARCHAR] (120)  NOT NULL ,
  [TIME_ZONE_ID] [VARCHAR] (80)
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_FIRED_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [ENTRY_ID] [VARCHAR] (95)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (200)  NOT NULL ,
  [FIRED_TIME] [BIGINT] NOT NULL ,
  [SCHED_TIME] [BIGINT] NOT NULL ,
  [PRIORITY] [INTEGER] NOT NULL ,
  [EXECUTION_GROUP] [VARCHAR] (200) NULL ,
  [STATE] [VARCHAR] (16)  NOT NULL,
  [JOB_NAME] [VARCHAR] (200)  NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NULL ,
  [IS_NONCONCURRENT] [VARCHAR] (1)  NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SCHEDULER_STATE] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (200)  NOT NULL ,
  [LAST_CHECKIN_TIME] [BIGINT] NOT NULL ,
  [CHECKIN_INTERVAL] [BIGINT] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_LOCKS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [LOCK_NAME] [VARCHAR] (40)  NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_JOB_DETAILS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (200)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (250) NULL ,
  [JOB_CLASS_NAME] [VARCHAR] (250)  NOT NULL ,
  [IS_DURABLE] [VARCHAR] (1)  NOT NULL ,
  [IS_NONCONCURRENT] [VARCHAR] (1)  NOT NULL ,
  [IS_UPDATE_DATA] [VARCHAR] (1)  NOT NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NOT NULL ,
  [JOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [REPEAT_COUNT] [BIGINT] NOT NULL ,
  [REPEAT_INTERVAL] [BIGINT] NOT NULL ,
  [TIMES_TRIGGERED] [BIGINT] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [STR_PROP_1] [VARCHAR] (512) NULL,
  [STR_PROP_2] [VARCHAR] (512) NULL,
  [STR_PROP_3] [VARCHAR] (512) NULL,
  [INT_PROP_1] [INT] NULL,
  [INT_PROP_2] [INT] NULL,
  [LONG_PROP_1] [BIGINT] NULL,
  [LONG_PROP_2] [BIGINT] NULL,
  [DEC_PROP_1] [NUMERIC] (13,4) NULL,
  [DEC_PROP_2] [NUMERIC] (13,4) NULL,
  [BOOL_PROP_1] [VARCHAR] (1) NULL,
  [BOOL_PROP_2] [VARCHAR] (1) NULL,
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_BLOB_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [BLOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (200)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (250) NULL ,
  [NEXT_FIRE_TIME] [BIGINT] NULL ,
  [PREV_FIRE_TIME] [BIGINT] NULL ,
  [PRIORITY] [INTEGER] NULL ,
  [EXECUTION_GROUP] [VARCHAR] (200) NULL ,
  [TRIGGER_STATE] [VARCHAR] (16)  NOT NULL ,
  [TRIGGER_TYPE] [VARCHAR] (8)  NOT NULL ,
  [START_TIME] [BIGINT] NOT NULL ,
  [END_TIME] [BIGINT] NULL ,
  [CALENDAR_NAME] [VARCHAR] (200)  NULL ,
  [MISFIRE_INSTR] [SMALLINT] NULL ,
  [JOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CALENDARS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CALENDARS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [CALENDAR_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CRON_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_FIRED_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_FIRED_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [ENTRY_ID]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_PAUSED_TRIGGER_GRPS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SCHEDULER_STATE] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SCHEDULER_STATE] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [INSTANCE_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_LOCKS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_LOCKS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [LOCK_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_JOB_DETAILS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_JOB_DETAILS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SIMPLE_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SIMPROP_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS] FOREIGN KEY
  (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_JOB_DETAILS] (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  );
-- GO;
