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
  fullSynchronizationTimestamp  TIMESTAMP,
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
  fullFlag       NUMBER(1, 0),
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
  profilingConfiguration         CLOB,
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

CREATE TABLE m_value_policy (
  lifetime     CLOB,
  name_norm    VARCHAR2(255 CHAR),
  name_orig    VARCHAR2(255 CHAR),
  stringPolicy CLOB,
  id           NUMBER(19, 0)     NOT NULL,
  oid          VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
) INITRANS 30;

CREATE INDEX iRequestable ON m_abstract_role (requestable) INITRANS 30;

ALTER TABLE m_abstract_role
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (id, oid)
REFERENCES m_focus;

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

    create table m_abstract_role (
        approvalExpression clob,
        approvalProcess varchar2(255 char),
        approvalSchema clob,
        automaticallyApproved clob,
        requestable number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_account_shadow (
        accountType varchar2(255 char),
        allowedIdmAdminGuiAccess number(1,0),
        passwordXml clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        owner_type number(10,0) not null,
        primary key (owner_id, owner_oid, owner_type)
    );

    create table m_any_clob (
        checksum varchar2(32 char) not null,
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        dynamicDef number(1,0),
        clobValue clob,
        valueType number(10,0),
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    );

    create table m_any_date (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        dateValue timestamp not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    );

    create table m_any_long (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        longValue number(19,0) not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    );

    create table m_any_poly_string (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        orig varchar2(255 char) not null,
        dynamicDef number(1,0),
        norm varchar2(255 char),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    );

    create table m_any_reference (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        targetoid varchar2(36 char) not null,
        description clob,
        dynamicDef number(1,0),
        filter clob,
        relation_namespace varchar2(255 char),
        relation_localPart varchar2(100 char),
        targetType number(10,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    );

    create table m_any_string (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        stringValue varchar2(255 char) not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    );

    create table m_assignment (
        accountConstruction clob,
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        assignmentOwner number(10,0),
        construction clob,
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar2(100 char),
        targetRef_relationNamespace varchar2(255 char),
        targetRef_targetOid varchar2(36 char),
        targetRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    );

    create table m_audit_delta (
        checksum varchar2(32 char) not null,
        record_id number(19,0) not null,
        delta clob,
        deltaOid varchar2(36 char),
        deltaType number(10,0),
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar2(255 char),
        operation clob,
        params clob,
        partialResults clob,
        status number(10,0),
        token number(19,0),
        primary key (checksum, record_id)
    );

    create table m_audit_event (
        id number(19,0) not null,
        channel varchar2(255 char),
        eventIdentifier varchar2(255 char),
        eventStage number(10,0),
        eventType number(10,0),
        hostIdentifier varchar2(255 char),
        initiatorName varchar2(255 char),
        initiatorOid varchar2(36 char),
        message varchar2(1024 char),
        outcome number(10,0),
        parameter varchar2(255 char),
        result varchar2(255 char),
        sessionIdentifier varchar2(255 char),
        targetName varchar2(255 char),
        targetOid varchar2(36 char),
        targetOwnerName varchar2(255 char),
        targetOwnerOid varchar2(36 char),
        targetType number(10,0),
        taskIdentifier varchar2(255 char),
        taskOID varchar2(255 char),
        timestampValue timestamp,
        primary key (id)
    );

    create table m_authorization (
        decision number(10,0),
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_authorization_action (
        role_id number(19,0) not null,
        role_oid varchar2(36 char) not null,
        action varchar2(255 char)
    );

    create table m_connector (
        connectorBundle varchar2(255 char),
        connectorHostRef_description clob,
        connectorHostRef_filter clob,
        c16_relationLocalPart varchar2(100 char),
        c16_relationNamespace varchar2(255 char),
        connectorHostRef_targetOid varchar2(36 char),
        connectorHostRef_type number(10,0),
        connectorType varchar2(255 char),
        connectorVersion varchar2(255 char),
        framework varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        namespace varchar2(255 char),
        xmlSchema clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        port varchar2(255 char),
        protectConnection number(1,0),
        sharedSecret clob,
        timeout number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id number(19,0) not null,
        connector_oid varchar2(36 char) not null,
        targetSystemType varchar2(255 char)
    );

    create table m_container (
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        policy number(10,0),
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar2(100 char),
        targetRef_relationNamespace varchar2(255 char),
        targetRef_targetOid varchar2(36 char),
        targetRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_focus (
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectType varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_metadata (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        createChannel varchar2(255 char),
        createTimestamp timestamp,
        creatorRef_description clob,
        creatorRef_filter clob,
        creatorRef_relationLocalPart varchar2(100 char),
        creatorRef_relationNamespace varchar2(255 char),
        creatorRef_targetOid varchar2(36 char),
        creatorRef_type number(10,0),
        modifierRef_description clob,
        modifierRef_filter clob,
        modifierRef_relationLocalPart varchar2(100 char),
        modifierRef_relationNamespace varchar2(255 char),
        modifierRef_targetOid varchar2(36 char),
        modifierRef_type number(10,0),
        modifyChannel varchar2(255 char),
        modifyTimestamp timestamp,
        primary key (owner_id, owner_oid)
    );

    create table m_node (
        clusteredNode number(1,0),
        hostname varchar2(255 char),
        internalNodeIdentifier varchar2(255 char),
        jmxPort number(10,0),
        lastCheckInTime timestamp,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        nodeIdentifier varchar2(255 char),
        running number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description clob,
        version number(19,0) not null,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    );

    create table m_object_template (
        accountConstruction clob,
        mapping clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_operation_result (
        owner_oid varchar2(36 char) not null,
        owner_id number(19,0) not null,
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar2(255 char),
        operation clob,
        params clob,
        partialResults clob,
        status number(10,0),
        token number(19,0),
        primary key (owner_oid, owner_id)
    );

    create table m_org (
        costCenter varchar2(255 char),
        displayName_norm varchar2(255 char),
        displayName_orig varchar2(255 char),
        identifier varchar2(255 char),
        locality_norm varchar2(255 char),
        locality_orig varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_org_closure (
        id number(19,0) not null,
        ancestor_id number(19,0),
        ancestor_oid varchar2(36 char),
        depthValue number(10,0),
        descendant_id number(19,0),
        descendant_oid varchar2(36 char),
        primary key (id)
    );

    create table m_org_incorrect (
        descendant_oid varchar2(36 char) not null,
        descendant_id number(19,0) not null,
        ancestor_oid varchar2(36 char) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    );

    create table m_org_org_type (
        org_id number(19,0) not null,
        org_oid varchar2(36 char) not null,
        orgType varchar2(255 char)
    );

    create table m_reference (
        reference_type number(10,0) not null,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        relLocalPart varchar2(100 char) not null,
        relNamespace varchar2(255 char) not null,
        targetOid varchar2(36 char) not null,
        description clob,
        filter clob,
        containerType number(10,0),
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    );

    create table m_resource (
        administrativeState number(10,0),
        capabilities_cachingMetadata clob,
        capabilities_configured clob,
        capabilities_native clob,
        configuration clob,
        connectorRef_description clob,
        connectorRef_filter clob,
        connectorRef_relationLocalPart varchar2(100 char),
        connectorRef_relationNamespace varchar2(255 char),
        connectorRef_targetOid varchar2(36 char),
        connectorRef_type number(10,0),
        consistency clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        namespace varchar2(255 char),
        o16_lastAvailabilityStatus number(10,0),
        projection clob,
        schemaHandling clob,
        scripts clob,
        synchronization clob,
        xmlSchema clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_role (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        roleType varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_shadow (
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        assigned number(1,0),
        attemptNumber number(10,0),
        dead number(1,0),
        exist number(1,0),
        failedOperationType number(10,0),
        intent varchar2(255 char),
        iteration number(10,0),
        iterationToken varchar2(255 char),
        kind number(10,0),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectChange clob,
        class_namespace varchar2(255 char),
        class_localPart varchar2(100 char),
        resourceRef_description clob,
        resourceRef_filter clob,
        resourceRef_relationLocalPart varchar2(100 char),
        resourceRef_relationNamespace varchar2(255 char),
        resourceRef_targetOid varchar2(36 char),
        resourceRef_type number(10,0),
        synchronizationSituation number(10,0),
        synchronizationTimestamp timestamp,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        attrId number(19,0),
        attrOid varchar2(36 char),
        attrType number(10,0),
        primary key (id, oid)
    );

    create table m_sync_situation_description (
        checksum varchar2(32 char) not null,
        shadow_id number(19,0) not null,
        shadow_oid varchar2(36 char) not null,
        chanel varchar2(255 char),
        situation number(10,0),
        timestampValue timestamp,
        primary key (checksum, shadow_id, shadow_oid)
    );

    create table m_system_configuration (
        cleanupPolicy clob,
        connectorFramework clob,
        d22_description clob,
        defaultUserTemplateRef_filter clob,
        d22_relationLocalPart varchar2(100 char),
        d22_relationNamespace varchar2(255 char),
        d22_targetOid varchar2(36 char),
        defaultUserTemplateRef_type number(10,0),
        g36 clob,
        g23_description clob,
        globalPasswordPolicyRef_filter clob,
        g23_relationLocalPart varchar2(100 char),
        g23_relationNamespace varchar2(255 char),
        g23_targetOid varchar2(36 char),
        globalPasswordPolicyRef_type number(10,0),
        logging clob,
        modelHooks clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        notificationConfiguration clob,
        profilingConfiguration clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding number(10,0),
        canRunOnNode varchar2(255 char),
        category varchar2(255 char),
        completionTimestamp timestamp,
        executionStatus number(10,0),
        handlerUri varchar2(255 char),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        node varchar2(255 char),
        objectRef_description clob,
        objectRef_filter clob,
        objectRef_relationLocalPart varchar2(100 char),
        objectRef_relationNamespace varchar2(255 char),
        objectRef_targetOid varchar2(36 char),
        objectRef_type number(10,0),
        otherHandlersUriStack clob,
        ownerRef_description clob,
        ownerRef_filter clob,
        ownerRef_relationLocalPart varchar2(100 char),
        ownerRef_relationNamespace varchar2(255 char),
        ownerRef_targetOid varchar2(36 char),
        ownerRef_type number(10,0),
        parent varchar2(255 char),
        progress number(19,0),
        recurrence number(10,0),
        resultStatus number(10,0),
        schedule clob,
        taskIdentifier varchar2(255 char),
        threadStopAction number(10,0),
        waitingReason number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_task_dependent (
        task_id number(19,0) not null,
        task_oid varchar2(36 char) not null,
        dependent varchar2(255 char)
    );

    create table m_trigger (
        handlerUri varchar2(255 char),
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        timestampValue timestamp,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_user (
        additionalName_norm varchar2(255 char),
        additionalName_orig varchar2(255 char),
        costCenter varchar2(255 char),
        allowedIdmAdminGuiAccess number(1,0),
        passwordXml clob,
        emailAddress varchar2(255 char),
        employeeNumber varchar2(255 char),
        familyName_norm varchar2(255 char),
        familyName_orig varchar2(255 char),
        fullName_norm varchar2(255 char),
        fullName_orig varchar2(255 char),
        givenName_norm varchar2(255 char),
        givenName_orig varchar2(255 char),
        honorificPrefix_norm varchar2(255 char),
        honorificPrefix_orig varchar2(255 char),
        honorificSuffix_norm varchar2(255 char),
        honorificSuffix_orig varchar2(255 char),
        locale varchar2(255 char),
        locality_norm varchar2(255 char),
        locality_orig varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        nickName_norm varchar2(255 char),
        nickName_orig varchar2(255 char),
        preferredLanguage varchar2(255 char),
        telephoneNumber varchar2(255 char),
        timezone varchar2(255 char),
        title_norm varchar2(255 char),
        title_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_user_employee_type (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        employeeType varchar2(255 char)
    );

    create table m_user_organization (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        norm varchar2(255 char),
        orig varchar2(255 char)
    );

    create table m_user_organizational_unit (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        norm varchar2(255 char),
        orig varchar2(255 char)
    );

    create table m_value_policy (
        lifetime clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        stringPolicy clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create index iRequestable on m_abstract_role (requestable);

    alter table m_abstract_role 
        add constraint fk_abstract_role 
        foreign key (id, oid) 
        references m_focus;

    alter table m_account_shadow 
        add constraint fk_account_shadow 
        foreign key (id, oid) 
        references m_shadow;

    alter table m_any_clob 
        add constraint fk_any_clob 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iDate on m_any_date (dateValue);

    alter table m_any_date 
        add constraint fk_any_date 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iLong on m_any_long (longValue);

    alter table m_any_long 
        add constraint fk_any_long 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iPolyString on m_any_poly_string (orig);

    alter table m_any_poly_string 
        add constraint fk_any_poly_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iTargetOid on m_any_reference (targetoid);

    alter table m_any_reference 
        add constraint fk_any_reference 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iString on m_any_string (stringValue);

    alter table m_any_string 
        add constraint fk_any_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iAssignmentAdministrative on m_assignment (administrativeStatus);

    create index iAssignmentEffective on m_assignment (effectiveStatus);

    alter table m_assignment 
        add constraint fk_assignment 
        foreign key (id, oid) 
        references m_container;

    alter table m_assignment 
        add constraint fk_assignment_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_audit_delta 
        add constraint fk_audit_delta 
        foreign key (record_id) 
        references m_audit_event;

    alter table m_authorization 
        add constraint fk_authorization 
        foreign key (id, oid) 
        references m_container;

    alter table m_authorization 
        add constraint fk_authorization_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_authorization_action 
        add constraint fk_authorization_action 
        foreign key (role_id, role_oid) 
        references m_authorization;
CREATE INDEX iConnectorNameNorm ON m_connector (name_norm) INITRANS 30;

CREATE INDEX iConnectorNameOrig ON m_connector (name_orig) INITRANS 30;

ALTER TABLE m_connector
ADD CONSTRAINT fk_connector
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iConnectorHostName ON m_connector_host (name_orig) INITRANS 30;

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

CREATE INDEX iGenericObjectName ON m_generic_object (name_orig) INITRANS 30;

ALTER TABLE m_generic_object
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_metadata
ADD CONSTRAINT fk_metadata_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container;

CREATE INDEX iNodeName ON m_node (name_orig) INITRANS 30;

ALTER TABLE m_node
ADD CONSTRAINT fk_node
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_object
ADD CONSTRAINT fk_object
FOREIGN KEY (id, oid)
REFERENCES m_container;

CREATE INDEX iObjectTemplate ON m_object_template (name_orig) INITRANS 30;

ALTER TABLE m_object_template
ADD CONSTRAINT fk_object_template
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_operation_result
ADD CONSTRAINT fk_result_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_object;

CREATE INDEX iOrgName ON m_org (name_orig) INITRANS 30;

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

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid) INITRANS 30;

ALTER TABLE m_reference
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_id, owner_oid)
REFERENCES m_container;

CREATE INDEX iResourceName ON m_resource (name_orig) INITRANS 30;

ALTER TABLE m_resource
ADD CONSTRAINT fk_resource
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iRoleName ON m_role (name_orig) INITRANS 30;

ALTER TABLE m_role
ADD CONSTRAINT fk_role
FOREIGN KEY (id, oid)
REFERENCES m_abstract_role;

CREATE INDEX iShadowNameOrig ON m_shadow (name_orig) INITRANS 30;

CREATE INDEX iShadowDead ON m_shadow (dead) INITRANS 30;

CREATE INDEX iShadowNameNorm ON m_shadow (name_norm) INITRANS 30;

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid) INITRANS 30;

CREATE INDEX iShadowAdministrative ON m_shadow (administrativeStatus) INITRANS 30;

CREATE INDEX iShadowEffective ON m_shadow (effectiveStatus) INITRANS 30;

ALTER TABLE m_shadow
ADD CONSTRAINT fk_shadow
FOREIGN KEY (id, oid)
REFERENCES m_object;

ALTER TABLE m_sync_situation_description
ADD CONSTRAINT fk_shadow_sync_situation
FOREIGN KEY (shadow_id, shadow_oid)
REFERENCES m_shadow;

CREATE INDEX iSystemConfigurationName ON m_system_configuration (name_orig) INITRANS 30;

ALTER TABLE m_system_configuration
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE INDEX iTaskNameNameNorm ON m_task (name_norm) INITRANS 30;

CREATE INDEX iTaskNameOrig ON m_task (name_orig) INITRANS 30;

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

CREATE INDEX iFullName ON m_user (fullName_orig) INITRANS 30;

CREATE INDEX iLocality ON m_user (locality_orig) INITRANS 30;

CREATE INDEX iHonorificSuffix ON m_user (honorificSuffix_orig) INITRANS 30;

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber) INITRANS 30;

CREATE INDEX iGivenName ON m_user (givenName_orig) INITRANS 30;

CREATE INDEX iFamilyName ON m_user (familyName_orig) INITRANS 30;

CREATE INDEX iAdditionalName ON m_user (additionalName_orig) INITRANS 30;

CREATE INDEX iHonorificPrefix ON m_user (honorificPrefix_orig) INITRANS 30;

CREATE INDEX iUserName ON m_user (name_orig) INITRANS 30;

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

CREATE INDEX iValuePolicy ON m_value_policy (name_orig) INITRANS 30;

ALTER TABLE m_value_policy
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (id, oid)
REFERENCES m_object;

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
