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

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE m_audit_event
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE m_audit_prop_value
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE m_audit_ref_value
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE m_audit_delta
  MODIFY COLUMN delta LONGBLOB,
  MODIFY COLUMN fullResult LONGBLOB;

ALTER TABLE m_audit_event
  ADD COLUMN targetOwnerType INTEGER;

CREATE TABLE m_acc_cert_campaign (
  definitionRef_relation  VARCHAR(157),
  definitionRef_targetOid VARCHAR(36),
  definitionRef_type      INTEGER,
  endTimestamp            DATETIME(6),
  handlerUri              VARCHAR(255),
  name_norm               VARCHAR(255),
  name_orig               VARCHAR(255),
  ownerRef_relation       VARCHAR(157),
  ownerRef_targetOid      VARCHAR(36),
  ownerRef_type           INTEGER,
  stageNumber             INTEGER,
  startTimestamp          DATETIME(6),
  state                   INTEGER,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
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
  currentStageOutcome      VARCHAR(255),
  fullObject               LONGBLOB,
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INTEGER,
  orgRef_relation          VARCHAR(157),
  orgRef_targetOid         VARCHAR(36),
  orgRef_type              INTEGER,
  outcome                  VARCHAR(255),
  remediedTimestamp        DATETIME(6),
  reviewDeadline           DATETIME(6),
  reviewRequestedTimestamp DATETIME(6),
  stageNumber              INTEGER,
  targetRef_relation       VARCHAR(157),
  targetRef_targetOid      VARCHAR(36),
  targetRef_type           INTEGER,
  tenantRef_relation       VARCHAR(157),
  tenantRef_targetOid      VARCHAR(36),
  tenantRef_type           INTEGER,
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_acc_cert_definition (
  handlerUri                   VARCHAR(255),
  lastCampaignClosedTimestamp  DATETIME(6),
  lastCampaignStartedTimestamp DATETIME(6),
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  ownerRef_relation            VARCHAR(157),
  ownerRef_targetOid           VARCHAR(36),
  ownerRef_type                INTEGER,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
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
  PRIMARY KEY (owner_owner_oid, owner_id, id)
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
  PRIMARY KEY (owner_owner_owner_oid, owner_owner_id, owner_id, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment (
  id                      INTEGER     NOT NULL,
  owner_oid               VARCHAR(36) NOT NULL,
  administrativeStatus    INTEGER,
  archiveTimestamp        DATETIME(6),
  disableReason           VARCHAR(255),
  disableTimestamp        DATETIME(6),
  effectiveStatus         INTEGER,
  enableTimestamp         DATETIME(6),
  validFrom               DATETIME(6),
  validTo                 DATETIME(6),
  validityChangeTimestamp DATETIME(6),
  validityStatus          INTEGER,
  assignmentOwner         INTEGER,
  createChannel           VARCHAR(255),
  createTimestamp         DATETIME(6),
  creatorRef_relation     VARCHAR(157),
  creatorRef_targetOid    VARCHAR(36),
  creatorRef_type         INTEGER,
  lifecycleState          VARCHAR(255),
  modifierRef_relation    VARCHAR(157),
  modifierRef_targetOid   VARCHAR(36),
  modifierRef_type        INTEGER,
  modifyChannel           VARCHAR(255),
  modifyTimestamp         DATETIME(6),
  orderValue              INTEGER,
  orgRef_relation         VARCHAR(157),
  orgRef_targetOid        VARCHAR(36),
  orgRef_type             INTEGER,
  resourceRef_relation    VARCHAR(157),
  resourceRef_targetOid   VARCHAR(36),
  resourceRef_type        INTEGER,
  targetRef_relation      VARCHAR(157),
  targetRef_targetOid     VARCHAR(36),
  targetRef_type          INTEGER,
  tenantRef_relation      VARCHAR(157),
  tenantRef_targetOid     VARCHAR(36),
  tenantRef_type          INTEGER,
  extId                   INTEGER,
  extOid                  VARCHAR(36),
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_boolean (
  item_id                      INTEGER     NOT NULL,
  anyContainer_owner_id        INTEGER     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  booleanValue                 BIT         NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_date (
  item_id                      INTEGER     NOT NULL,
  anyContainer_owner_id        INTEGER     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  dateValue                    DATETIME(6) NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_long (
  item_id                      INTEGER     NOT NULL,
  anyContainer_owner_id        INTEGER     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  longValue                    BIGINT      NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_poly (
  item_id                      INTEGER      NOT NULL,
  anyContainer_owner_id        INTEGER      NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  norm                         VARCHAR(255),
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_reference (
  item_id                      INTEGER     NOT NULL,
  anyContainer_owner_id        INTEGER     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  targetoid                    VARCHAR(36) NOT NULL,
  relation                     VARCHAR(157),
  targetType                   INTEGER,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_ext_string (
  item_id                      INTEGER      NOT NULL,
  anyContainer_owner_id        INTEGER      NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_extension (
  owner_id        INTEGER     NOT NULL,
  owner_owner_oid VARCHAR(36) NOT NULL,
  booleansCount   SMALLINT,
  datesCount      SMALLINT,
  longsCount      SMALLINT,
  polysCount      SMALLINT,
  referencesCount SMALLINT,
  stringsCount    SMALLINT,
  PRIMARY KEY (owner_owner_oid, owner_id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_policy_situation (
  assignment_id   INTEGER     NOT NULL,
  assignment_oid  VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_assignment_reference (
  owner_id        INTEGER      NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INTEGER      NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_case_wi (
  id                            INTEGER     NOT NULL,
  owner_oid                     VARCHAR(36) NOT NULL,
  closeTimestamp                DATETIME(6),
  deadline                      DATETIME(6),
  originalAssigneeRef_relation  VARCHAR(157),
  originalAssigneeRef_targetOid VARCHAR(36),
  originalAssigneeRef_type      INTEGER,
  outcome                       VARCHAR(255),
  performerRef_relation         VARCHAR(157),
  performerRef_targetOid        VARCHAR(36),
  performerRef_type             INTEGER,
  stageNumber                   INTEGER,
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_case_wi_reference (
  owner_id        INTEGER      NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_owner_oid, owner_id, targetOid, relation)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_ext_item (
  id       INTEGER NOT NULL AUTO_INCREMENT,
  kind     INTEGER,
  itemName VARCHAR(157),
  itemType VARCHAR(157),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_focus_photo (
  owner_oid VARCHAR(36) NOT NULL,
  photo     LONGBLOB,
  PRIMARY KEY (owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object (
  oid                   VARCHAR(36) NOT NULL,
  booleansCount         SMALLINT,
  createChannel         VARCHAR(255),
  createTimestamp       DATETIME(6),
  creatorRef_relation   VARCHAR(157),
  creatorRef_targetOid  VARCHAR(36),
  creatorRef_type       INTEGER,
  datesCount            SMALLINT,
  fullObject            LONGBLOB,
  lifecycleState        VARCHAR(255),
  longsCount            SMALLINT,
  modifierRef_relation  VARCHAR(157),
  modifierRef_targetOid VARCHAR(36),
  modifierRef_type      INTEGER,
  modifyChannel         VARCHAR(255),
  modifyTimestamp       DATETIME(6),
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  objectTypeClass       INTEGER,
  polysCount            SMALLINT,
  referencesCount       SMALLINT,
  stringsCount          SMALLINT,
  tenantRef_relation    VARCHAR(157),
  tenantRef_targetOid   VARCHAR(36),
  tenantRef_type        INTEGER,
  version               INTEGER     NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_boolean (
  item_id      INTEGER     NOT NULL,
  owner_oid    VARCHAR(36) NOT NULL,
  ownerType    INTEGER     NOT NULL,
  booleanValue BIT         NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, booleanValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_date (
  item_id   INTEGER     NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INTEGER     NOT NULL,
  dateValue DATETIME(6) NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_long (
  item_id   INTEGER     NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INTEGER     NOT NULL,
  longValue BIGINT      NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_poly (
  item_id   INTEGER      NOT NULL,
  owner_oid VARCHAR(36)  NOT NULL,
  ownerType INTEGER      NOT NULL,
  orig      VARCHAR(255) NOT NULL,
  norm      VARCHAR(255),
  PRIMARY KEY (owner_oid, ownerType, item_id, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_reference (
  item_id    INTEGER     NOT NULL,
  owner_oid  VARCHAR(36) NOT NULL,
  ownerType  INTEGER     NOT NULL,
  targetoid  VARCHAR(36) NOT NULL,
  relation   VARCHAR(157),
  targetType INTEGER,
  PRIMARY KEY (owner_oid, ownerType, item_id, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_ext_string (
  item_id     INTEGER      NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INTEGER      NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_subtype (
  object_oid VARCHAR(36) NOT NULL,
  subtype    VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_text_info (
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
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
  timestampValue         DATETIME(6),
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_org_closure (
  ancestor_oid   VARCHAR(36) NOT NULL,
  descendant_oid VARCHAR(36) NOT NULL,
  val            INTEGER,
  PRIMARY KEY (ancestor_oid, descendant_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_org_org_type (
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_reference (
  owner_oid      VARCHAR(36)  NOT NULL,
  reference_type INTEGER      NOT NULL,
  relation       VARCHAR(157) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  targetType     INTEGER,
  PRIMARY KEY (owner_oid, reference_type, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_service_type (
  service_oid VARCHAR(36) NOT NULL,
  serviceType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_shadow (
  attemptNumber                INTEGER,
  dead                         BIT,
  exist                        BIT,
  failedOperationType          INTEGER,
  fullSynchronizationTimestamp DATETIME(6),
  intent                       VARCHAR(255),
  kind                         INTEGER,
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  objectClass                  VARCHAR(157),
  pendingOperationCount        INTEGER,
  resourceRef_relation         VARCHAR(157),
  resourceRef_targetOid        VARCHAR(36),
  resourceRef_type             INTEGER,
  status                       INTEGER,
  synchronizationSituation     INTEGER,
  synchronizationTimestamp     DATETIME(6),
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_task (
  binding                  INTEGER,
  canRunOnNode             VARCHAR(255),
  category                 VARCHAR(255),
  completionTimestamp      DATETIME(6),
  executionStatus          INTEGER,
  fullResult               LONGBLOB,
  handlerUri               VARCHAR(255),
  lastRunFinishTimestamp   DATETIME(6),
  lastRunStartTimestamp    DATETIME(6),
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  node                     VARCHAR(255),
  objectRef_relation       VARCHAR(157),
  objectRef_targetOid      VARCHAR(36),
  objectRef_type           INTEGER,
  ownerRef_relation        VARCHAR(157),
  ownerRef_targetOid       VARCHAR(36),
  ownerRef_type            INTEGER,
  parent                   VARCHAR(255),
  recurrence               INTEGER,
  status                   INTEGER,
  taskIdentifier           VARCHAR(255),
  threadStopAction         INTEGER,
  waitingReason            INTEGER,
  wfEndTimestamp           DATETIME(6),
  wfObjectRef_relation     VARCHAR(157),
  wfObjectRef_targetOid    VARCHAR(36),
  wfObjectRef_type         INTEGER,
  wfProcessInstanceId      VARCHAR(255),
  wfRequesterRef_relation  VARCHAR(157),
  wfRequesterRef_targetOid VARCHAR(36),
  wfRequesterRef_type      INTEGER,
  wfStartTimestamp         DATETIME(6),
  wfTargetRef_relation     VARCHAR(157),
  wfTargetRef_targetOid    VARCHAR(36),
  wfTargetRef_type         INTEGER,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_task_dependent (
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_user_organization (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_abstract_role (
  approvalProcess    VARCHAR(255),
  autoassign_enabled BIT,
  displayName_norm   VARCHAR(255),
  displayName_orig   VARCHAR(255),
  identifier         VARCHAR(255),
  ownerRef_relation  VARCHAR(157),
  ownerRef_targetOid VARCHAR(36),
  ownerRef_type      INTEGER,
  requestable        BIT,
  riskLevel          VARCHAR(255),
  oid                VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_case (
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  objectRef_relation  VARCHAR(157),
  objectRef_targetOid VARCHAR(36),
  objectRef_type      INTEGER,
  state               VARCHAR(255),
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_connector (
  connectorBundle            VARCHAR(255),
  connectorHostRef_relation  VARCHAR(157),
  connectorHostRef_targetOid VARCHAR(36),
  connectorHostRef_type      INTEGER,
  connectorType              VARCHAR(255),
  connectorVersion           VARCHAR(255),
  framework                  VARCHAR(255),
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_connector_host (
  hostname  VARCHAR(255),
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  port      VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_focus (
  administrativeStatus    INTEGER,
  archiveTimestamp        DATETIME(6),
  disableReason           VARCHAR(255),
  disableTimestamp        DATETIME(6),
  effectiveStatus         INTEGER,
  enableTimestamp         DATETIME(6),
  validFrom               DATETIME(6),
  validTo                 DATETIME(6),
  validityChangeTimestamp DATETIME(6),
  validityStatus          INTEGER,
  costCenter              VARCHAR(255),
  emailAddress            VARCHAR(255),
  hasPhoto                BIT DEFAULT FALSE NOT NULL,
  locale                  VARCHAR(255),
  locality_norm           VARCHAR(255),
  locality_orig           VARCHAR(255),
  preferredLanguage       VARCHAR(255),
  telephoneNumber         VARCHAR(255),
  timezone                VARCHAR(255),
  oid                     VARCHAR(36)       NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_function_library (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_lookup_table (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_lookup_table_row (
  id                  INTEGER     NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  row_key             VARCHAR(255),
  label_norm          VARCHAR(255),
  label_orig          VARCHAR(255),
  lastChangeTimestamp DATETIME(6),
  row_value           VARCHAR(255),
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_node (
  name_norm      VARCHAR(255),
  name_orig      VARCHAR(255),
  nodeIdentifier VARCHAR(255),
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INTEGER,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_org (
  displayOrder INTEGER,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  tenant       BIT,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_report (
  export              INTEGER,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  orientation         INTEGER,
  parent              BIT,
  useHibernateSession BIT,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_report_output (
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  reportRef_relation  VARCHAR(157),
  reportRef_targetOid VARCHAR(36),
  reportRef_type      INTEGER,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_resource (
  administrativeState        INTEGER,
  connectorRef_relation      VARCHAR(157),
  connectorRef_targetOid     VARCHAR(36),
  connectorRef_type          INTEGER,
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  o16_lastAvailabilityStatus INTEGER,
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_security_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_sequence (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_service (
  displayOrder INTEGER,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_system_configuration (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_trigger (
  id             INTEGER     NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue DATETIME(6),
  PRIMARY KEY (owner_oid, id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
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
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
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
CREATE INDEX iAuditItemRecordId
  ON m_audit_item (record_id);
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
-- create index iAncestor on m_org_closure (ancestor_oid);
-- create index iDescendant on m_org_closure (descendant_oid);
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
ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);
CREATE INDEX iAbstractRoleIdentifier
  ON m_abstract_role (identifier);
CREATE INDEX iRequestable
  ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);
CREATE INDEX iCaseNameOrig
  ON m_case (name_orig);
ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);
CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig);
CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm);
CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig);
ALTER TABLE m_connector_host
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
  ADD CONSTRAINT fk_acc_cert_campaign FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner FOREIGN KEY (owner_oid) REFERENCES m_acc_cert_campaign (oid);
ALTER TABLE m_acc_cert_definition
  ADD CONSTRAINT fk_acc_cert_definition FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_acc_cert_case (owner_oid, id);
ALTER TABLE m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner FOREIGN KEY (owner_owner_owner_oid, owner_owner_id, owner_id) REFERENCES m_acc_cert_wi (owner_owner_oid, owner_id, id);
ALTER TABLE m_assignment
  ADD CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_boolean_reference FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension (owner_owner_oid, owner_id);
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation FOREIGN KEY (assignment_oid, assignment_id) REFERENCES m_assignment (owner_oid, id);
ALTER TABLE m_assignment_reference
  ADD CONSTRAINT fk_assignment_reference FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_assignment (owner_oid, id);
ALTER TABLE m_case_wi
  ADD CONSTRAINT fk_case_wi_owner FOREIGN KEY (owner_oid) REFERENCES m_case (oid);
ALTER TABLE m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi (owner_oid, id);
ALTER TABLE m_connector_target_system
  ADD CONSTRAINT fk_connector_target_system FOREIGN KEY (connector_oid) REFERENCES m_connector (oid);
ALTER TABLE m_focus_photo
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (owner_oid) REFERENCES m_focus (oid);
ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation FOREIGN KEY (focus_oid) REFERENCES m_focus (oid);
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_object_ext_long FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_o_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_object_ext_string FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_o_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item (id);
ALTER TABLE m_object_subtype
  ADD CONSTRAINT fk_object_subtype FOREIGN KEY (object_oid) REFERENCES m_object (oid);
ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor_oid) REFERENCES m_object (oid);
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant_oid) REFERENCES m_object (oid);
ALTER TABLE m_org_org_type
  ADD CONSTRAINT fk_org_org_type FOREIGN KEY (org_oid) REFERENCES m_org (oid);
ALTER TABLE m_reference
  ADD CONSTRAINT fk_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type FOREIGN KEY (service_oid) REFERENCES m_service (oid);
ALTER TABLE m_shadow
  ADD CONSTRAINT fk_shadow FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_task
  ADD CONSTRAINT fk_task FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_task_dependent
  ADD CONSTRAINT fk_task_dependent FOREIGN KEY (task_oid) REFERENCES m_task (oid);
ALTER TABLE m_user_employee_type
  ADD CONSTRAINT fk_user_employee_type FOREIGN KEY (user_oid) REFERENCES m_user (oid);
ALTER TABLE m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user (oid);
ALTER TABLE m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user (oid);
ALTER TABLE m_abstract_role
  ADD CONSTRAINT fk_abstract_role FOREIGN KEY (oid) REFERENCES m_focus (oid);
ALTER TABLE m_case
  ADD CONSTRAINT fk_case FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_connector
  ADD CONSTRAINT fk_connector FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_connector_host
  ADD CONSTRAINT fk_connector_host FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_focus
  ADD CONSTRAINT fk_focus FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_form
  ADD CONSTRAINT fk_form FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_lookup_table
  ADD CONSTRAINT fk_lookup_table FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_lookup_table_row
  ADD CONSTRAINT fk_lookup_table_owner FOREIGN KEY (owner_oid) REFERENCES m_lookup_table (oid);
ALTER TABLE m_node
  ADD CONSTRAINT fk_node FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_object_template
  ADD CONSTRAINT fk_object_template FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_org
  ADD CONSTRAINT fk_org FOREIGN KEY (oid) REFERENCES m_abstract_role (oid);
ALTER TABLE m_report
  ADD CONSTRAINT fk_report FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_report_output
  ADD CONSTRAINT fk_report_output FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_resource
  ADD CONSTRAINT fk_resource FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_role
  ADD CONSTRAINT fk_role FOREIGN KEY (oid) REFERENCES m_abstract_role (oid);
ALTER TABLE m_security_policy
  ADD CONSTRAINT fk_security_policy FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_sequence
  ADD CONSTRAINT fk_sequence FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_service
  ADD CONSTRAINT fk_service FOREIGN KEY (oid) REFERENCES m_abstract_role (oid);
ALTER TABLE m_system_configuration
  ADD CONSTRAINT fk_system_configuration FOREIGN KEY (oid) REFERENCES m_object (oid);
ALTER TABLE m_trigger
  ADD CONSTRAINT fk_trigger_owner FOREIGN KEY (owner_oid) REFERENCES m_object (oid);
ALTER TABLE m_user
  ADD CONSTRAINT fk_user FOREIGN KEY (oid) REFERENCES m_focus (oid);
ALTER TABLE m_value_policy
  ADD CONSTRAINT fk_value_policy FOREIGN KEY (oid) REFERENCES m_object (oid);

commit;