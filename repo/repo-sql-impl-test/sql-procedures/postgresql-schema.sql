CREATE TABLE m_account_shadow (
  accountType              VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
  passwordXml              TEXT,
  id                       INT8        NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_any (
  owner_id  INT8        NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INT4        NOT NULL,
  PRIMARY KEY (owner_id, owner_oid, ownerType)
);

CREATE TABLE m_any_clob (
  checksum               VARCHAR(32)  NOT NULL,
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  INT8         NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INT4         NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  dynamicDef             BOOLEAN,
  clobValue              TEXT,
  valueType              INT4,
  PRIMARY KEY (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart)
);

CREATE TABLE m_any_date (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  INT8         NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INT4         NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  dateValue              TIMESTAMP    NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INT4,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, dateValue)
);

CREATE TABLE m_any_long (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  INT8         NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INT4         NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  longValue              INT8         NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INT4,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, longValue)
);

CREATE TABLE m_any_reference (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  INT8         NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INT4         NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  targetoid              VARCHAR(255) NOT NULL,
  description            TEXT,
  dynamicDef             BOOLEAN,
  filter                 TEXT,
  relation_namespace     VARCHAR(255),
  relation_localPart     VARCHAR(100),
  targetType             INT4,
  valueType              INT4,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, targetoid)
);

CREATE TABLE m_any_string (
  name_namespace         VARCHAR(255) NOT NULL,
  name_localPart         VARCHAR(100) NOT NULL,
  anyContainer_owner_id  INT8         NOT NULL,
  anyContainer_owner_oid VARCHAR(36)  NOT NULL,
  anyContainer_ownertype INT4         NOT NULL,
  type_namespace         VARCHAR(255) NOT NULL,
  type_localPart         VARCHAR(100) NOT NULL,
  stringValue            VARCHAR(255) NOT NULL,
  dynamicDef             BOOLEAN,
  valueType              INT4,
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownertype, type_namespace, type_localPart, stringValue)
);

CREATE TABLE m_assignment (
  accountConstruction         TEXT,
  enabled                     BOOLEAN,
  validFrom                   TIMESTAMP,
  validTo                     TIMESTAMP,
  description                 TEXT,
  owner_id                    INT8        NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  targetRef_description       TEXT,
  targetRef_filter            TEXT,
  targetRef_relationLocalPart VARCHAR(100),
  targetRef_relationNamespace VARCHAR(255),
  targetRef_targetOid         VARCHAR(36),
  targetRef_type              INT4,
  id                          INT8        NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  extId                       INT8,
  extOid                      VARCHAR(36),
  extType                     INT4,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_audit_delta (
  RAuditEventRecord_id INT8 NOT NULL,
  deltas               TEXT
);

CREATE TABLE m_audit_event (
  id                INT8 NOT NULL,
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INT4,
  eventType         INT4,
  hostIdentifier    VARCHAR(255),
  initiator         TEXT,
  outcome           INT4,
  sessionIdentifier VARCHAR(255),
  target            TEXT,
  targetOwner       TEXT,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    INT8,
  PRIMARY KEY (id)
);

CREATE TABLE m_connector (
  connectorBundle              VARCHAR(255),
  connectorHostRef_description TEXT,
  connectorHostRef_filter      TEXT,
  c16_relationLocalPart        VARCHAR(100),
  c16_relationNamespace        VARCHAR(255),
  connectorHostRef_targetOid   VARCHAR(36),
  connectorHostRef_type        INT4,
  connectorType                VARCHAR(255),
  connectorVersion             VARCHAR(255),
  framework                    VARCHAR(255),
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  namespace                    VARCHAR(255),
  xmlSchema                    TEXT,
  id                           INT8        NOT NULL,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_connector_host (
  hostname          VARCHAR(255),
  name_norm         VARCHAR(255),
  name_orig         VARCHAR(255),
  port              VARCHAR(255),
  protectConnection BOOLEAN,
  sharedSecret      TEXT,
  timeout           INT4,
  id                INT8        NOT NULL,
  oid               VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_connector_target_system (
  connector_id     INT8        NOT NULL,
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
);

CREATE TABLE m_container (
  id  INT8        NOT NULL,
  oid VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_exclusion (
  description                 TEXT,
  owner_id                    INT8        NOT NULL,
  owner_oid                   VARCHAR(36) NOT NULL,
  policy                      INT4,
  targetRef_description       TEXT,
  targetRef_filter            TEXT,
  targetRef_relationLocalPart VARCHAR(100),
  targetRef_relationNamespace VARCHAR(255),
  targetRef_targetOid         VARCHAR(36),
  targetRef_type              INT4,
  id                          INT8        NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  id         INT8        NOT NULL,
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_node (
  clusteredNode          BOOLEAN,
  hostname               VARCHAR(255),
  internalNodeIdentifier VARCHAR(255),
  jmxPort                INT4,
  lastCheckInTime        TIMESTAMP,
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  nodeIdentifier         VARCHAR(255),
  running                BOOLEAN,
  id                     INT8        NOT NULL,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_object (
  description TEXT,
  version     INT8        NOT NULL,
  id          INT8        NOT NULL,
  oid         VARCHAR(36) NOT NULL,
  extId       INT8,
  extOid      VARCHAR(36),
  extType     INT4,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_operation_result (
  owner_oid        VARCHAR(36) NOT NULL,
  owner_id         INT8        NOT NULL,
  details          TEXT,
  localizedMessage TEXT,
  message          TEXT,
  messageCode      VARCHAR(255),
  operation        TEXT,
  params           TEXT,
  partialResults   TEXT,
  status           INT4,
  token            INT8,
  PRIMARY KEY (owner_oid, owner_id)
);

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayName_norm VARCHAR(255),
  displayName_orig VARCHAR(255),
  identifier       VARCHAR(255),
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  id               INT8        NOT NULL,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_org_closure (
  id             INT8 NOT NULL,
  depthValue     INT4,
  ancestor_id    INT8,
  ancestor_oid   VARCHAR(36),
  descendant_id  INT8,
  descendant_oid VARCHAR(36),
  PRIMARY KEY (id)
);

CREATE TABLE m_org_org_type (
  org_id  INT8        NOT NULL,
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
);

CREATE TABLE m_password_policy (
  lifetime     TEXT,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  stringPolicy TEXT,
  id           INT8        NOT NULL,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_reference (
  reference_type INT4         NOT NULL,
  owner_id       INT8         NOT NULL,
  owner_oid      VARCHAR(36)  NOT NULL,
  relLocalPart   VARCHAR(100) NOT NULL,
  relNamespace   VARCHAR(255) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  description    TEXT,
  filter         TEXT,
  containerType  INT4,
  PRIMARY KEY (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
);

CREATE TABLE m_resource (
  administrativeState            INT4,
  capabilities_cachingMetadata   TEXT,
  capabilities_configured        TEXT,
  capabilities_native            TEXT,
  configuration                  TEXT,
  connectorRef_description       TEXT,
  connectorRef_filter            TEXT,
  connectorRef_relationLocalPart VARCHAR(100),
  connectorRef_relationNamespace VARCHAR(255),
  connectorRef_targetOid         VARCHAR(36),
  connectorRef_type              INT4,
  consistency                    TEXT,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  namespace                      VARCHAR(255),
  o16_lastAvailabilityStatus     INT4,
  schemaHandling                 TEXT,
  scripts                        TEXT,
  synchronization                TEXT,
  xmlSchema                      TEXT,
  id                             INT8        NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_resource_shadow (
  enabled                       BOOLEAN,
  validFrom                     TIMESTAMP,
  validTo                       TIMESTAMP,
  attemptNumber                 INT4,
  dead                          BOOLEAN,
  failedOperationType           INT4,
  intent                        VARCHAR(255),
  name_norm                     VARCHAR(255),
  name_orig                     VARCHAR(255),
  objectChange                  TEXT,
  class_namespace               VARCHAR(255),
  class_localPart               VARCHAR(100),
  resourceRef_description       TEXT,
  resourceRef_filter            TEXT,
  resourceRef_relationLocalPart VARCHAR(100),
  resourceRef_relationNamespace VARCHAR(255),
  resourceRef_targetOid         VARCHAR(36),
  resourceRef_type              INT4,
  synchronizationSituation      INT4,
  synchronizationTimestamp      TIMESTAMP,
  id                            INT8        NOT NULL,
  oid                           VARCHAR(36) NOT NULL,
  attrId                        INT8,
  attrOid                       VARCHAR(36),
  attrType                      INT4,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_role (
  approvalExpression    TEXT,
  approvalProcess       VARCHAR(255),
  approvalSchema        TEXT,
  automaticallyApproved TEXT,
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  requestable           BOOLEAN,
  roleType              VARCHAR(255),
  id                    INT8        NOT NULL,
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_sync_situation_description (
  shadow_id      INT8        NOT NULL,
  shadow_oid     VARCHAR(36) NOT NULL,
  chanel         VARCHAR(255),
  situation      INT4,
  timestampValue TIMESTAMP
);

CREATE TABLE m_system_configuration (
  connectorFramework             TEXT,
  d22_description                TEXT,
  defaultUserTemplateRef_filter  TEXT,
  d22_relationLocalPart          VARCHAR(100),
  d22_relationNamespace          VARCHAR(255),
  d22_targetOid                  VARCHAR(36),
  defaultUserTemplateRef_type    INT4,
  g36                            TEXT,
  g23_description                TEXT,
  globalPasswordPolicyRef_filter TEXT,
  g23_relationLocalPart          VARCHAR(100),
  g23_relationNamespace          VARCHAR(255),
  g23_targetOid                  VARCHAR(36),
  globalPasswordPolicyRef_type   INT4,
  logging                        TEXT,
  modelHooks                     TEXT,
  name_norm                      VARCHAR(255),
  name_orig                      VARCHAR(255),
  notificationConfiguration      TEXT,
  id                             INT8        NOT NULL,
  oid                            VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_task (
  binding                     INT4,
  canRunOnNode                VARCHAR(255),
  category                    VARCHAR(255),
  claimExpirationTimestamp    TIMESTAMP,
  exclusivityStatus           INT4,
  executionStatus             INT4,
  handlerUri                  VARCHAR(255),
  lastRunFinishTimestamp      TIMESTAMP,
  lastRunStartTimestamp       TIMESTAMP,
  modelOperationState         TEXT,
  name_norm                   VARCHAR(255),
  name_orig                   VARCHAR(255),
  nextRunStartTime            TIMESTAMP,
  node                        VARCHAR(255),
  objectRef_description       TEXT,
  objectRef_filter            TEXT,
  objectRef_relationLocalPart VARCHAR(100),
  objectRef_relationNamespace VARCHAR(255),
  objectRef_targetOid         VARCHAR(36),
  objectRef_type              INT4,
  otherHandlersUriStack       TEXT,
  ownerRef_description        TEXT,
  ownerRef_filter             TEXT,
  ownerRef_relationLocalPart  VARCHAR(100),
  ownerRef_relationNamespace  VARCHAR(255),
  ownerRef_targetOid          VARCHAR(36),
  ownerRef_type               INT4,
  parent                      VARCHAR(255),
  progress                    INT8,
  recurrence                  INT4,
  resultStatus                INT4,
  schedule                    TEXT,
  taskIdentifier              VARCHAR(255),
  threadStopAction            INT4,
  id                          INT8        NOT NULL,
  oid                         VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid)
);

CREATE TABLE m_user (
  enabled                  BOOLEAN,
  validFrom                TIMESTAMP,
  validTo                  TIMESTAMP,
  additionalName_norm      VARCHAR(255),
  additionalName_orig      VARCHAR(255),
  costCenter               VARCHAR(255),
  allowedIdmAdminGuiAccess BOOLEAN,
  passwordXml              TEXT,
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
  id                       INT8        NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_user_employee_type (
  user_id      INT8        NOT NULL,
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
);

CREATE TABLE m_user_organization (
  user_id  INT8        NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);

CREATE TABLE m_user_organizational_unit (
  user_id  INT8        NOT NULL,
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);

CREATE TABLE m_user_template (
  accountConstruction  TEXT,
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  propertyConstruction TEXT,
  id                   INT8        NOT NULL,
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

ALTER TABLE m_account_shadow
ADD CONSTRAINT fk_account_shadow
FOREIGN KEY (id, oid)
REFERENCES m_resource_shadow;

ALTER TABLE m_any_clob
ADD CONSTRAINT fk_any_clob
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any;

CREATE INDEX iDate ON m_any_date (dateValue);

ALTER TABLE m_any_date
ADD CONSTRAINT fk_any_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any;

CREATE INDEX iLong ON m_any_long (longValue);

ALTER TABLE m_any_long
ADD CONSTRAINT fk_any_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any;

CREATE INDEX iTargetOid ON m_any_reference (targetoid);

ALTER TABLE m_any_reference
ADD CONSTRAINT fk_any_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any;

CREATE INDEX iString ON m_any_string (stringValue);

ALTER TABLE m_any_string
ADD CONSTRAINT fk_any_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_ownerType)
REFERENCES m_any;

CREATE INDEX iAssignmentEnabled ON m_assignment (enabled);

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
FOREIGN KEY (RAuditEventRecord_id)
REFERENCES m_audit_event;

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

ALTER TABLE m_generic_object
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_node
ADD CONSTRAINT fk_node
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_object
ADD CONSTRAINT fk_object
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_operation_result
ADD CONSTRAINT fk_result_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

ALTER TABLE m_org
ADD CONSTRAINT fk_org
FOREIGN KEY (id, oid)
REFERENCES m_role;

CREATE INDEX iDescendant ON m_org_closure (descendant_oid, descendant_id);

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid, ancestor_id);

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

CREATE INDEX iResourceObjectShadowEnabled ON m_resource_shadow (enabled);

CREATE INDEX iShadowResourceRef ON m_resource_shadow (resourceRef_targetOid);

CREATE INDEX iResourceShadowName ON m_resource_shadow (name_norm);

ALTER TABLE m_resource_shadow
ADD CONSTRAINT fk_resource_object_shadow
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iRequestable ON m_role (requestable);

ALTER TABLE m_role
ADD CONSTRAINT fk_role
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_sync_situation_description
ADD CONSTRAINT fk_shadow_sync_situation
FOREIGN KEY (shadow_id, shadow_oid)
REFERENCES m_resource_shadow;

ALTER TABLE m_system_configuration
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iTaskName ON m_task (name_norm);

ALTER TABLE m_task
ADD CONSTRAINT fk_task
FOREIGN KEY (id, oid)
REFERENCES m_object;

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
ADD CONSTRAINT fk_user
FOREIGN KEY (id, oid)
REFERENCES m_object;

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

ALTER TABLE m_user_template
ADD CONSTRAINT fk_user_template
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE SEQUENCE hibernate_sequence START 1 INCREMENT 1;