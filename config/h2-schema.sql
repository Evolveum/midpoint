CREATE TABLE m_abstract_role (
  approvalExpression    CLOB,
  approvalProcess       VARCHAR(255),
  approvalSchema        CLOB,
  automaticallyApproved CLOB,
  requestable           BOOLEAN,
  id                    BIGINT      NOT NULL,
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_account_shadow (
  accountType              VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
  passwordXml              CLOB,
  id                       BIGINT      NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_any (
  owner_id   BIGINT      NOT NULL,
  owner_oid  VARCHAR(36) NOT NULL,
  owner_type INTEGER     NOT NULL,
  PRIMARY KEY (owner_id, owner_oid, owner_type)
);

CREATE TABLE m_any_clob (
  checksum                VARCHAR(32)  NOT NULL,
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  dynamicDef              BOOLEAN,
  clobValue               CLOB,
  valueType               INTEGER,
  PRIMARY KEY (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
);

CREATE TABLE m_any_date (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  dateValue               TIMESTAMP    NOT NULL,
  dynamicDef              BOOLEAN,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
);

CREATE TABLE m_any_long (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  longValue               BIGINT       NOT NULL,
  dynamicDef              BOOLEAN,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
);

CREATE TABLE m_any_poly_string (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  orig                    VARCHAR(255) NOT NULL,
  dynamicDef              BOOLEAN,
  norm                    VARCHAR(255),
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
);

CREATE TABLE m_any_reference (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  targetoid               VARCHAR(36)  NOT NULL,
  description             CLOB,
  dynamicDef              BOOLEAN,
  filter                  CLOB,
  relation_namespace      VARCHAR(255),
  relation_localPart      VARCHAR(100),
  targetType              INTEGER,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
);

CREATE TABLE m_any_string (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  stringValue             VARCHAR(255) NOT NULL,
  dynamicDef              BOOLEAN,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
);

CREATE TABLE m_assignment (
  accountConstruction         CLOB,
  administrativeStatus        INTEGER,
  archiveTimestamp            TIMESTAMP,
  disableTimestamp            TIMESTAMP,
  effectiveStatus             INTEGER,
  enableTimestamp             TIMESTAMP,
  validFrom                   TIMESTAMP,
  validTo                     TIMESTAMP,
  validityChangeTimestamp     TIMESTAMP,
  validityStatus              INTEGER,
  assignmentOwner             INTEGER,
  construction                CLOB,
  description                 CLOB,
  owner_id                    BIGINT      NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  targetRef_description       CLOB,
  targetRef_filter            CLOB,
  targetRef_relationLocalPart VARCHAR(100),
  targetRef_relationNamespace VARCHAR(255),
  targetRef_targetOid         VARCHAR(36),
  targetRef_type              INTEGER,
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  extId                       BIGINT,
  extOid                      VARCHAR(36),
  extType                     INTEGER,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_audit_delta (
  checksum         VARCHAR(32) NOT NULL,
  record_id        BIGINT      NOT NULL,
  delta            CLOB,
  deltaOid         VARCHAR(36),
  deltaType        INTEGER,
  details          CLOB,
  localizedMessage CLOB,
  message          CLOB,
  messageCode      VARCHAR(255),
  operation        CLOB,
  params           CLOB,
  partialResults   CLOB,
  status           INTEGER,
  token            BIGINT,
  PRIMARY KEY (checksum, record_id)
);

CREATE TABLE m_audit_event (
  id                BIGINT NOT NULL,
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    VARCHAR(255),
  initiatorName     VARCHAR(255),
  initiatorOid      VARCHAR(36),
  message           VARCHAR(255),
  outcome           INTEGER,
  parameter         VARCHAR(255),
  result            VARCHAR(255),
  sessionIdentifier VARCHAR(255),
  targetName        VARCHAR(255),
  targetOid         VARCHAR(36),
  targetOwnerName   VARCHAR(255),
  targetOwnerOid    VARCHAR(36),
  targetType        INTEGER,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE m_authorization (
  decision    INTEGER,
  description CLOB,
  owner_id    BIGINT      NOT NULL,
  owner_oid   VARCHAR(36) NOT NULL,
  id          BIGINT      NOT NULL,
  oid         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_authorization_action (
  role_id  BIGINT      NOT NULL,
  role_oid VARCHAR(36) NOT NULL,
  action   VARCHAR(255)
);

CREATE TABLE m_connector (
  connectorBundle              VARCHAR(255),
  connectorHostRef_description CLOB,
  connectorHostRef_filter      CLOB,
  c16_relationLocalPart        VARCHAR(100),
  c16_relationNamespace        VARCHAR(255),
  connectorHostRef_targetOid   VARCHAR(36),
  connectorHostRef_type        INTEGER,
  connectorType                VARCHAR(255),
  connectorVersion             VARCHAR(255),
  framework                    VARCHAR(255),
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  namespace                    VARCHAR(255),
  xmlSchema                    CLOB,
  id                           BIGINT      NOT NULL,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_connector_host (
  hostname          VARCHAR(255),
  name_norm         VARCHAR(255),
  name_orig         VARCHAR(255),
  port              VARCHAR(255),
  protectConnection BOOLEAN,
  sharedSecret      CLOB,
  timeout           INTEGER,
  id                BIGINT      NOT NULL,
  oid               VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_connector_target_system (
  connector_id     BIGINT      NOT NULL,
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
);

CREATE TABLE m_container (
  id  BIGINT      NOT NULL,
  oid VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_exclusion (
  description                 CLOB,
  owner_id                    BIGINT      NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  policy                      INTEGER,
  targetRef_description       CLOB,
  targetRef_filter            CLOB,
  targetRef_relationLocalPart VARCHAR(100),
  targetRef_relationNamespace VARCHAR(255),
  targetRef_targetOid         VARCHAR(36),
  targetRef_type              INTEGER,
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_focus (
  administrativeStatus    INTEGER,
  archiveTimestamp        TIMESTAMP,
  disableTimestamp        TIMESTAMP,
  effectiveStatus         INTEGER,
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          INTEGER,
  id                      BIGINT      NOT NULL,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  id         BIGINT      NOT NULL,
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_metadata (
  owner_id                      BIGINT      NOT NULL,
  owner_oid                     VARCHAR(36) NOT NULL,
  createChannel                 VARCHAR(255),
  createTimestamp               TIMESTAMP,
  creatorRef_description        CLOB,
  creatorRef_filter             CLOB,
  creatorRef_relationLocalPart  VARCHAR(100),
  creatorRef_relationNamespace  VARCHAR(255),
  creatorRef_targetOid          VARCHAR(36),
  creatorRef_type               INTEGER,
  modifierRef_description       CLOB,
  modifierRef_filter            CLOB,
  modifierRef_relationLocalPart VARCHAR(100),
  modifierRef_relationNamespace VARCHAR(255),
  modifierRef_targetOid         VARCHAR(36),
  modifierRef_type              INTEGER,
  modifyChannel                 VARCHAR(255),
  modifyTimestamp               TIMESTAMP,
  PRIMARY KEY (owner_id, owner_oid)
);

CREATE TABLE m_node (
  clusteredNode          BOOLEAN,
  hostname               VARCHAR(255),
  internalNodeIdentifier VARCHAR(255),
  jmxPort                INTEGER,
  lastCheckInTime        TIMESTAMP,
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  nodeIdentifier         VARCHAR(255),
  running                BOOLEAN,
  id                     BIGINT      NOT NULL,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_object (
  description CLOB,
  version     BIGINT      NOT NULL,
  id          BIGINT      NOT NULL,
  oid         VARCHAR(36) NOT NULL,
  extId       BIGINT,
  extOid      VARCHAR(36),
  extType     INTEGER,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_object_template (
  accountConstruction CLOB,
  mapping             CLOB,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  type                INTEGER,
  id                  BIGINT      NOT NULL,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_operation_result (
  owner_oid        VARCHAR(36) NOT NULL,
  owner_id         BIGINT      NOT NULL,
  details          CLOB,
  localizedMessage CLOB,
  message          CLOB,
  messageCode      VARCHAR(255),
  operation        CLOB,
  params           CLOB,
  partialResults   CLOB,
  status           INTEGER,
  token            BIGINT,
  PRIMARY KEY (owner_oid, owner_id)
);

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayName_norm VARCHAR(255),
  displayName_orig VARCHAR(255),
  identifier       VARCHAR(255),
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  name_norm        VARCHAR(255),
  name_orig        VARCHAR(255),
  id               BIGINT      NOT NULL,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_org_closure (
  id             BIGINT NOT NULL,
  ancestor_id    BIGINT,
  ancestor_oid   VARCHAR(36),
  depthValue     INTEGER,
  descendant_id  BIGINT,
  descendant_oid VARCHAR(36),
  PRIMARY KEY (id)
);

CREATE TABLE m_org_incorrect (
  descendant_oid VARCHAR(36) NOT NULL,
  descendant_id  BIGINT      NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL,
  PRIMARY KEY (descendant_oid, descendant_id, ancestor_oid)
);

CREATE TABLE m_org_org_type (
  org_id  BIGINT      NOT NULL,
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
);

CREATE TABLE m_password_policy (
  lifetime     CLOB,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  stringPolicy CLOB,
  id           BIGINT      NOT NULL,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_reference (
  reference_type INTEGER      NOT NULL,
  owner_id       BIGINT       NOT NULL,
  owner_oid      VARCHAR(36)  NOT NULL,
  relLocalPart   VARCHAR(100) NOT NULL,
  relNamespace   VARCHAR(255) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  description    CLOB,
  filter         CLOB,
  containerType  INTEGER,
  PRIMARY KEY (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
);

CREATE TABLE m_resource (
  administrativeState            INTEGER,
  capabilities_cachingMetadata   CLOB,
  capabilities_configured        CLOB,
  capabilities_native            CLOB,
  configuration                  CLOB,
  connectorRef_description       CLOB,
  connectorRef_filter            CLOB,
  connectorRef_relationLocalPart VARCHAR(100),
  connectorRef_relationNamespace VARCHAR(255),
  connectorRef_targetOid         VARCHAR(36),
  connectorRef_type              INTEGER,
  consistency                    CLOB,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  namespace                      VARCHAR(255),
  o16_lastAvailabilityStatus     INTEGER,
  projection                     CLOB,
  schemaHandling                 CLOB,
  scripts                        CLOB,
  synchronization                CLOB,
  xmlSchema                      CLOB,
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  id        BIGINT      NOT NULL,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_shadow (
  administrativeStatus          INTEGER,
  archiveTimestamp              TIMESTAMP,
  disableTimestamp              TIMESTAMP,
  effectiveStatus               INTEGER,
  enableTimestamp               TIMESTAMP,
  validFrom                     TIMESTAMP,
  validTo                       TIMESTAMP,
  validityChangeTimestamp       TIMESTAMP,
  validityStatus                INTEGER,
  assigned                      BOOLEAN,
  attemptNumber                 INTEGER,
  dead                          BOOLEAN,
  exist                         BOOLEAN,
  failedOperationType           INTEGER,
  intent                        VARCHAR(255),
  kind                          INTEGER,
  name_norm                     VARCHAR(255),
  name_orig                     VARCHAR(255),
  objectChange                  CLOB,
  class_namespace               VARCHAR(255),
  class_localPart               VARCHAR(100),
  resourceRef_description       CLOB,
  resourceRef_filter            CLOB,
  resourceRef_relationLocalPart VARCHAR(100),
  resourceRef_relationNamespace VARCHAR(255),
  resourceRef_targetOid         VARCHAR(36),
  resourceRef_type              INTEGER,
  synchronizationSituation      INTEGER,
  synchronizationTimestamp      TIMESTAMP,
  id                            BIGINT      NOT NULL,
  oid                           VARCHAR(36) NOT NULL,
  attrId                        BIGINT,
  attrOid                       VARCHAR(36),
  attrType                      INTEGER,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_sync_situation_description (
  checksum       VARCHAR(32) NOT NULL,
  shadow_id      BIGINT      NOT NULL,
  shadow_oid     VARCHAR(36) NOT NULL,
  chanel         VARCHAR(255),
  situation      INTEGER,
  timestampValue TIMESTAMP,
  PRIMARY KEY (checksum, shadow_id, shadow_oid)
);

CREATE TABLE m_system_configuration (
  cleanupPolicy                  CLOB,
  connectorFramework             CLOB,
  d22_description                CLOB,
  defaultUserTemplateRef_filter  CLOB,
  d22_relationLocalPart          VARCHAR(100),
  d22_relationNamespace          VARCHAR(255),
  d22_targetOid                  VARCHAR(36),
  defaultUserTemplateRef_type    INTEGER,
  g36                            CLOB,
  g23_description                CLOB,
  globalPasswordPolicyRef_filter CLOB,
  g23_relationLocalPart          VARCHAR(100),
  g23_relationNamespace          VARCHAR(255),
  g23_targetOid                  VARCHAR(36),
  globalPasswordPolicyRef_type   INTEGER,
  logging                        CLOB,
  modelHooks                     CLOB,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  notificationConfiguration      CLOB,
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_task (
  binding                     INTEGER,
  canRunOnNode                VARCHAR(255),
  category                    VARCHAR(255),
  completionTimestamp         TIMESTAMP,
  executionStatus             INTEGER,
  handlerUri                  VARCHAR(255),
  lastRunFinishTimestamp      TIMESTAMP,
  lastRunStartTimestamp       TIMESTAMP,
  name_norm                   VARCHAR(255),
  name_orig                   VARCHAR(255),
  node                        VARCHAR(255),
  objectRef_description       CLOB,
  objectRef_filter            CLOB,
  objectRef_relationLocalPart VARCHAR(100),
  objectRef_relationNamespace VARCHAR(255),
  objectRef_targetOid         VARCHAR(36),
  objectRef_type              INTEGER,
  otherHandlersUriStack       CLOB,
  ownerRef_description        CLOB,
  ownerRef_filter             CLOB,
  ownerRef_relationLocalPart  VARCHAR(100),
  ownerRef_relationNamespace  VARCHAR(255),
  ownerRef_targetOid          VARCHAR(36),
  ownerRef_type               INTEGER,
  parent                      VARCHAR(255),
  progress                    BIGINT,
  recurrence                  INTEGER,
  resultStatus                INTEGER,
  schedule                    CLOB,
  taskIdentifier              VARCHAR(255),
  threadStopAction            INTEGER,
  waitingReason               INTEGER,
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_task_dependent (
  task_id   BIGINT      NOT NULL,
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
);

CREATE TABLE m_trigger (
  handlerUri     VARCHAR(255),
  owner_id       BIGINT      NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  timestampValue TIMESTAMP,
  id             BIGINT      NOT NULL,
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_user (
  additionalName_norm      VARCHAR(255),
  additionalName_orig      VARCHAR(255),
  costCenter               VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
  passwordXml              CLOB,
  emailAddress             VARCHAR(255),
  employeeNumber           VARCHAR(255),
  familyName_norm          VARCHAR(255),
  familyName_orig          VARCHAR(255),
  fullName_norm            VARCHAR(255),
  fullName_orig            VARCHAR(255),
  givenName_norm           VARCHAR(255),
  givenName_orig           VARCHAR(255),
  honorificPrefix_norm     VARCHAR(255),
  honorificPrefix_orig     VARCHAR(255),
  honorificSuffix_norm     VARCHAR(255),
  honorificSuffix_orig     VARCHAR(255),
  locale                   VARCHAR(255),
  locality_norm            VARCHAR(255),
  locality_orig            VARCHAR(255),
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  nickName_norm            VARCHAR(255),
  nickName_orig            VARCHAR(255),
  preferredLanguage        VARCHAR(255),
  telephoneNumber          VARCHAR(255),
  timezone                 VARCHAR(255),
  title_norm               VARCHAR(255),
  title_orig               VARCHAR(255),
  id                       BIGINT      NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_user_employee_type (
  user_id      BIGINT      NOT NULL,
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
);

CREATE TABLE m_user_organization (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);

CREATE TABLE m_user_organizational_unit (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);

CREATE INDEX iRequestable ON m_abstract_role (requestable);

ALTER TABLE m_abstract_role
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (id, oid)
REFERENCES m_focus;

ALTER TABLE m_account_shadow
ADD CONSTRAINT fk_account_shadow
FOREIGN KEY (id, oid)
REFERENCES m_shadow;

ALTER TABLE m_any_clob
ADD CONSTRAINT fk_any_clob
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iDate ON m_any_date (dateValue);

ALTER TABLE m_any_date
ADD CONSTRAINT fk_any_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iLong ON m_any_long (longValue);

ALTER TABLE m_any_long
ADD CONSTRAINT fk_any_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iPolyString ON m_any_poly_string (orig);

ALTER TABLE m_any_poly_string
ADD CONSTRAINT fk_any_poly_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iTargetOid ON m_any_reference (targetoid);

ALTER TABLE m_any_reference
ADD CONSTRAINT fk_any_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iString ON m_any_string (stringValue);

ALTER TABLE m_any_string
ADD CONSTRAINT fk_any_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

ALTER TABLE m_assignment
ADD CONSTRAINT fk_assignment
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_assignment
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

ALTER TABLE m_audit_delta
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_authorization
ADD CONSTRAINT fk_authorization
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_authorization
ADD CONSTRAINT fk_authorization_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

ALTER TABLE m_authorization_action
ADD CONSTRAINT fk_authorization_action
FOREIGN KEY (role_id, role_oid)
REFERENCES m_authorization;

CREATE INDEX iConnectorName ON m_connector (name_norm);

ALTER TABLE m_connector
ADD CONSTRAINT fk_connector
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_connector_host
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_connector_target_system
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_id, connector_oid)
REFERENCES m_connector;

ALTER TABLE m_exclusion
ADD CONSTRAINT fk_exclusion
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_exclusion
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_focus
ADD CONSTRAINT fk_focus
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_generic_object
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_metadata
ADD CONSTRAINT fk_metadata_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container;

ALTER TABLE m_node
ADD CONSTRAINT fk_node
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_object
ADD CONSTRAINT fk_object
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_object_template
ADD CONSTRAINT fk_object_template
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_operation_result
ADD CONSTRAINT fk_result_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

ALTER TABLE m_org
ADD CONSTRAINT fk_org
FOREIGN KEY (id, oid)
REFERENCES m_abstract_role;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_id, descendant_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_id, ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_org_type
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_id, org_oid)
REFERENCES m_org;

ALTER TABLE m_password_policy
ADD CONSTRAINT fk_password_policy
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_reference
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container;

ALTER TABLE m_resource
ADD CONSTRAINT fk_resource
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_role
ADD CONSTRAINT fk_role
FOREIGN KEY (id, oid)
REFERENCES m_abstract_role;

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowAdministrative ON m_shadow (administrativeStatus);

CREATE INDEX iShadowEffective ON m_shadow (effectiveStatus);

CREATE INDEX iShadowName ON m_shadow (name_norm);

ALTER TABLE m_shadow
ADD CONSTRAINT fk_shadow
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_sync_situation_description
ADD CONSTRAINT fk_shadow_sync_situation
FOREIGN KEY (shadow_id, shadow_oid)
REFERENCES m_shadow;

ALTER TABLE m_system_configuration
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iTaskName ON m_task (name_norm);

ALTER TABLE m_task
ADD CONSTRAINT fk_task
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_task_dependent
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_id, task_oid)
REFERENCES m_task;

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

CREATE INDEX iFullName ON m_user (fullName_norm);

CREATE INDEX iLocality ON m_user (locality_norm);

CREATE INDEX iHonorificSuffix ON m_user (honorificSuffix_norm);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iGivenName ON m_user (givenName_norm);

CREATE INDEX iFamilyName ON m_user (familyName_norm);

CREATE INDEX iAdditionalName ON m_user (additionalName_norm);

CREATE INDEX iHonorificPrefix ON m_user (honorificPrefix_norm);

ALTER TABLE m_user
ADD CONSTRAINT fk_user
FOREIGN KEY (id, oid)
REFERENCES m_focus;

ALTER TABLE m_user_employee_type
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organization
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organizational_unit
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user;

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
