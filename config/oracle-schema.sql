-- INITRANS added because we use serializable transactions http://docs.oracle.com/cd/B14117_01/appdev.101/b10795/adfns_sq.htm#1025374
-- replace ");" with ") INITRANS 30;"

CREATE TABLE m_abstract_role (
  approvalExpression    CLOB,
  approvalProcess       VARCHAR2(255 CHAR),
  approvalSchema        CLOB,
  automaticallyApproved CLOB,
  requestable           NUMBER(1, 0),
  id                    NUMBER(19, 0)     NOT NULL,
  oid                   VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_account_shadow (
  accountType              VARCHAR2(255 CHAR),
  allowedIdmAdminGuiAccess NUMBER(1, 0),
  passwordXml              CLOB,
  id                       NUMBER(19, 0)     NOT NULL,
  oid                      VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_any (
  owner_id   NUMBER(19, 0)     NOT NULL,
  owner_oid  VARCHAR2(36 CHAR) NOT NULL,
  owner_type NUMBER(10, 0)     NOT NULL,
  PRIMARY KEY (owner_id, owner_oid, owner_type)
) INITRANS 30;

CREATE TABLE m_any_clob (
  checksum                VARCHAR2(32 CHAR)  NOT NULL,
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  dynamicDef              NUMBER(1, 0),
  clobValue               CLOB,
  valueType               NUMBER(10, 0),
  PRIMARY KEY (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
) INITRANS 30;

CREATE TABLE m_any_date (
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  dateValue               TIMESTAMP          NOT NULL,
  dynamicDef              NUMBER(1, 0),
  valueType               NUMBER(10, 0),
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
) INITRANS 30;

CREATE TABLE m_any_long (
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  longValue               NUMBER(19, 0)      NOT NULL,
  dynamicDef              NUMBER(1, 0),
  valueType               NUMBER(10, 0),
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
) INITRANS 30;

CREATE TABLE m_any_poly_string (
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  orig                    VARCHAR2(255 CHAR) NOT NULL,
  dynamicDef              NUMBER(1, 0),
  norm                    VARCHAR2(255 CHAR),
  valueType               NUMBER(10, 0),
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
) INITRANS 30;

CREATE TABLE m_any_reference (
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  targetoid               VARCHAR2(36 CHAR)  NOT NULL,
  description             CLOB,
  dynamicDef              NUMBER(1, 0),
  filter                  CLOB,
  relation_namespace      VARCHAR2(255 CHAR),
  relation_localPart      VARCHAR2(100 CHAR),
  targetType              NUMBER(10, 0),
  valueType               NUMBER(10, 0),
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
) INITRANS 30;

CREATE TABLE m_any_string (
  name_namespace          VARCHAR2(255 CHAR) NOT NULL,
  name_localPart          VARCHAR2(100 CHAR) NOT NULL,
  anyContainer_owner_id   NUMBER(19, 0)      NOT NULL,
  anyContainer_owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  anyContainer_owner_type NUMBER(10, 0)      NOT NULL,
  type_namespace          VARCHAR2(255 CHAR) NOT NULL,
  type_localPart          VARCHAR2(100 CHAR) NOT NULL,
  stringValue             VARCHAR2(255 CHAR) NOT NULL,
  dynamicDef              NUMBER(1, 0),
  valueType               NUMBER(10, 0),
  PRIMARY KEY (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
) INITRANS 30;

CREATE TABLE m_assignment (
  accountConstruction         CLOB,
  administrativeStatus        NUMBER(10, 0),
  archiveTimestamp            TIMESTAMP,
  disableTimestamp            TIMESTAMP,
  effectiveStatus             NUMBER(10, 0),
  enableTimestamp             TIMESTAMP,
  validFrom                   TIMESTAMP,
  validTo                     TIMESTAMP,
  validityChangeTimestamp     TIMESTAMP,
  validityStatus              NUMBER(10, 0),
  assignmentOwner             NUMBER(10, 0),
  construction                CLOB,
  description                 CLOB,
  owner_id                    NUMBER(19, 0)     NOT NULL,
  owner_oid                   VARCHAR2(36 CHAR) NOT NULL,
  targetRef_description       CLOB,
  targetRef_filter            CLOB,
  targetRef_relationLocalPart VARCHAR2(100 CHAR),
  targetRef_relationNamespace VARCHAR2(255 CHAR),
  targetRef_targetOid         VARCHAR2(36 CHAR),
  targetRef_type              NUMBER(10, 0),
  id                          NUMBER(19, 0)     NOT NULL,
  oid                         VARCHAR2(36 CHAR) NOT NULL,
  extId                       NUMBER(19, 0),
  extOid                      VARCHAR2(36 CHAR),
  extType                     NUMBER(10, 0),
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_audit_delta (
  checksum         VARCHAR2(32 CHAR) NOT NULL,
  record_id        NUMBER(19, 0)     NOT NULL,
  delta            CLOB,
  deltaOid         VARCHAR2(36 CHAR),
  deltaType        NUMBER(10, 0),
  details          CLOB,
  localizedMessage CLOB,
  message          CLOB,
  messageCode      VARCHAR2(255 CHAR),
  operation        CLOB,
  params           CLOB,
  partialResults   CLOB,
  status           NUMBER(10, 0),
  token            NUMBER(19, 0),
  PRIMARY KEY (checksum, record_id)
) INITRANS 30;

CREATE TABLE m_audit_event (
  id                NUMBER(19, 0) NOT NULL,
  channel           VARCHAR2(255 CHAR),
  eventIdentifier   VARCHAR2(255 CHAR),
  eventStage        NUMBER(10, 0),
  eventType         NUMBER(10, 0),
  hostIdentifier    VARCHAR2(255 CHAR),
  initiatorName     VARCHAR2(255 CHAR),
  initiatorOid      VARCHAR2(36 CHAR),
  message           VARCHAR2(1024 CHAR),
  outcome           NUMBER(10, 0),
  parameter         VARCHAR2(255 CHAR),
  result            VARCHAR2(255 CHAR),
  sessionIdentifier VARCHAR2(255 CHAR),
  targetName        VARCHAR2(255 CHAR),
  targetOid         VARCHAR2(36 CHAR),
  targetOwnerName   VARCHAR2(255 CHAR),
  targetOwnerOid    VARCHAR2(36 CHAR),
  targetType        NUMBER(10, 0),
  taskIdentifier    VARCHAR2(255 CHAR),
  taskOID           VARCHAR2(255 CHAR),
  timestampValue    TIMESTAMP,
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_authorization (
  decision    NUMBER(10, 0),
  description CLOB,
  owner_id    NUMBER(19, 0)     NOT NULL,
  owner_oid   VARCHAR2(36 CHAR) NOT NULL,
  id          NUMBER(19, 0)     NOT NULL,
  oid         VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_authorization_action (
  role_id  NUMBER(19, 0)     NOT NULL,
  role_oid VARCHAR2(36 CHAR) NOT NULL,
  action   VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_connector (
  connectorBundle              VARCHAR2(255 CHAR),
  connectorHostRef_description CLOB,
  connectorHostRef_filter      CLOB,
  c16_relationLocalPart        VARCHAR2(100 CHAR),
  c16_relationNamespace        VARCHAR2(255 CHAR),
  connectorHostRef_targetOid   VARCHAR2(36 CHAR),
  connectorHostRef_type        NUMBER(10, 0),
  connectorType                VARCHAR2(255 CHAR),
  connectorVersion             VARCHAR2(255 CHAR),
  framework                    VARCHAR2(255 CHAR),
  name_norm                    VARCHAR2(255 CHAR),
  name_orig                    VARCHAR2(255 CHAR),
  namespace                    VARCHAR2(255 CHAR),
  xmlSchema                    CLOB,
  id                           NUMBER(19, 0)     NOT NULL,
  oid                          VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_connector_host (
  hostname          VARCHAR2(255 CHAR),
  name_norm         VARCHAR2(255 CHAR),
  name_orig         VARCHAR2(255 CHAR),
  port              VARCHAR2(255 CHAR),
  protectConnection NUMBER(1, 0),
  sharedSecret      CLOB,
  timeout           NUMBER(10, 0),
  id                NUMBER(19, 0)     NOT NULL,
  oid               VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_connector_target_system (
  connector_id     NUMBER(19, 0)     NOT NULL,
  connector_oid    VARCHAR2(36 CHAR) NOT NULL,
  targetSystemType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_container (
  id  NUMBER(19, 0)     NOT NULL,
  oid VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_exclusion (
  description                 CLOB,
  owner_id                    NUMBER(19, 0)     NOT NULL,
  owner_oid                   VARCHAR2(36 CHAR) NOT NULL,
  policy                      NUMBER(10, 0),
  targetRef_description       CLOB,
  targetRef_filter            CLOB,
  targetRef_relationLocalPart VARCHAR2(100 CHAR),
  targetRef_relationNamespace VARCHAR2(255 CHAR),
  targetRef_targetOid         VARCHAR2(36 CHAR),
  targetRef_type              NUMBER(10, 0),
  id                          NUMBER(19, 0)     NOT NULL,
  oid                         VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_focus (
  administrativeStatus    NUMBER(10, 0),
  archiveTimestamp        TIMESTAMP,
  disableTimestamp        TIMESTAMP,
  effectiveStatus         NUMBER(10, 0),
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          NUMBER(10, 0),
  id                      NUMBER(19, 0)     NOT NULL,
  oid                     VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR2(255 CHAR),
  name_orig  VARCHAR2(255 CHAR),
  objectType VARCHAR2(255 CHAR),
  id         NUMBER(19, 0)     NOT NULL,
  oid        VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_metadata (
  owner_id                      NUMBER(19, 0)     NOT NULL,
  owner_oid                     VARCHAR2(36 CHAR) NOT NULL,
  createChannel                 VARCHAR2(255 CHAR),
  createTimestamp               TIMESTAMP,
  creatorRef_description        CLOB,
  creatorRef_filter             CLOB,
  creatorRef_relationLocalPart  VARCHAR2(100 CHAR),
  creatorRef_relationNamespace  VARCHAR2(255 CHAR),
  creatorRef_targetOid          VARCHAR2(36 CHAR),
  creatorRef_type               NUMBER(10, 0),
  modifierRef_description       CLOB,
  modifierRef_filter            CLOB,
  modifierRef_relationLocalPart VARCHAR2(100 CHAR),
  modifierRef_relationNamespace VARCHAR2(255 CHAR),
  modifierRef_targetOid         VARCHAR2(36 CHAR),
  modifierRef_type              NUMBER(10, 0),
  modifyChannel                 VARCHAR2(255 CHAR),
  modifyTimestamp               TIMESTAMP,
  PRIMARY KEY (owner_id, owner_oid)
) INITRANS 30;

CREATE TABLE m_node (
  clusteredNode          NUMBER(1, 0),
  hostname               VARCHAR2(255 CHAR),
  internalNodeIdentifier VARCHAR2(255 CHAR),
  jmxPort                NUMBER(10, 0),
  lastCheckInTime        TIMESTAMP,
  name_norm              VARCHAR2(255 CHAR),
  name_orig              VARCHAR2(255 CHAR),
  nodeIdentifier         VARCHAR2(255 CHAR),
  running                NUMBER(1, 0),
  id                     NUMBER(19, 0)     NOT NULL,
  oid                    VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_object (
  description CLOB,
  version     NUMBER(19, 0)     NOT NULL,
  id          NUMBER(19, 0)     NOT NULL,
  oid         VARCHAR2(36 CHAR) NOT NULL,
  extId       NUMBER(19, 0),
  extOid      VARCHAR2(36 CHAR),
  extType     NUMBER(10, 0),
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_object_template (
  accountConstruction CLOB,
  mapping             CLOB,
  name_norm           VARCHAR2(255 CHAR),
  name_orig           VARCHAR2(255 CHAR),
  type                NUMBER(10, 0),
  id                  NUMBER(19, 0)     NOT NULL,
  oid                 VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_operation_result (
  owner_oid        VARCHAR2(36 CHAR) NOT NULL,
  owner_id         NUMBER(19, 0)     NOT NULL,
  details          CLOB,
  localizedMessage CLOB,
  message          CLOB,
  messageCode      VARCHAR2(255 CHAR),
  operation        CLOB,
  params           CLOB,
  partialResults   CLOB,
  status           NUMBER(10, 0),
  token            NUMBER(19, 0),
  PRIMARY KEY (owner_oid, owner_id)
) INITRANS 30;

CREATE TABLE m_org (
  costCenter       VARCHAR2(255 CHAR),
  displayName_norm VARCHAR2(255 CHAR),
  displayName_orig VARCHAR2(255 CHAR),
  identifier       VARCHAR2(255 CHAR),
  locality_norm    VARCHAR2(255 CHAR),
  locality_orig    VARCHAR2(255 CHAR),
  name_norm        VARCHAR2(255 CHAR),
  name_orig        VARCHAR2(255 CHAR),
  id               NUMBER(19, 0)     NOT NULL,
  oid              VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_org_closure (
  id             NUMBER(19, 0) NOT NULL,
  ancestor_id    NUMBER(19, 0),
  ancestor_oid   VARCHAR2(36 CHAR),
  depthValue     NUMBER(10, 0),
  descendant_id  NUMBER(19, 0),
  descendant_oid VARCHAR2(36 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_org_incorrect (
  descendant_oid VARCHAR2(36 CHAR) NOT NULL,
  descendant_id  NUMBER(19, 0)     NOT NULL,
  ancestor_oid   VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (descendant_oid, descendant_id, ancestor_oid)
) INITRANS 30;

CREATE TABLE m_org_org_type (
  org_id  NUMBER(19, 0)     NOT NULL,
  org_oid VARCHAR2(36 CHAR) NOT NULL,
  orgType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_password_policy (
  lifetime     CLOB,
  name_norm    VARCHAR2(255 CHAR),
  name_orig    VARCHAR2(255 CHAR),
  stringPolicy CLOB,
  id           NUMBER(19, 0)     NOT NULL,
  oid          VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_reference (
  reference_type NUMBER(10, 0)      NOT NULL,
  owner_id       NUMBER(19, 0)      NOT NULL,
  owner_oid      VARCHAR2(36 CHAR)  NOT NULL,
  relLocalPart   VARCHAR2(100 CHAR) NOT NULL,
  relNamespace   VARCHAR2(255 CHAR) NOT NULL,
  targetOid      VARCHAR2(36 CHAR)  NOT NULL,
  description    CLOB,
  filter         CLOB,
  containerType  NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
) INITRANS 30;

CREATE TABLE m_resource (
  administrativeState            NUMBER(10, 0),
  capabilities_cachingMetadata   CLOB,
  capabilities_configured        CLOB,
  capabilities_native            CLOB,
  configuration                  CLOB,
  connectorRef_description       CLOB,
  connectorRef_filter            CLOB,
  connectorRef_relationLocalPart VARCHAR2(100 CHAR),
  connectorRef_relationNamespace VARCHAR2(255 CHAR),
  connectorRef_targetOid         VARCHAR2(36 CHAR),
  connectorRef_type              NUMBER(10, 0),
  consistency                    CLOB,
  name_norm                      VARCHAR2(255 CHAR),
  name_orig                      VARCHAR2(255 CHAR),
  namespace                      VARCHAR2(255 CHAR),
  o16_lastAvailabilityStatus     NUMBER(10, 0),
  projection                     CLOB,
  schemaHandling                 CLOB,
  scripts                        CLOB,
  synchronization                CLOB,
  xmlSchema                      CLOB,
  id                             NUMBER(19, 0)     NOT NULL,
  oid                            VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_role (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  roleType  VARCHAR2(255 CHAR),
  id        NUMBER(19, 0)     NOT NULL,
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_shadow (
  administrativeStatus          NUMBER(10, 0),
  archiveTimestamp              TIMESTAMP,
  disableTimestamp              TIMESTAMP,
  effectiveStatus               NUMBER(10, 0),
  enableTimestamp               TIMESTAMP,
  validFrom                     TIMESTAMP,
  validTo                       TIMESTAMP,
  validityChangeTimestamp       TIMESTAMP,
  validityStatus                NUMBER(10, 0),
  assigned                      NUMBER(1, 0),
  attemptNumber                 NUMBER(10, 0),
  dead                          NUMBER(1, 0),
  exist                         NUMBER(1, 0),
  failedOperationType           NUMBER(10, 0),
  intent                        VARCHAR2(255 CHAR),
  iteration                     NUMBER(10, 0),
  iterationToken                VARCHAR2(255 CHAR),
  kind                          NUMBER(10, 0),
  name_norm                     VARCHAR2(255 CHAR),
  name_orig                     VARCHAR2(255 CHAR),
  objectChange                  CLOB,
  class_namespace               VARCHAR2(255 CHAR),
  class_localPart               VARCHAR2(100 CHAR),
  resourceRef_description       CLOB,
  resourceRef_filter            CLOB,
  resourceRef_relationLocalPart VARCHAR2(100 CHAR),
  resourceRef_relationNamespace VARCHAR2(255 CHAR),
  resourceRef_targetOid         VARCHAR2(36 CHAR),
  resourceRef_type              NUMBER(10, 0),
  synchronizationSituation      NUMBER(10, 0),
  synchronizationTimestamp      TIMESTAMP,
  id                            NUMBER(19, 0)     NOT NULL,
  oid                           VARCHAR2(36 CHAR) NOT NULL,
  attrId                        NUMBER(19, 0),
  attrOid                       VARCHAR2(36 CHAR),
  attrType                      NUMBER(10, 0),
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_sync_situation_description (
  checksum       VARCHAR2(32 CHAR) NOT NULL,
  shadow_id      NUMBER(19, 0)     NOT NULL,
  shadow_oid     VARCHAR2(36 CHAR) NOT NULL,
  chanel         VARCHAR2(255 CHAR),
  situation      NUMBER(10, 0),
  timestampValue TIMESTAMP,
  PRIMARY KEY (checksum, shadow_id, shadow_oid)
) INITRANS 30;

CREATE TABLE m_system_configuration (
  cleanupPolicy                  CLOB,
  connectorFramework             CLOB,
  d22_description                CLOB,
  defaultUserTemplateRef_filter  CLOB,
  d22_relationLocalPart          VARCHAR2(100 CHAR),
  d22_relationNamespace          VARCHAR2(255 CHAR),
  d22_targetOid                  VARCHAR2(36 CHAR),
  defaultUserTemplateRef_type    NUMBER(10, 0),
  g36                            CLOB,
  g23_description                CLOB,
  globalPasswordPolicyRef_filter CLOB,
  g23_relationLocalPart          VARCHAR2(100 CHAR),
  g23_relationNamespace          VARCHAR2(255 CHAR),
  g23_targetOid                  VARCHAR2(36 CHAR),
  globalPasswordPolicyRef_type   NUMBER(10, 0),
  logging                        CLOB,
  modelHooks                     CLOB,
  name_norm                      VARCHAR2(255 CHAR),
  name_orig                      VARCHAR2(255 CHAR),
  notificationConfiguration      CLOB,
  id                             NUMBER(19, 0)     NOT NULL,
  oid                            VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_task (
  binding                     NUMBER(10, 0),
  canRunOnNode                VARCHAR2(255 CHAR),
  category                    VARCHAR2(255 CHAR),
  completionTimestamp         TIMESTAMP,
  executionStatus             NUMBER(10, 0),
  handlerUri                  VARCHAR2(255 CHAR),
  lastRunFinishTimestamp      TIMESTAMP,
  lastRunStartTimestamp       TIMESTAMP,
  name_norm                   VARCHAR2(255 CHAR),
  name_orig                   VARCHAR2(255 CHAR),
  node                        VARCHAR2(255 CHAR),
  objectRef_description       CLOB,
  objectRef_filter            CLOB,
  objectRef_relationLocalPart VARCHAR2(100 CHAR),
  objectRef_relationNamespace VARCHAR2(255 CHAR),
  objectRef_targetOid         VARCHAR2(36 CHAR),
  objectRef_type              NUMBER(10, 0),
  otherHandlersUriStack       CLOB,
  ownerRef_description        CLOB,
  ownerRef_filter             CLOB,
  ownerRef_relationLocalPart  VARCHAR2(100 CHAR),
  ownerRef_relationNamespace  VARCHAR2(255 CHAR),
  ownerRef_targetOid          VARCHAR2(36 CHAR),
  ownerRef_type               NUMBER(10, 0),
  parent                      VARCHAR2(255 CHAR),
  progress                    NUMBER(19, 0),
  recurrence                  NUMBER(10, 0),
  resultStatus                NUMBER(10, 0),
  schedule                    CLOB,
  taskIdentifier              VARCHAR2(255 CHAR),
  threadStopAction            NUMBER(10, 0),
  waitingReason               NUMBER(10, 0),
  id                          NUMBER(19, 0)     NOT NULL,
  oid                         VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_task_dependent (
  task_id   NUMBER(19, 0)     NOT NULL,
  task_oid  VARCHAR2(36 CHAR) NOT NULL,
  dependent VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_trigger (
  handlerUri     VARCHAR2(255 CHAR),
  owner_id       NUMBER(19, 0)     NOT NULL,
  owner_oid      VARCHAR2(36 CHAR) NOT NULL,
  timestampValue TIMESTAMP,
  id             NUMBER(19, 0)     NOT NULL,
  oid            VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid)
) INITRANS 30;

CREATE TABLE m_user (
  additionalName_norm      VARCHAR2(255 CHAR),
  additionalName_orig      VARCHAR2(255 CHAR),
  costCenter               VARCHAR2(255 CHAR),
  allowedIdmAdminGuiAccess NUMBER(1, 0),
  passwordXml              CLOB,
  emailAddress             VARCHAR2(255 CHAR),
  employeeNumber           VARCHAR2(255 CHAR),
  familyName_norm          VARCHAR2(255 CHAR),
  familyName_orig          VARCHAR2(255 CHAR),
  fullName_norm            VARCHAR2(255 CHAR),
  fullName_orig            VARCHAR2(255 CHAR),
  givenName_norm           VARCHAR2(255 CHAR),
  givenName_orig           VARCHAR2(255 CHAR),
  honorificPrefix_norm     VARCHAR2(255 CHAR),
  honorificPrefix_orig     VARCHAR2(255 CHAR),
  honorificSuffix_norm     VARCHAR2(255 CHAR),
  honorificSuffix_orig     VARCHAR2(255 CHAR),
  locale                   VARCHAR2(255 CHAR),
  locality_norm            VARCHAR2(255 CHAR),
  locality_orig            VARCHAR2(255 CHAR),
  name_norm                VARCHAR2(255 CHAR),
  name_orig                VARCHAR2(255 CHAR),
  nickName_norm            VARCHAR2(255 CHAR),
  nickName_orig            VARCHAR2(255 CHAR),
  preferredLanguage        VARCHAR2(255 CHAR),
  telephoneNumber          VARCHAR2(255 CHAR),
  timezone                 VARCHAR2(255 CHAR),
  title_norm               VARCHAR2(255 CHAR),
  title_orig               VARCHAR2(255 CHAR),
  id                       NUMBER(19, 0)     NOT NULL,
  oid                      VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE TABLE m_user_employee_type (
  user_id      NUMBER(19, 0)     NOT NULL,
  user_oid     VARCHAR2(36 CHAR) NOT NULL,
  employeeType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_user_organization (
  user_id  NUMBER(19, 0)     NOT NULL,
  user_oid VARCHAR2(36 CHAR) NOT NULL,
  norm     VARCHAR2(255 CHAR),
  orig     VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_user_organizational_unit (
  user_id  NUMBER(19, 0)     NOT NULL,
  user_oid VARCHAR2(36 CHAR) NOT NULL,
  norm     VARCHAR2(255 CHAR),
  orig     VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE INDEX iRequestable ON m_abstract_role (requestable) INITRANS 30;

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

CREATE INDEX iDate ON m_any_date (dateValue) INITRANS 30;

ALTER TABLE m_any_date
ADD CONSTRAINT fk_any_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iLong ON m_any_long (longValue) INITRANS 30;

ALTER TABLE m_any_long
ADD CONSTRAINT fk_any_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iPolyString ON m_any_poly_string (orig) INITRANS 30;

ALTER TABLE m_any_poly_string
ADD CONSTRAINT fk_any_poly_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iTargetOid ON m_any_reference (targetoid) INITRANS 30;

ALTER TABLE m_any_reference
ADD CONSTRAINT fk_any_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iString ON m_any_string (stringValue) INITRANS 30;

ALTER TABLE m_any_string
ADD CONSTRAINT fk_any_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
REFERENCES m_any;

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus) INITRANS 30;

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus) INITRANS 30;

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

CREATE INDEX iConnectorName ON m_connector (name_norm) INITRANS 30;

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

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus) INITRANS 30;

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus) INITRANS 30;

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

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid) INITRANS 30;

CREATE INDEX iShadowAdministrative ON m_shadow (administrativeStatus) INITRANS 30;

CREATE INDEX iShadowEffective ON m_shadow (effectiveStatus) INITRANS 30;

CREATE INDEX iShadowName ON m_shadow (name_norm) INITRANS 30;

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

CREATE INDEX iTaskName ON m_task (name_norm) INITRANS 30;

ALTER TABLE m_task
ADD CONSTRAINT fk_task
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_task_dependent
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_id, task_oid)
REFERENCES m_task;

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue) INITRANS 30;

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger
FOREIGN KEY (id, oid)
REFERENCES m_container;

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

CREATE INDEX iFullName ON m_user (fullName_norm) INITRANS 30;

CREATE INDEX iLocality ON m_user (locality_norm) INITRANS 30;

CREATE INDEX iHonorificSuffix ON m_user (honorificSuffix_norm) INITRANS 30;

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber) INITRANS 30;

CREATE INDEX iGivenName ON m_user (givenName_norm) INITRANS 30;

CREATE INDEX iFamilyName ON m_user (familyName_norm) INITRANS 30;

CREATE INDEX iAdditionalName ON m_user (additionalName_norm) INITRANS 30;

CREATE INDEX iHonorificPrefix ON m_user (honorificPrefix_norm) INITRANS 30;

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
