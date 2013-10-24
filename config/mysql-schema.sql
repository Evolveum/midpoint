# use for db create
# CREATE DATABASE <database name>
#   CHARACTER SET utf8
#   DEFAULT CHARACTER SET utf8
#   COLLATE utf8_bin
#   DEFAULT COLLATE utf8_bin
# ;

# replace "ENGINE=InnoDB" with "DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB"
# replace "DATETIME" with "DATETIME(6)"

CREATE TABLE m_abstract_role (
  approvalExpression    LONGTEXT,
  approvalProcess       VARCHAR(255),
  approvalSchema        LONGTEXT,
  automaticallyApproved LONGTEXT,
  requestable           BIT,
  id                    BIGINT      NOT NULL,
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any (
  owner_id   BIGINT      NOT NULL,
  owner_oid  VARCHAR(36) NOT NULL,
  owner_type INTEGER     NOT NULL,
  PRIMARY KEY (owner_id, owner_oid, owner_type)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_clob (
  checksum                VARCHAR(32)  NOT NULL,
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  dynamicDef              BIT,
  clobValue               LONGTEXT,
  valueType               INTEGER,
  PRIMARY KEY (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_date (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  dateValue               DATETIME(6)  NOT NULL,
  dynamicDef              BIT,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_long (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  longValue               BIGINT       NOT NULL,
  dynamicDef              BIT,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_poly_string (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  orig                    VARCHAR(255) NOT NULL,
  dynamicDef              BIT,
  norm                    VARCHAR(255),
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_reference (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  targetoid               VARCHAR(36)  NOT NULL,
  description             LONGTEXT,
  dynamicDef              BIT,
  filter                  LONGTEXT,
  relation_namespace      VARCHAR(255),
  relation_localPart      VARCHAR(100),
  targetType              INTEGER,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_any_string (
  name_namespace          VARCHAR(255) NOT NULL,
  name_localPart          VARCHAR(100) NOT NULL,
  anyContainer_owner_id   BIGINT       NOT NULL,
  anyContainer_owner_oid  VARCHAR(36)  NOT NULL,
  anyContainer_owner_type INTEGER      NOT NULL,
  type_namespace          VARCHAR(255) NOT NULL,
  type_localPart          VARCHAR(100) NOT NULL,
  stringValue             VARCHAR(255) NOT NULL,
  dynamicDef              BIT,
  valueType               INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment (
  accountConstruction         LONGTEXT,
  administrativeStatus        INTEGER,
  archiveTimestamp            DATETIME(6),
  disableTimestamp            DATETIME(6),
  effectiveStatus             INTEGER,
  enableTimestamp             DATETIME(6),
  validFrom                   DATETIME(6),
  validTo                     DATETIME(6),
  validityChangeTimestamp     DATETIME(6),
  validityStatus              INTEGER,
  assignmentOwner             INTEGER,
  construction                LONGTEXT,
  description                 LONGTEXT,
  owner_id                    BIGINT      NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  targetRef_description       LONGTEXT,
  targetRef_filter            LONGTEXT,
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
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_audit_delta (
  checksum         VARCHAR(32) NOT NULL,
  record_id        BIGINT      NOT NULL,
  delta            LONGTEXT,
  deltaOid         VARCHAR(36),
  deltaType        INTEGER,
  details          LONGTEXT,
  localizedMessage LONGTEXT,
  message          LONGTEXT,
  messageCode      VARCHAR(255),
  operation        LONGTEXT,
  params           LONGTEXT,
  partialResults   LONGTEXT,
  status           INTEGER,
  token            BIGINT,
  PRIMARY KEY (checksum, record_id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_audit_event (
  id                BIGINT NOT NULL,
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    VARCHAR(255),
  initiatorName     VARCHAR(255),
  initiatorOid      VARCHAR(36),
  message           VARCHAR(1024),
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
  timestampValue    DATETIME(6),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_authorization (
  decision    INTEGER,
  description LONGTEXT,
  owner_id    BIGINT      NOT NULL,
  owner_oid   VARCHAR(36) NOT NULL,
  id          BIGINT      NOT NULL,
  oid         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_authorization_action (
  role_id  BIGINT      NOT NULL,
  role_oid VARCHAR(36) NOT NULL,
  action   VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector (
  connectorBundle              VARCHAR(255),
  connectorHostRef_description LONGTEXT,
  connectorHostRef_filter      LONGTEXT,
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
  xmlSchema                    LONGTEXT,
  id                           BIGINT      NOT NULL,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector_host (
  hostname          VARCHAR(255),
  name_norm         VARCHAR(255),
  name_orig         VARCHAR(255),
  port              VARCHAR(255),
  protectConnection BIT,
  sharedSecret      LONGTEXT,
  timeout           INTEGER,
  id                BIGINT      NOT NULL,
  oid               VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector_target_system (
  connector_id     BIGINT      NOT NULL,
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_container (
  id  BIGINT      NOT NULL,
  oid VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_exclusion (
  description                 LONGTEXT,
  owner_id                    BIGINT      NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  policy                      INTEGER,
  targetRef_description       LONGTEXT,
  targetRef_filter            LONGTEXT,
  targetRef_relationLocalPart VARCHAR(100),
  targetRef_relationNamespace VARCHAR(255),
  targetRef_targetOid         VARCHAR(36),
  targetRef_type              INTEGER,
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_focus (
  administrativeStatus    INTEGER,
  archiveTimestamp        DATETIME(6),
  disableTimestamp        DATETIME(6),
  effectiveStatus         INTEGER,
  enableTimestamp         DATETIME(6),
  validFrom               DATETIME(6),
  validTo                 DATETIME(6),
  validityChangeTimestamp DATETIME(6),
  validityStatus          INTEGER,
  id                      BIGINT      NOT NULL,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  id         BIGINT      NOT NULL,
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_metadata (
  owner_id                      BIGINT      NOT NULL,
  owner_oid                     VARCHAR(36) NOT NULL,
  createChannel                 VARCHAR(255),
  createTimestamp               DATETIME(6),
  creatorRef_description        LONGTEXT,
  creatorRef_filter             LONGTEXT,
  creatorRef_relationLocalPart  VARCHAR(100),
  creatorRef_relationNamespace  VARCHAR(255),
  creatorRef_targetOid          VARCHAR(36),
  creatorRef_type               INTEGER,
  modifierRef_description       LONGTEXT,
  modifierRef_filter            LONGTEXT,
  modifierRef_relationLocalPart VARCHAR(100),
  modifierRef_relationNamespace VARCHAR(255),
  modifierRef_targetOid         VARCHAR(36),
  modifierRef_type              INTEGER,
  modifyChannel                 VARCHAR(255),
  modifyTimestamp               DATETIME(6),
  PRIMARY KEY (owner_id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_node (
  clusteredNode          BIT,
  hostname               VARCHAR(255),
  internalNodeIdentifier VARCHAR(255),
  jmxPort                INTEGER,
  lastCheckInTime        DATETIME(6),
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  nodeIdentifier         VARCHAR(255),
  running                BIT,
  id                     BIGINT      NOT NULL,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object (
  description LONGTEXT,
  version     BIGINT      NOT NULL,
  id          BIGINT      NOT NULL,
  oid         VARCHAR(36) NOT NULL,
  extId       BIGINT,
  extOid      VARCHAR(36),
  extType     INTEGER,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_template (
  accountConstruction LONGTEXT,
  mapping             LONGTEXT,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  type                INTEGER,
  id                  BIGINT      NOT NULL,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_operation_result (
  owner_oid        VARCHAR(36) NOT NULL,
  owner_id         BIGINT      NOT NULL,
  details          LONGTEXT,
  localizedMessage LONGTEXT,
  message          LONGTEXT,
  messageCode      VARCHAR(255),
  operation        LONGTEXT,
  params           LONGTEXT,
  partialResults   LONGTEXT,
  status           INTEGER,
  token            BIGINT,
  PRIMARY KEY (owner_oid, owner_id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

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
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_closure (
  id             BIGINT NOT NULL,
  ancestor_id    BIGINT,
  ancestor_oid   VARCHAR(36),
  depthValue     INTEGER,
  descendant_id  BIGINT,
  descendant_oid VARCHAR(36),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_incorrect (
  descendant_oid VARCHAR(36) NOT NULL,
  descendant_id  BIGINT      NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL,
  PRIMARY KEY (descendant_oid, descendant_id, ancestor_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_org_type (
  org_id  BIGINT      NOT NULL,
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_reference (
  reference_type INTEGER      NOT NULL,
  owner_id       BIGINT       NOT NULL,
  owner_oid      VARCHAR(36)  NOT NULL,
  relLocalPart   VARCHAR(100) NOT NULL,
  relNamespace   VARCHAR(255) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  description    LONGTEXT,
  filter         LONGTEXT,
  containerType  INTEGER,
  PRIMARY KEY (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_resource (
  administrativeState            INTEGER,
  capabilities_cachingMetadata   LONGTEXT,
  capabilities_configured        LONGTEXT,
  capabilities_native            LONGTEXT,
  configuration                  LONGTEXT,
  connectorRef_description       LONGTEXT,
  connectorRef_filter            LONGTEXT,
  connectorRef_relationLocalPart VARCHAR(100),
  connectorRef_relationNamespace VARCHAR(255),
  connectorRef_targetOid         VARCHAR(36),
  connectorRef_type              INTEGER,
  consistency                    LONGTEXT,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  namespace                      VARCHAR(255),
  o16_lastAvailabilityStatus     INTEGER,
  projection                     LONGTEXT,
  schemaHandling                 LONGTEXT,
  scripts                        LONGTEXT,
  synchronization                LONGTEXT,
  xmlSchema                      LONGTEXT,
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  id        BIGINT      NOT NULL,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_shadow (
  administrativeStatus          INTEGER,
  archiveTimestamp              DATETIME(6),
  disableTimestamp              DATETIME(6),
  effectiveStatus               INTEGER,
  enableTimestamp               DATETIME(6),
  validFrom                     DATETIME(6),
  validTo                       DATETIME(6),
  validityChangeTimestamp       DATETIME(6),
  validityStatus                INTEGER,
  assigned                      BIT,
  attemptNumber                 INTEGER,
  dead                          BIT,
  exist                         BIT,
  failedOperationType           INTEGER,
  fullSynchronizationTimestamp  DATETIME(6),
  intent                        VARCHAR(255),
  iteration                     INTEGER,
  iterationToken                VARCHAR(255),
  kind                          INTEGER,
  name_norm                     VARCHAR(255),
  name_orig                     VARCHAR(255),
  objectChange                  LONGTEXT,
  class_namespace               VARCHAR(255),
  class_localPart               VARCHAR(100),
  resourceRef_description       LONGTEXT,
  resourceRef_filter            LONGTEXT,
  resourceRef_relationLocalPart VARCHAR(100),
  resourceRef_relationNamespace VARCHAR(255),
  resourceRef_targetOid         VARCHAR(36),
  resourceRef_type              INTEGER,
  synchronizationSituation      INTEGER,
  synchronizationTimestamp      DATETIME(6),
  id                            BIGINT      NOT NULL,
  oid                           VARCHAR(36) NOT NULL,
  attrId                        BIGINT,
  attrOid                       VARCHAR(36),
  attrType                      INTEGER,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_sync_situation_description (
  checksum       VARCHAR(32) NOT NULL,
  shadow_id      BIGINT      NOT NULL,
  shadow_oid     VARCHAR(36) NOT NULL,
  chanel         VARCHAR(255),
  fullFlag       BIT,
  situation      INTEGER,
  timestampValue DATETIME(6),
  PRIMARY KEY (checksum, shadow_id, shadow_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_system_configuration (
  cleanupPolicy                  LONGTEXT,
  connectorFramework             LONGTEXT,
  d22_description                LONGTEXT,
  defaultUserTemplateRef_filter  LONGTEXT,
  d22_relationLocalPart          VARCHAR(100),
  d22_relationNamespace          VARCHAR(255),
  d22_targetOid                  VARCHAR(36),
  defaultUserTemplateRef_type    INTEGER,
  g36                            LONGTEXT,
  g23_description                LONGTEXT,
  globalPasswordPolicyRef_filter LONGTEXT,
  g23_relationLocalPart          VARCHAR(100),
  g23_relationNamespace          VARCHAR(255),
  g23_targetOid                  VARCHAR(36),
  globalPasswordPolicyRef_type   INTEGER,
  logging                        LONGTEXT,
  modelHooks                     LONGTEXT,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  notificationConfiguration      LONGTEXT,
  profilingConfiguration         LONGTEXT,
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task (
  binding                     INTEGER,
  canRunOnNode                VARCHAR(255),
  category                    VARCHAR(255),
  completionTimestamp         DATETIME(6),
  executionStatus             INTEGER,
  handlerUri                  VARCHAR(255),
  lastRunFinishTimestamp      DATETIME(6),
  lastRunStartTimestamp       DATETIME(6),
  name_norm                   VARCHAR(255),
  name_orig                   VARCHAR(255),
  node                        VARCHAR(255),
  objectRef_description       LONGTEXT,
  objectRef_filter            LONGTEXT,
  objectRef_relationLocalPart VARCHAR(100),
  objectRef_relationNamespace VARCHAR(255),
  objectRef_targetOid         VARCHAR(36),
  objectRef_type              INTEGER,
  otherHandlersUriStack       LONGTEXT,
  ownerRef_description        LONGTEXT,
  ownerRef_filter             LONGTEXT,
  ownerRef_relationLocalPart  VARCHAR(100),
  ownerRef_relationNamespace  VARCHAR(255),
  ownerRef_targetOid          VARCHAR(36),
  ownerRef_type               INTEGER,
  parent                      VARCHAR(255),
  progress                    BIGINT,
  recurrence                  INTEGER,
  resultStatus                INTEGER,
  schedule                    LONGTEXT,
  taskIdentifier              VARCHAR(255),
  threadStopAction            INTEGER,
  waitingReason               INTEGER,
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task_dependent (
  task_id   BIGINT      NOT NULL,
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_trigger (
  handlerUri     VARCHAR(255),
  owner_id       BIGINT      NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  timestampValue DATETIME(6),
  id             BIGINT      NOT NULL,
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user (
  additionalName_norm      VARCHAR(255),
  additionalName_orig      VARCHAR(255),
  costCenter               VARCHAR(255),
  allowedIdmAdminGuiAccess BIT,
  passwordXml              LONGTEXT,
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
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_employee_type (
  user_id      BIGINT      NOT NULL,
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organization (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organizational_unit (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_value_policy (
  lifetime     LONGTEXT,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  stringPolicy LONGTEXT,
  id           BIGINT      NOT NULL,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE INDEX iRequestable ON m_abstract_role (requestable);

ALTER TABLE m_abstract_role
ADD INDEX fk_abstract_role (id, oid),
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (id, oid)
REFERENCES m_focus (id, oid);

ALTER TABLE m_any_clob
ADD INDEX fk_any_clob (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_clob
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iDate ON m_any_date (dateValue);

ALTER TABLE m_any_date
ADD INDEX fk_any_date (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iLong ON m_any_long (longValue);

ALTER TABLE m_any_long
ADD INDEX fk_any_long (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iPolyString ON m_any_poly_string (orig);

ALTER TABLE m_any_poly_string
ADD INDEX fk_any_poly_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_poly_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iTargetOid ON m_any_reference (targetoid);

ALTER TABLE m_any_reference
ADD INDEX fk_any_reference (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iString ON m_any_string (stringValue);

ALTER TABLE m_any_string
ADD INDEX fk_any_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
ADD CONSTRAINT fk_any_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any (owner_id, owner_oid, owner_type);

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

ALTER TABLE m_assignment
ADD INDEX fk_assignment (id, oid),
ADD CONSTRAINT fk_assignment
FOREIGN KEY (id, oid)
REFERENCES m_container (id, oid);

ALTER TABLE m_assignment
ADD INDEX fk_assignment_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_audit_delta
ADD INDEX fk_audit_delta (record_id),
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

ALTER TABLE m_authorization
ADD INDEX fk_authorization (id, oid),
ADD CONSTRAINT fk_authorization
FOREIGN KEY (id, oid)
REFERENCES m_container (id, oid);

ALTER TABLE m_authorization
ADD INDEX fk_authorization_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_authorization_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_authorization_action
ADD INDEX fk_authorization_action (role_id, role_oid),
ADD CONSTRAINT fk_authorization_action
FOREIGN KEY (role_id, role_oid)
REFERENCES m_authorization (id, oid);

CREATE INDEX iConnectorNameNorm ON m_connector (name_norm);

CREATE INDEX iConnectorNameOrig ON m_connector (name_orig);

ALTER TABLE m_connector
ADD INDEX fk_connector (id, oid),
ADD CONSTRAINT fk_connector
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iConnectorHostName ON m_connector_host (name_orig);

ALTER TABLE m_connector_host
ADD INDEX fk_connector_host (id, oid),
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_connector_target_system
ADD INDEX fk_connector_target_system (connector_id, connector_oid),
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_id, connector_oid)
REFERENCES m_connector (id, oid);

ALTER TABLE m_exclusion
ADD INDEX fk_exclusion (id, oid),
ADD CONSTRAINT fk_exclusion
FOREIGN KEY (id, oid)
REFERENCES m_container (id, oid);

ALTER TABLE m_exclusion
ADD INDEX fk_exclusion_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_focus
ADD INDEX fk_focus (id, oid),
ADD CONSTRAINT fk_focus
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iGenericObjectName ON m_generic_object (name_orig);

ALTER TABLE m_generic_object
ADD INDEX fk_generic_object (id, oid),
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_metadata
ADD INDEX fk_metadata_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_metadata_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container (id, oid);

CREATE INDEX iNodeName ON m_node (name_orig);

ALTER TABLE m_node
ADD INDEX fk_node (id, oid),
ADD CONSTRAINT fk_node
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_object
ADD INDEX fk_object (id, oid),
ADD CONSTRAINT fk_object
FOREIGN KEY (id, oid)
REFERENCES m_container (id, oid);

CREATE INDEX iObjectTemplate ON m_object_template (name_orig);

ALTER TABLE m_object_template
ADD INDEX fk_object_template (id, oid),
ADD CONSTRAINT fk_object_template
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_operation_result
ADD INDEX fk_result_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_result_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

CREATE INDEX iOrgName ON m_org (name_orig);

ALTER TABLE m_org
ADD INDEX fk_org (id, oid),
ADD CONSTRAINT fk_org
FOREIGN KEY (id, oid)
REFERENCES m_abstract_role (id, oid);

ALTER TABLE m_org_closure
ADD INDEX fk_descendant (descendant_id, descendant_oid),
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_id, descendant_oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_org_closure
ADD INDEX fk_ancestor (ancestor_id, ancestor_oid),
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_id, ancestor_oid)
REFERENCES m_object (id, oid);

    create table m_abstract_role (
        approvalExpression longtext,
        approvalProcess varchar(255),
        approvalSchema longtext,
        automaticallyApproved longtext,
        requestable bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_account_shadow (
        accountType varchar(255),
        allowedIdmAdminGuiAccess bit,
        passwordXml longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_any (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        owner_type integer not null,
        primary key (owner_id, owner_oid, owner_type)
    ) ENGINE=InnoDB;

    create table m_any_clob (
        checksum varchar(32) not null,
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dynamicDef bit,
        clobValue longtext,
        valueType integer,
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    ) ENGINE=InnoDB;

    create table m_any_date (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dateValue datetime not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    ) ENGINE=InnoDB;

    create table m_any_long (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        longValue bigint not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    ) ENGINE=InnoDB;

    create table m_any_poly_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        orig varchar(255) not null,
        dynamicDef bit,
        norm varchar(255),
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    ) ENGINE=InnoDB;

    create table m_any_reference (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        targetoid varchar(36) not null,
        description longtext,
        dynamicDef bit,
        filter longtext,
        relation_namespace varchar(255),
        relation_localPart varchar(100),
        targetType integer,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    ) ENGINE=InnoDB;

    create table m_any_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        stringValue varchar(255) not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    ) ENGINE=InnoDB;

    create table m_assignment (
        accountConstruction longtext,
        administrativeStatus integer,
        archiveTimestamp datetime,
        disableTimestamp datetime,
        effectiveStatus integer,
        enableTimestamp datetime,
        validFrom datetime,
        validTo datetime,
        validityChangeTimestamp datetime,
        validityStatus integer,
        assignmentOwner integer,
        construction longtext,
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetRef_description longtext,
        targetRef_filter longtext,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_audit_delta (
        checksum varchar(32) not null,
        record_id bigint not null,
        delta longtext,
        deltaOid varchar(36),
        deltaType integer,
        details longtext,
        localizedMessage longtext,
        message longtext,
        messageCode varchar(255),
        operation longtext,
        params longtext,
        partialResults longtext,
        status integer,
        token bigint,
        primary key (checksum, record_id)
    ) ENGINE=InnoDB;

    create table m_audit_event (
        id bigint not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage integer,
        eventType integer,
        hostIdentifier varchar(255),
        initiatorName varchar(255),
        initiatorOid varchar(36),
        message varchar(1024),
        outcome integer,
        parameter varchar(255),
        result varchar(255),
        sessionIdentifier varchar(255),
        targetName varchar(255),
        targetOid varchar(36),
        targetOwnerName varchar(255),
        targetOwnerOid varchar(36),
        targetType integer,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue datetime,
        primary key (id)
    ) ENGINE=InnoDB;

    create table m_authorization (
        decision integer,
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_authorization_action (
        role_id bigint not null,
        role_oid varchar(36) not null,
        action varchar(255)
    ) ENGINE=InnoDB;

    create table m_connector (
        connectorBundle varchar(255),
        connectorHostRef_description longtext,
        connectorHostRef_filter longtext,
        c16_relationLocalPart varchar(100),
        c16_relationNamespace varchar(255),
        connectorHostRef_targetOid varchar(36),
        connectorHostRef_type integer,
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection bit,
        sharedSecret longtext,
        timeout integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_connector_target_system (
        connector_id bigint not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    ) ENGINE=InnoDB;

    create table m_container (
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_exclusion (
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        policy integer,
        targetRef_description longtext,
        targetRef_filter longtext,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_focus (
        administrativeStatus integer,
        archiveTimestamp datetime,
        disableTimestamp datetime,
        effectiveStatus integer,
        enableTimestamp datetime,
        validFrom datetime,
        validTo datetime,
        validityChangeTimestamp datetime,
        validityStatus integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_metadata (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        createChannel varchar(255),
        createTimestamp datetime,
        creatorRef_description longtext,
        creatorRef_filter longtext,
        creatorRef_relationLocalPart varchar(100),
        creatorRef_relationNamespace varchar(255),
        creatorRef_targetOid varchar(36),
        creatorRef_type integer,
        modifierRef_description longtext,
        modifierRef_filter longtext,
        modifierRef_relationLocalPart varchar(100),
        modifierRef_relationNamespace varchar(255),
        modifierRef_targetOid varchar(36),
        modifierRef_type integer,
        modifyChannel varchar(255),
        modifyTimestamp datetime,
        primary key (owner_id, owner_oid)
    ) ENGINE=InnoDB;

    create table m_node (
        clusteredNode bit,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort integer,
        lastCheckInTime datetime,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_object (
        description longtext,
        version bigint not null,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_object_template (
        accountConstruction longtext,
        mapping longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id bigint not null,
        details longtext,
        localizedMessage longtext,
        message longtext,
        messageCode varchar(255),
        operation longtext,
        params longtext,
        partialResults longtext,
        status integer,
        token bigint,
        primary key (owner_oid, owner_id)
    ) ENGINE=InnoDB;

    create table m_org (
        costCenter varchar(255),
        displayName_norm varchar(255),
        displayName_orig varchar(255),
        identifier varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_org_closure (
        id bigint not null,
        ancestor_id bigint,
        ancestor_oid varchar(36),
        depthValue integer,
        descendant_id bigint,
        descendant_oid varchar(36),
        primary key (id)
    ) ENGINE=InnoDB;

    create table m_org_incorrect (
        descendant_oid varchar(36) not null,
        descendant_id bigint not null,
        ancestor_oid varchar(36) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    ) ENGINE=InnoDB;

    create table m_org_org_type (
        org_id bigint not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    ) ENGINE=InnoDB;

    create table m_reference (
        reference_type integer not null,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        relLocalPart varchar(100) not null,
        relNamespace varchar(255) not null,
        targetOid varchar(36) not null,
        description longtext,
        filter longtext,
        containerType integer,
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    ) ENGINE=InnoDB;

    create table m_resource (
        administrativeState integer,
        capabilities_cachingMetadata longtext,
        capabilities_configured longtext,
        capabilities_native longtext,
        configuration longtext,
        connectorRef_description longtext,
        connectorRef_filter longtext,
        connectorRef_relationLocalPart varchar(100),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type integer,
        consistency longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus integer,
        projection longtext,
        schemaHandling longtext,
        scripts longtext,
        synchronization longtext,
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_role (
        name_norm varchar(255),
        name_orig varchar(255),
        roleType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_shadow (
        administrativeStatus integer,
        archiveTimestamp datetime,
        disableTimestamp datetime,
        effectiveStatus integer,
        enableTimestamp datetime,
        validFrom datetime,
        validTo datetime,
        validityChangeTimestamp datetime,
        validityStatus integer,
        assigned bit,
        attemptNumber integer,
        dead bit,
        exist bit,
        failedOperationType integer,
        intent varchar(255),
        iteration integer,
        iterationToken varchar(255),
        kind integer,
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange longtext,
        class_namespace varchar(255),
        class_localPart varchar(100),
        resourceRef_description longtext,
        resourceRef_filter longtext,
        resourceRef_relationLocalPart varchar(100),
        resourceRef_relationNamespace varchar(255),
        resourceRef_targetOid varchar(36),
        resourceRef_type integer,
        synchronizationSituation integer,
        synchronizationTimestamp datetime,
        id bigint not null,
        oid varchar(36) not null,
        attrId bigint,
        attrOid varchar(36),
        attrType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_sync_situation_description (
        checksum varchar(32) not null,
        shadow_id bigint not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        situation integer,
        timestampValue datetime,
        primary key (checksum, shadow_id, shadow_oid)
    ) ENGINE=InnoDB;

    create table m_system_configuration (
        cleanupPolicy longtext,
        connectorFramework longtext,
        d22_description longtext,
        defaultUserTemplateRef_filter longtext,
        d22_relationLocalPart varchar(100),
        d22_relationNamespace varchar(255),
        d22_targetOid varchar(36),
        defaultUserTemplateRef_type integer,
        g36 longtext,
        g23_description longtext,
        globalPasswordPolicyRef_filter longtext,
        g23_relationLocalPart varchar(100),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type integer,
        logging longtext,
        modelHooks longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration longtext,
        profilingConfiguration longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_task (
        binding integer,
        canRunOnNode varchar(255),
        category varchar(255),
        completionTimestamp datetime,
        executionStatus integer,
        handlerUri varchar(255),
        lastRunFinishTimestamp datetime,
        lastRunStartTimestamp datetime,
        name_norm varchar(255),
        name_orig varchar(255),
        node varchar(255),
        objectRef_description longtext,
        objectRef_filter longtext,
        objectRef_relationLocalPart varchar(100),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type integer,
        otherHandlersUriStack longtext,
        ownerRef_description longtext,
        ownerRef_filter longtext,
        ownerRef_relationLocalPart varchar(100),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type integer,
        parent varchar(255),
        progress bigint,
        recurrence integer,
        resultStatus integer,
        schedule longtext,
        taskIdentifier varchar(255),
        threadStopAction integer,
        waitingReason integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_task_dependent (
        task_id bigint not null,
        task_oid varchar(36) not null,
        dependent varchar(255)
    ) ENGINE=InnoDB;

    create table m_trigger (
        handlerUri varchar(255),
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        timestampValue datetime,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_user (
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess bit,
        passwordXml longtext,
        emailAddress varchar(255),
        employeeNumber varchar(255),
        familyName_norm varchar(255),
        familyName_orig varchar(255),
        fullName_norm varchar(255),
        fullName_orig varchar(255),
        givenName_norm varchar(255),
        givenName_orig varchar(255),
        honorificPrefix_norm varchar(255),
        honorificPrefix_orig varchar(255),
        honorificSuffix_norm varchar(255),
        honorificSuffix_orig varchar(255),
        locale varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        nickName_norm varchar(255),
        nickName_orig varchar(255),
        preferredLanguage varchar(255),
        telephoneNumber varchar(255),
        timezone varchar(255),
        title_norm varchar(255),
        title_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_user_employee_type (
        user_id bigint not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_organization (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) ENGINE=InnoDB;

    create table m_value_policy (
        lifetime longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create index iRequestable on m_abstract_role (requestable);

    alter table m_abstract_role 
        add index fk_abstract_role (id, oid), 
        add constraint fk_abstract_role 
        foreign key (id, oid) 
        references m_focus (id, oid);

    alter table m_account_shadow 
        add index fk_account_shadow (id, oid), 
        add constraint fk_account_shadow 
        foreign key (id, oid) 
        references m_shadow (id, oid);

    alter table m_any_clob 
        add index fk_any_clob (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_clob 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iDate on m_any_date (dateValue);

    alter table m_any_date 
        add index fk_any_date (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_date 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iLong on m_any_long (longValue);

    alter table m_any_long 
        add index fk_any_long (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_long 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iPolyString on m_any_poly_string (orig);

    alter table m_any_poly_string 
        add index fk_any_poly_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_poly_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iTargetOid on m_any_reference (targetoid);

    alter table m_any_reference 
        add index fk_any_reference (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_reference 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iString on m_any_string (stringValue);

    alter table m_any_string 
        add index fk_any_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type), 
        add constraint fk_any_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any (owner_id, owner_oid, owner_type);

    create index iAssignmentAdministrative on m_assignment (administrativeStatus);

    create index iAssignmentEffective on m_assignment (effectiveStatus);

    alter table m_assignment 
        add index fk_assignment (id, oid), 
        add constraint fk_assignment 
        foreign key (id, oid) 
        references m_container (id, oid);

    alter table m_assignment 
        add index fk_assignment_owner (owner_id, owner_oid), 
        add constraint fk_assignment_owner 
        foreign key (owner_id, owner_oid) 
        references m_object (id, oid);

    alter table m_audit_delta 
        add index fk_audit_delta (record_id), 
        add constraint fk_audit_delta 
        foreign key (record_id) 
        references m_audit_event (id);

    alter table m_authorization 
        add index fk_authorization (id, oid), 
        add constraint fk_authorization 
        foreign key (id, oid) 
        references m_container (id, oid);

    alter table m_authorization 
        add index fk_authorization_owner (owner_id, owner_oid), 
        add constraint fk_authorization_owner 
        foreign key (owner_id, owner_oid) 
        references m_object (id, oid);

    alter table m_authorization_action 
        add index fk_authorization_action (role_id, role_oid), 
        add constraint fk_authorization_action 
        foreign key (role_id, role_oid) 
        references m_authorization (id, oid);

    create index iConnectorNameNorm on m_connector (name_norm);

    create index iConnectorNameOrig on m_connector (name_orig);

    alter table m_connector 
        add index fk_connector (id, oid), 
        add constraint fk_connector 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iConnectorHostName on m_connector_host (name_orig);

    alter table m_connector_host 
        add index fk_connector_host (id, oid), 
        add constraint fk_connector_host 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_connector_target_system 
        add index fk_connector_target_system (connector_id, connector_oid), 
        add constraint fk_connector_target_system 
        foreign key (connector_id, connector_oid) 
        references m_connector (id, oid);

    alter table m_exclusion 
        add index fk_exclusion (id, oid), 
        add constraint fk_exclusion 
        foreign key (id, oid) 
        references m_container (id, oid);

    alter table m_exclusion 
        add index fk_exclusion_owner (owner_id, owner_oid), 
        add constraint fk_exclusion_owner 
        foreign key (owner_id, owner_oid) 
        references m_object (id, oid);

    create index iFocusAdministrative on m_focus (administrativeStatus);

    create index iFocusEffective on m_focus (effectiveStatus);

    alter table m_focus 
        add index fk_focus (id, oid), 
        add constraint fk_focus 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iGenericObjectName on m_generic_object (name_orig);

    alter table m_generic_object 
        add index fk_generic_object (id, oid), 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_metadata 
        add index fk_metadata_owner (owner_id, owner_oid), 
        add constraint fk_metadata_owner 
        foreign key (owner_id, owner_oid) 
        references m_container (id, oid);

    create index iNodeName on m_node (name_orig);

    alter table m_node 
        add index fk_node (id, oid), 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_object 
        add index fk_object (id, oid), 
        add constraint fk_object 
        foreign key (id, oid) 
        references m_container (id, oid);

    create index iObjectTemplate on m_object_template (name_orig);

    alter table m_object_template 
        add index fk_object_template (id, oid), 
        add constraint fk_object_template 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_operation_result 
        add index fk_result_owner (owner_id, owner_oid), 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object (id, oid);

    create index iOrgName on m_org (name_orig);

    alter table m_org 
        add index fk_org (id, oid), 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_abstract_role (id, oid);

    alter table m_org_closure 
        add index fk_descendant (descendant_id, descendant_oid), 
        add constraint fk_descendant 
        foreign key (descendant_id, descendant_oid) 
        references m_object (id, oid);

    alter table m_org_closure 
        add index fk_ancestor (ancestor_id, ancestor_oid), 
        add constraint fk_ancestor 
        foreign key (ancestor_id, ancestor_oid) 
        references m_object (id, oid);
ALTER TABLE m_org_org_type
ADD INDEX fk_org_org_type (org_id, org_oid),
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_id, org_oid)
REFERENCES m_org (id, oid);

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid);

ALTER TABLE m_reference
ADD INDEX fk_reference_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container (id, oid);

CREATE INDEX iResourceName ON m_resource (name_orig);

ALTER TABLE m_resource
ADD INDEX fk_resource (id, oid),
ADD CONSTRAINT fk_resource
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iRoleName ON m_role (name_orig);

ALTER TABLE m_role
ADD INDEX fk_role (id, oid),
ADD CONSTRAINT fk_role
FOREIGN KEY (id, oid)
REFERENCES m_abstract_role (id, oid);

CREATE INDEX iShadowNameOrig ON m_shadow (name_orig);

CREATE INDEX iShadowDead ON m_shadow (dead);

CREATE INDEX iShadowNameNorm ON m_shadow (name_norm);

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowAdministrative ON m_shadow (administrativeStatus);

CREATE INDEX iShadowEffective ON m_shadow (effectiveStatus);

ALTER TABLE m_shadow
ADD INDEX fk_shadow (id, oid),
ADD CONSTRAINT fk_shadow
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_sync_situation_description
ADD INDEX fk_shadow_sync_situation (shadow_id, shadow_oid),
ADD CONSTRAINT fk_shadow_sync_situation
FOREIGN KEY (shadow_id, shadow_oid)
REFERENCES m_shadow (id, oid);

CREATE INDEX iSystemConfigurationName ON m_system_configuration (name_orig);

ALTER TABLE m_system_configuration
ADD INDEX fk_system_configuration (id, oid),
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iTaskNameNameNorm ON m_task (name_norm);

CREATE INDEX iParent ON m_task (parent);

CREATE INDEX iTaskNameOrig ON m_task (name_orig);

ALTER TABLE m_task
ADD INDEX fk_task (id, oid),
ADD CONSTRAINT fk_task
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_task_dependent
ADD INDEX fk_task_dependent (task_id, task_oid),
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_id, task_oid)
REFERENCES m_task (id, oid);

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_trigger
ADD INDEX fk_trigger (id, oid),
ADD CONSTRAINT fk_trigger
FOREIGN KEY (id, oid)
REFERENCES m_container (id, oid);

ALTER TABLE m_trigger
ADD INDEX fk_trigger_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

CREATE INDEX iFullName ON m_user (fullName_orig);

CREATE INDEX iLocality ON m_user (locality_orig);

CREATE INDEX iHonorificSuffix ON m_user (honorificSuffix_orig);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iGivenName ON m_user (givenName_orig);

CREATE INDEX iFamilyName ON m_user (familyName_orig);

CREATE INDEX iAdditionalName ON m_user (additionalName_orig);

CREATE INDEX iHonorificPrefix ON m_user (honorificPrefix_orig);

CREATE INDEX iUserName ON m_user (name_orig);

ALTER TABLE m_user
ADD INDEX fk_user (id, oid),
ADD CONSTRAINT fk_user
FOREIGN KEY (id, oid)
REFERENCES m_focus (id, oid);

ALTER TABLE m_user_employee_type
ADD INDEX fk_user_employee_type (user_id, user_oid),
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user (id, oid);

ALTER TABLE m_user_organization
ADD INDEX fk_user_organization (user_id, user_oid),
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user (id, oid);

ALTER TABLE m_user_organizational_unit
ADD INDEX fk_user_org_unit (user_id, user_oid),
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_id, user_oid)
REFERENCES m_user (id, oid);

CREATE INDEX iValuePolicy ON m_value_policy (name_orig);

ALTER TABLE m_value_policy
ADD INDEX fk_value_policy (id, oid),
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE TABLE hibernate_sequence (
  next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);
