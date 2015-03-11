-- INITRANS added because we use serializable transactions http://docs.oracle.com/cd/B14117_01/appdev.101/b10795/adfns_sq.htm#1025374
-- replace ");" with ") INITRANS 30;"

CREATE TABLE m_abstract_role (
  approvalProcess VARCHAR2(255 CHAR),
  requestable     NUMBER(1, 0),
  oid             VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_assignment (
  id                      NUMBER(5, 0)      NOT NULL,
  owner_oid               VARCHAR2(36 CHAR) NOT NULL,
  administrativeStatus    NUMBER(10, 0),
  archiveTimestamp        TIMESTAMP,
  disableReason           VARCHAR2(255 CHAR),
  disableTimestamp        TIMESTAMP,
  effectiveStatus         NUMBER(10, 0),
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          NUMBER(10, 0),
  assignmentOwner         NUMBER(10, 0),
  createChannel           VARCHAR2(255 CHAR),
  createTimestamp         TIMESTAMP,
  creatorRef_relation     VARCHAR2(157 CHAR),
  creatorRef_targetOid    VARCHAR2(36 CHAR),
  creatorRef_type         NUMBER(10, 0),
  modifierRef_relation    VARCHAR2(157 CHAR),
  modifierRef_targetOid   VARCHAR2(36 CHAR),
  modifierRef_type        NUMBER(10, 0),
  modifyChannel           VARCHAR2(255 CHAR),
  modifyTimestamp         TIMESTAMP,
  orderValue              NUMBER(10, 0),
  targetRef_relation      VARCHAR2(157 CHAR),
  targetRef_targetOid     VARCHAR2(36 CHAR),
  targetRef_type          NUMBER(10, 0),
  tenantRef_relation      VARCHAR2(157 CHAR),
  tenantRef_targetOid     VARCHAR2(36 CHAR),
  tenantRef_type          NUMBER(10, 0),
  extId                   NUMBER(5, 0),
  extOid                  VARCHAR2(36 CHAR),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_assignment_ext_date (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(5, 0)       NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  dateValue                    TIMESTAMP          NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
) INITRANS 30;

CREATE TABLE m_assignment_ext_long (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(5, 0)       NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  longValue                    NUMBER(19, 0)      NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
) INITRANS 30;

CREATE TABLE m_assignment_ext_poly (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(5, 0)       NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  orig                         VARCHAR2(255 CHAR) NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  norm                         VARCHAR2(255 CHAR),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
) INITRANS 30;

CREATE TABLE m_assignment_ext_reference (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(5, 0)       NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  targetoid                    VARCHAR2(36 CHAR)  NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  relation                     VARCHAR2(157 CHAR),
  targetType                   NUMBER(10, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
) INITRANS 30;

CREATE TABLE m_assignment_ext_string (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(5, 0)       NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  stringValue                  VARCHAR2(255 CHAR) NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
) INITRANS 30;

CREATE TABLE m_assignment_extension (
  owner_id        NUMBER(5, 0)      NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR) NOT NULL,
  datesCount      NUMBER(5, 0),
  longsCount      NUMBER(5, 0),
  polysCount      NUMBER(5, 0),
  referencesCount NUMBER(5, 0),
  stringsCount    NUMBER(5, 0),
  PRIMARY KEY (owner_id, owner_owner_oid)
) INITRANS 30;

CREATE TABLE m_assignment_reference (
  owner_id        NUMBER(5, 0)       NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  reference_type  NUMBER(10, 0)      NOT NULL,
  relation        VARCHAR2(157 CHAR) NOT NULL,
  targetOid       VARCHAR2(36 CHAR)  NOT NULL,
  containerType   NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
) INITRANS 30;

CREATE TABLE m_audit_delta (
  checksum   VARCHAR2(32 CHAR) NOT NULL,
  record_id  NUMBER(19, 0)     NOT NULL,
  delta      CLOB,
  deltaOid   VARCHAR2(36 CHAR),
  deltaType  NUMBER(10, 0),
  fullResult CLOB,
  status     NUMBER(10, 0),
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

CREATE TABLE m_connector (
  connectorBundle            VARCHAR2(255 CHAR),
  connectorHostRef_relation  VARCHAR2(157 CHAR),
  connectorHostRef_targetOid VARCHAR2(36 CHAR),
  connectorHostRef_type      NUMBER(10, 0),
  connectorType              VARCHAR2(255 CHAR),
  connectorVersion           VARCHAR2(255 CHAR),
  framework                  VARCHAR2(255 CHAR),
  name_norm                  VARCHAR2(255 CHAR),
  name_orig                  VARCHAR2(255 CHAR),
  oid                        VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_connector_host (
  hostname  VARCHAR2(255 CHAR),
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  port      VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR2(36 CHAR) NOT NULL,
  targetSystemType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_exclusion (
  id                  NUMBER(5, 0)      NOT NULL,
  owner_oid           VARCHAR2(36 CHAR) NOT NULL,
  policy              NUMBER(10, 0),
  targetRef_relation  VARCHAR2(157 CHAR),
  targetRef_targetOid VARCHAR2(36 CHAR),
  targetRef_type      NUMBER(10, 0),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_focus (
  administrativeStatus    NUMBER(10, 0),
  archiveTimestamp        TIMESTAMP,
  disableReason           VARCHAR2(255 CHAR),
  disableTimestamp        TIMESTAMP,
  effectiveStatus         NUMBER(10, 0),
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          NUMBER(10, 0),
  oid                     VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR2(255 CHAR),
  name_orig  VARCHAR2(255 CHAR),
  objectType VARCHAR2(255 CHAR),
  oid        VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_lookup_table (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_lookup_table_row (
  id                  NUMBER(5, 0)      NOT NULL,
  owner_oid           VARCHAR2(36 CHAR) NOT NULL,
  row_key             VARCHAR2(255 CHAR),
  label_norm          VARCHAR2(255 CHAR),
  label_orig          VARCHAR2(255 CHAR),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR2(255 CHAR),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_node (
  name_norm      VARCHAR2(255 CHAR),
  name_orig      VARCHAR2(255 CHAR),
  nodeIdentifier VARCHAR2(255 CHAR),
  oid            VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_object (
  oid                   VARCHAR2(36 CHAR) NOT NULL,
  createChannel         VARCHAR2(255 CHAR),
  createTimestamp       TIMESTAMP,
  creatorRef_relation   VARCHAR2(157 CHAR),
  creatorRef_targetOid  VARCHAR2(36 CHAR),
  creatorRef_type       NUMBER(10, 0),
  datesCount            NUMBER(5, 0),
  fullObject            BLOB,
  longsCount            NUMBER(5, 0),
  modifierRef_relation  VARCHAR2(157 CHAR),
  modifierRef_targetOid VARCHAR2(36 CHAR),
  modifierRef_type      NUMBER(10, 0),
  modifyChannel         VARCHAR2(255 CHAR),
  modifyTimestamp       TIMESTAMP,
  name_norm             VARCHAR2(255 CHAR),
  name_orig             VARCHAR2(255 CHAR),
  objectTypeClass       NUMBER(10, 0),
  polysCount            NUMBER(5, 0),
  referencesCount       NUMBER(5, 0),
  stringsCount          NUMBER(5, 0),
  tenantRef_relation    VARCHAR2(157 CHAR),
  tenantRef_targetOid   VARCHAR2(36 CHAR),
  tenantRef_type        NUMBER(10, 0),
  version               NUMBER(10, 0)     NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_object_ext_date (
  eName      VARCHAR2(157 CHAR) NOT NULL,
  owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  ownerType  NUMBER(10, 0)      NOT NULL,
  dateValue  TIMESTAMP          NOT NULL,
  dynamicDef NUMBER(1, 0),
  eType      VARCHAR2(157 CHAR),
  valueType  NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, dateValue)
) INITRANS 30;

CREATE TABLE m_object_ext_long (
  eName      VARCHAR2(157 CHAR) NOT NULL,
  owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  ownerType  NUMBER(10, 0)      NOT NULL,
  longValue  NUMBER(19, 0)      NOT NULL,
  dynamicDef NUMBER(1, 0),
  eType      VARCHAR2(157 CHAR),
  valueType  NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, longValue)
) INITRANS 30;

CREATE TABLE m_object_ext_poly (
  eName      VARCHAR2(157 CHAR) NOT NULL,
  owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  ownerType  NUMBER(10, 0)      NOT NULL,
  orig       VARCHAR2(255 CHAR) NOT NULL,
  dynamicDef NUMBER(1, 0),
  norm       VARCHAR2(255 CHAR),
  eType      VARCHAR2(157 CHAR),
  valueType  NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, orig)
) INITRANS 30;

CREATE TABLE m_object_ext_reference (
  eName      VARCHAR2(157 CHAR) NOT NULL,
  owner_oid  VARCHAR2(36 CHAR)  NOT NULL,
  ownerType  NUMBER(10, 0)      NOT NULL,
  targetoid  VARCHAR2(36 CHAR)  NOT NULL,
  dynamicDef NUMBER(1, 0),
  relation   VARCHAR2(157 CHAR),
  targetType NUMBER(10, 0),
  eType      VARCHAR2(157 CHAR),
  valueType  NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, targetoid)
) INITRANS 30;

CREATE TABLE m_object_ext_string (
  eName       VARCHAR2(157 CHAR) NOT NULL,
  owner_oid   VARCHAR2(36 CHAR)  NOT NULL,
  ownerType   NUMBER(10, 0)      NOT NULL,
  stringValue VARCHAR2(255 CHAR) NOT NULL,
  dynamicDef  NUMBER(1, 0),
  eType       VARCHAR2(157 CHAR),
  valueType   NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, stringValue)
) INITRANS 30;

CREATE TABLE m_object_template (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  type      NUMBER(10, 0),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_org (
  costCenter       VARCHAR2(255 CHAR),
  displayName_norm VARCHAR2(255 CHAR),
  displayName_orig VARCHAR2(255 CHAR),
  displayOrder     NUMBER(10, 0),
  identifier       VARCHAR2(255 CHAR),
  locality_norm    VARCHAR2(255 CHAR),
  locality_orig    VARCHAR2(255 CHAR),
  name_norm        VARCHAR2(255 CHAR),
  name_orig        VARCHAR2(255 CHAR),
  tenant           NUMBER(1, 0),
  oid              VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_org_closure (
  ancestor_oid   VARCHAR2(36 CHAR) NOT NULL,
  descendant_oid VARCHAR2(36 CHAR) NOT NULL,
  val            NUMBER(10, 0),
  PRIMARY KEY (ancestor_oid, descendant_oid)
) INITRANS 30;

CREATE GLOBAL TEMPORARY TABLE m_org_closure_temp_delta (
  descendant_oid VARCHAR2(36 CHAR) NOT NULL,
  ancestor_oid VARCHAR2(36 CHAR) NOT NULL,
  val NUMBER (10, 0) NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
) ON COMMIT DELETE ROWS;

CREATE TABLE m_org_org_type (
  org_oid VARCHAR2(36 CHAR) NOT NULL,
  orgType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_reference (
  owner_oid      VARCHAR2(36 CHAR)  NOT NULL,
  reference_type NUMBER(10, 0)      NOT NULL,
  relation       VARCHAR2(157 CHAR) NOT NULL,
  targetOid      VARCHAR2(36 CHAR)  NOT NULL,
  containerType  NUMBER(10, 0),
  PRIMARY KEY (owner_oid, reference_type, relation, targetOid)
) INITRANS 30;

CREATE TABLE m_report (
  export              NUMBER(10, 0),
  name_norm           VARCHAR2(255 CHAR),
  name_orig           VARCHAR2(255 CHAR),
  orientation         NUMBER(10, 0),
  parent              NUMBER(1, 0),
  useHibernateSession NUMBER(1, 0),
  oid                 VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_report_output (
  name_norm           VARCHAR2(255 CHAR),
  name_orig           VARCHAR2(255 CHAR),
  reportRef_relation  VARCHAR2(157 CHAR),
  reportRef_targetOid VARCHAR2(36 CHAR),
  reportRef_type      NUMBER(10, 0),
  oid                 VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_resource (
  administrativeState        NUMBER(10, 0),
  connectorRef_relation      VARCHAR2(157 CHAR),
  connectorRef_targetOid     VARCHAR2(36 CHAR),
  connectorRef_type          NUMBER(10, 0),
  name_norm                  VARCHAR2(255 CHAR),
  name_orig                  VARCHAR2(255 CHAR),
  o16_lastAvailabilityStatus NUMBER(10, 0),
  oid                        VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_role (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  roleType  VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_security_policy (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_shadow (
  attemptNumber                NUMBER(10, 0),
  dead                         NUMBER(1, 0),
  exist                        NUMBER(1, 0),
  failedOperationType          NUMBER(10, 0),
  fullSynchronizationTimestamp TIMESTAMP,
  intent                       VARCHAR2(255 CHAR),
  kind                         NUMBER(10, 0),
  name_norm                    VARCHAR2(255 CHAR),
  name_orig                    VARCHAR2(255 CHAR),
  objectClass                  VARCHAR2(157 CHAR),
  resourceRef_relation         VARCHAR2(157 CHAR),
  resourceRef_targetOid        VARCHAR2(36 CHAR),
  resourceRef_type             NUMBER(10, 0),
  status                       NUMBER(10, 0),
  synchronizationSituation     NUMBER(10, 0),
  synchronizationTimestamp     TIMESTAMP,
  oid                          VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_system_configuration (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_task (
  binding                NUMBER(10, 0),
  canRunOnNode           VARCHAR2(255 CHAR),
  category               VARCHAR2(255 CHAR),
  completionTimestamp    TIMESTAMP,
  executionStatus        NUMBER(10, 0),
  handlerUri             VARCHAR2(255 CHAR),
  lastRunFinishTimestamp TIMESTAMP,
  lastRunStartTimestamp  TIMESTAMP,
  name_norm              VARCHAR2(255 CHAR),
  name_orig              VARCHAR2(255 CHAR),
  node                   VARCHAR2(255 CHAR),
  objectRef_relation     VARCHAR2(157 CHAR),
  objectRef_targetOid    VARCHAR2(36 CHAR),
  objectRef_type         NUMBER(10, 0),
  ownerRef_relation      VARCHAR2(157 CHAR),
  ownerRef_targetOid     VARCHAR2(36 CHAR),
  ownerRef_type          NUMBER(10, 0),
  parent                 VARCHAR2(255 CHAR),
  recurrence             NUMBER(10, 0),
  status                 NUMBER(10, 0),
  taskIdentifier         VARCHAR2(255 CHAR),
  threadStopAction       NUMBER(10, 0),
  waitingReason          NUMBER(10, 0),
  oid                    VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_task_dependent (
  task_oid  VARCHAR2(36 CHAR) NOT NULL,
  dependent VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_trigger (
  id             NUMBER(5, 0)      NOT NULL,
  owner_oid      VARCHAR2(36 CHAR) NOT NULL,
  handlerUri     VARCHAR2(255 CHAR),
  timestampValue TIMESTAMP,
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_user (
  additionalName_norm  VARCHAR2(255 CHAR),
  additionalName_orig  VARCHAR2(255 CHAR),
  costCenter           VARCHAR2(255 CHAR),
  emailAddress         VARCHAR2(255 CHAR),
  employeeNumber       VARCHAR2(255 CHAR),
  familyName_norm      VARCHAR2(255 CHAR),
  familyName_orig      VARCHAR2(255 CHAR),
  fullName_norm        VARCHAR2(255 CHAR),
  fullName_orig        VARCHAR2(255 CHAR),
  givenName_norm       VARCHAR2(255 CHAR),
  givenName_orig       VARCHAR2(255 CHAR),
  hasPhoto             NUMBER(1, 0)      NOT NULL,
  honorificPrefix_norm VARCHAR2(255 CHAR),
  honorificPrefix_orig VARCHAR2(255 CHAR),
  honorificSuffix_norm VARCHAR2(255 CHAR),
  honorificSuffix_orig VARCHAR2(255 CHAR),
  locale               VARCHAR2(255 CHAR),
  locality_norm        VARCHAR2(255 CHAR),
  locality_orig        VARCHAR2(255 CHAR),
  name_norm            VARCHAR2(255 CHAR),
  name_orig            VARCHAR2(255 CHAR),
  nickName_norm        VARCHAR2(255 CHAR),
  nickName_orig        VARCHAR2(255 CHAR),
  preferredLanguage    VARCHAR2(255 CHAR),
  status               NUMBER(10, 0),
  telephoneNumber      VARCHAR2(255 CHAR),
  timezone             VARCHAR2(255 CHAR),
  title_norm           VARCHAR2(255 CHAR),
  title_orig           VARCHAR2(255 CHAR),
  oid                  VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR2(36 CHAR) NOT NULL,
  employeeType VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_user_organization (
  user_oid VARCHAR2(36 CHAR) NOT NULL,
  norm     VARCHAR2(255 CHAR),
  orig     VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR2(36 CHAR) NOT NULL,
  norm     VARCHAR2(255 CHAR),
  orig     VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_user_photo (
  owner_oid VARCHAR2(36 CHAR) NOT NULL,
  photo     BLOB,
  PRIMARY KEY (owner_oid)
) INITRANS 30;

CREATE TABLE m_value_policy (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE INDEX iRequestable ON m_abstract_role (requestable) INITRANS 30;

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus) INITRANS 30;

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus) INITRANS 30;

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue) INITRANS 30;

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue) INITRANS 30;

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig) INITRANS 30;

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid) INITRANS 30;

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue) INITRANS 30;

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid) INITRANS 30;

ALTER TABLE m_connector_host
ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus) INITRANS 30;

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus) INITRANS 30;

ALTER TABLE m_generic_object
ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_node
ADD CONSTRAINT uc_node_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iObjectNameOrig ON m_object (name_orig) INITRANS 30;

CREATE INDEX iObjectNameNorm ON m_object (name_norm) INITRANS 30;

CREATE INDEX iObjectTypeClass ON m_object (objectTypeClass) INITRANS 30;

CREATE INDEX iObjectCreateTimestamp ON m_object (createTimestamp) INITRANS 30;

CREATE INDEX iExtensionDate ON m_object_ext_date (ownerType, eName, dateValue) INITRANS 30;

CREATE INDEX iExtensionDateDef ON m_object_ext_date (owner_oid, ownerType) INITRANS 30;

CREATE INDEX iExtensionLong ON m_object_ext_long (ownerType, eName, longValue) INITRANS 30;

CREATE INDEX iExtensionLongDef ON m_object_ext_long (owner_oid, ownerType) INITRANS 30;

CREATE INDEX iExtensionPolyString ON m_object_ext_poly (ownerType, eName, orig) INITRANS 30;

CREATE INDEX iExtensionPolyStringDef ON m_object_ext_poly (owner_oid, ownerType) INITRANS 30;

CREATE INDEX iExtensionReference ON m_object_ext_reference (ownerType, eName, targetoid) INITRANS 30;

CREATE INDEX iExtensionReferenceDef ON m_object_ext_reference (owner_oid, ownerType) INITRANS 30;

CREATE INDEX iExtensionString ON m_object_ext_string (ownerType, eName, stringValue) INITRANS 30;

CREATE INDEX iExtensionStringDef ON m_object_ext_string (owner_oid, ownerType) INITRANS 30;

ALTER TABLE m_object_template
ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_org
ADD CONSTRAINT uc_org_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iDisplayOrder ON m_org (displayOrder) INITRANS 30;

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid) INITRANS 30;

CREATE INDEX iDescendant ON m_org_closure (descendant_oid) INITRANS 30;

CREATE INDEX iDescendantAncestor ON m_org_closure (descendant_oid, ancestor_oid) INITRANS 30;

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid) INITRANS 30;

ALTER TABLE m_report
ADD CONSTRAINT uc_report_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iReportParent ON m_report (parent) INITRANS 30;

ALTER TABLE m_resource
ADD CONSTRAINT uc_resource_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_role
ADD CONSTRAINT uc_role_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_security_policy
ADD CONSTRAINT uc_security_policy_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid) INITRANS 30;

CREATE INDEX iShadowDead ON m_shadow (dead) INITRANS 30;

ALTER TABLE m_system_configuration
ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iParent ON m_task (parent) INITRANS 30;

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue) INITRANS 30;

ALTER TABLE m_user
ADD CONSTRAINT uc_user_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber) INITRANS 30;

CREATE INDEX iFullName ON m_user (fullName_orig) INITRANS 30;

CREATE INDEX iFamilyName ON m_user (familyName_orig) INITRANS 30;

CREATE INDEX iGivenName ON m_user (givenName_orig) INITRANS 30;

CREATE INDEX iLocality ON m_user (locality_orig) INITRANS 30;

ALTER TABLE m_value_policy
ADD CONSTRAINT uc_value_policy_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_abstract_role
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (oid)
REFERENCES m_focus;

ALTER TABLE m_assignment
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

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

ALTER TABLE m_assignment_reference
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment;

ALTER TABLE m_audit_delta
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

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

ALTER TABLE m_user_photo
ADD CONSTRAINT fk_user_photo
FOREIGN KEY (owner_oid)
REFERENCES m_user;

ALTER TABLE m_value_policy
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
