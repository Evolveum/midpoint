# use for db create
# CREATE DATABASE <database name>
#   CHARACTER SET utf8
#   DEFAULT CHARACTER SET utf8
#   COLLATE utf8_general_ci
#   DEFAULT COLLATE utf8_general_ci
# ;

CREATE TABLE m_account_shadow (
  accountType              VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
  passwordXml              LONGTEXT,
  id                       BIGINT      NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any (
  owner_id  BIGINT      NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INTEGER     NOT NULL,
  PRIMARY KEY (owner_id, owner_oid, ownerType)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any_clob (
  checksum               VARCHAR(32)  NOT NULL,
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  BIGINT       NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INTEGER      NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  dynamicDef             BOOLEAN,
  clobValue              LONGTEXT,
  valueType              INTEGER,
  PRIMARY KEY (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any_date (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  BIGINT       NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INTEGER      NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  dateValue              DATETIME(6)     NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, dateValue)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any_long (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  BIGINT       NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INTEGER      NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  longValue              BIGINT       NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, longValue)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any_reference (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  BIGINT       NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INTEGER      NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  targetoid              VARCHAR(255) NOT NULL,
  description            LONGTEXT,
  dynamicDef             BOOLEAN,
  filter                 LONGTEXT,
  relation_namespace     VARCHAR(255),
  relation_localPart     VARCHAR(100),
  targetType             INTEGER,
  valueType              INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, targetoid)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_any_string (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  BIGINT       NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INTEGER      NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  stringValue            VARCHAR(255) NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INTEGER,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, stringValue)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_assignment (
  accountConstruction         LONGTEXT,
  enabled                     BOOLEAN,
  validFrom                   DATETIME(6),
  validTo                     DATETIME(6),
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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_audit_delta (
  RAuditEventRecord_id BIGINT NOT NULL,
  deltas               LONGTEXT
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_audit_event (
  id                BIGINT NOT NULL,
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    VARCHAR(255),
  initiator         LONGTEXT,
  outcome           INTEGER,
  sessionIdentifier VARCHAR(255),
  target            LONGTEXT,
  targetOwner       LONGTEXT,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    BIGINT,
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_connector_host (
  hostname          VARCHAR(255),
  name_norm         VARCHAR(255),
  name_orig         VARCHAR(255),
  port              VARCHAR(255),
  protectConnection BOOLEAN,
  sharedSecret      LONGTEXT,
  timeout           INTEGER,
  id                BIGINT      NOT NULL,
  oid               VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_connector_target_system (
  connector_id     BIGINT      NOT NULL,
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_container (
  id  BIGINT      NOT NULL,
  oid VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  id         BIGINT      NOT NULL,
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_node (
  clusteredNode          BOOLEAN,
  hostname               VARCHAR(255),
  internalNodeIdentifier VARCHAR(255),
  jmxPort                INTEGER,
  lastCheckInTime        DATETIME(6),
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  nodeIdentifier         VARCHAR(255),
  running                BOOLEAN,
  id                     BIGINT      NOT NULL,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayName_norm VARCHAR(255),
  displayName_orig VARCHAR(255),
  identifier       VARCHAR(255),
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  id               BIGINT      NOT NULL,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_org_closure (
  id             BIGINT NOT NULL,
  depthValue     INTEGER,
  ancestor_id    BIGINT,
  ancestor_oid   VARCHAR(36),
  descendant_id  BIGINT,
  descendant_oid VARCHAR(36),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_org_org_type (
  org_id  BIGINT      NOT NULL,
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_password_policy (
  lifetime     LONGTEXT,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  stringPolicy LONGTEXT,
  id           BIGINT      NOT NULL,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

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
  schemaHandling                 LONGTEXT,
  scripts                        LONGTEXT,
  synchronization                LONGTEXT,
  xmlSchema                      LONGTEXT,
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_resource_shadow (
  enabled                       BOOLEAN,
  validFrom                     DATETIME(6),
  validTo                       DATETIME(6),
  attemptNumber                 INTEGER,
  dead                          BOOLEAN,
  failedOperationType           INTEGER,
  intent                        VARCHAR(255),
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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_role (
  approvalExpression    LONGTEXT,
  approvalProcess       VARCHAR(255),
  approvalSchema        LONGTEXT,
  automaticallyApproved LONGTEXT,
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  requestable           BOOLEAN,
  roleType              VARCHAR(255),
  id                    BIGINT      NOT NULL,
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_sync_situation_description (
  shadow_id      BIGINT      NOT NULL,
  shadow_oid     VARCHAR(36) NOT NULL,
  chanel         VARCHAR(255),
  situation      INTEGER,
  timestampValue DATETIME(6)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_system_configuration (
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
  id                             BIGINT      NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_task (
  binding                     INTEGER,
  canRunOnNode                VARCHAR(255),
  category                    VARCHAR(255),
  claimExpirationTimestamp    DATETIME(6),
  exclusivityStatus           INTEGER,
  executionStatus             INTEGER,
  handlerUri                  VARCHAR(255),
  lastRunFinishTimestamp      DATETIME(6),
  lastRunStartTimestamp       DATETIME(6),
  modelOperationState         LONGTEXT,
  name_norm                   VARCHAR(255),
  name_orig                   VARCHAR(255),
  nextRunStartTime            DATETIME(6),
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
  id                          BIGINT      NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_user (
  enabled                  BOOLEAN,
  validFrom                DATETIME(6),
  validTo                  DATETIME(6),
  additionalName_norm      VARCHAR(255),
  additionalName_orig      VARCHAR(255),
  costCenter               VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
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
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_user_employee_type (
  user_id      BIGINT      NOT NULL,
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_user_organization (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_user_organizational_unit (
  user_id  BIGINT      NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

CREATE TABLE m_user_template (
  accountConstruction  LONGTEXT,
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  propertyConstruction LONGTEXT,
  id                   BIGINT      NOT NULL,
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ENGINE = InnoDB;

ALTER TABLE m_account_shadow
ADD INDEX fk_account_shadow (id, oid),
ADD CONSTRAINT fk_account_shadow
FOREIGN KEY (id, oid)
REFERENCES m_resource_shadow (id, oid);

ALTER TABLE m_any_clob
ADD INDEX fk_any_clob (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType),
ADD CONSTRAINT fk_any_clob
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any (owner_id, owner_oid, ownerType);

CREATE INDEX iDate ON m_any_date (dateValue);

ALTER TABLE m_any_date
ADD INDEX fk_any_date (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType),
ADD CONSTRAINT fk_any_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any (owner_id, owner_oid, ownerType);

CREATE INDEX iLong ON m_any_long (longValue);

ALTER TABLE m_any_long
ADD INDEX fk_any_long (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType),
ADD CONSTRAINT fk_any_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any (owner_id, owner_oid, ownerType);

CREATE INDEX iTargetOid ON m_any_reference (targetoid);

ALTER TABLE m_any_reference
ADD INDEX fk_any_reference (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType),
ADD CONSTRAINT fk_any_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any (owner_id, owner_oid, ownerType);

CREATE INDEX iString ON m_any_string (stringValue);

ALTER TABLE m_any_string
ADD INDEX fk_any_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType),
ADD CONSTRAINT fk_any_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any (owner_id, owner_oid, ownerType);

CREATE INDEX iAssignmentEnabled ON m_assignment (enabled);

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
ADD INDEX fk_audit_delta (RAuditEventRecord_id),
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (RAuditEventRecord_id)
REFERENCES m_audit_event (id);

CREATE INDEX iConnectorName ON m_connector (name_norm);

ALTER TABLE m_connector
ADD INDEX fk_connector (id, oid),
ADD CONSTRAINT fk_connector
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

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

ALTER TABLE m_generic_object
ADD INDEX fk_generic_object (id, oid),
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

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

ALTER TABLE m_operation_result
ADD INDEX fk_result_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_result_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_org
ADD INDEX fk_org (id, oid),
ADD CONSTRAINT fk_org
FOREIGN KEY (id, oid)
REFERENCES m_role (id, oid);

CREATE INDEX iDescendant ON m_org_closure (descendant_oid, descendant_id);

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid, ancestor_id);

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

ALTER TABLE m_org_org_type
ADD INDEX fk_org_org_type (org_id, org_oid),
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_id, org_oid)
REFERENCES m_org (id, oid);

ALTER TABLE m_password_policy
ADD INDEX fk_password_policy (id, oid),
ADD CONSTRAINT fk_password_policy
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_reference
ADD INDEX fk_reference_owner (owner_id, owner_oid),
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container (id, oid);

ALTER TABLE m_resource
ADD INDEX fk_resource (id, oid),
ADD CONSTRAINT fk_resource
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iResourceObjectShadowEnabled ON m_resource_shadow (enabled);

CREATE INDEX iShadowResourceRef ON m_resource_shadow (resourceRef_targetOid);

CREATE INDEX iResourceShadowName ON m_resource_shadow (name_norm);

ALTER TABLE m_resource_shadow
ADD INDEX fk_resource_object_shadow (id, oid),
ADD CONSTRAINT fk_resource_object_shadow
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iRequestable ON m_role (requestable);

ALTER TABLE m_role
ADD INDEX fk_role (id, oid),
ADD CONSTRAINT fk_role
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

ALTER TABLE m_sync_situation_description
ADD INDEX fk_shadow_sync_situation (shadow_id, shadow_oid),
ADD CONSTRAINT fk_shadow_sync_situation
FOREIGN KEY (shadow_id, shadow_oid)
REFERENCES m_resource_shadow (id, oid);

ALTER TABLE m_system_configuration
ADD INDEX fk_system_configuration (id, oid),
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iTaskName ON m_task (name_norm);

ALTER TABLE m_task
ADD INDEX fk_task (id, oid),
ADD CONSTRAINT fk_task
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE INDEX iFullName ON m_user (fullName_norm);

CREATE INDEX iLocality ON m_user (locality_norm);

CREATE INDEX iHonorificSuffix ON m_user (honorificSuffix_norm);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iGivenName ON m_user (givenName_norm);

CREATE INDEX iFamilyName ON m_user (familyName_norm);

CREATE INDEX iAdditionalName ON m_user (additionalName_norm);

CREATE INDEX iHonorificPrefix ON m_user (honorificPrefix_norm);

CREATE INDEX iUserEnabled ON m_user (enabled);

ALTER TABLE m_user
ADD INDEX fk_user (id, oid),
ADD CONSTRAINT fk_user
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

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

ALTER TABLE m_user_template
ADD INDEX fk_user_template (id, oid),
ADD CONSTRAINT fk_user_template
FOREIGN KEY (id, oid)
REFERENCES m_object (id, oid);

CREATE TABLE hibernate_sequence (
  next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);