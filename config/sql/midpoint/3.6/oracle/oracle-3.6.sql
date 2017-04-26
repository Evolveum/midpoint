-- INITRANS added because we use serializable transactions http://docs.oracle.com/cd/B14117_01/appdev.101/b10795/adfns_sq.htm#1025374
-- replace ");" with ") INITRANS 30;"

CREATE TABLE m_abstract_role (
  approvalProcess    VARCHAR2(255 CHAR),
  displayName_norm   VARCHAR2(255 CHAR),
  displayName_orig   VARCHAR2(255 CHAR),
  identifier         VARCHAR2(255 CHAR),
  ownerRef_relation  VARCHAR2(157 CHAR),
  ownerRef_targetOid VARCHAR2(36 CHAR),
  ownerRef_type      NUMBER(10, 0),
  requestable        NUMBER(1, 0),
  riskLevel          VARCHAR2(255 CHAR),
  oid                VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_campaign (
  definitionRef_relation  VARCHAR2(157 CHAR),
  definitionRef_targetOid VARCHAR2(36 CHAR),
  definitionRef_type      NUMBER(10, 0),
  endTimestamp            TIMESTAMP,
  handlerUri              VARCHAR2(255 CHAR),
  name_norm               VARCHAR2(255 CHAR),
  name_orig               VARCHAR2(255 CHAR),
  ownerRef_relation       VARCHAR2(157 CHAR),
  ownerRef_targetOid      VARCHAR2(36 CHAR),
  ownerRef_type           NUMBER(10, 0),
  stageNumber             NUMBER(10, 0),
  startTimestamp          TIMESTAMP,
  state                   NUMBER(10, 0),
  oid                     VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_case (
  id                       NUMBER(10, 0)     NOT NULL,
  owner_oid                VARCHAR2(36 CHAR) NOT NULL,
  administrativeStatus     NUMBER(10, 0),
  archiveTimestamp         TIMESTAMP,
  disableReason            VARCHAR2(255 CHAR),
  disableTimestamp         TIMESTAMP,
  effectiveStatus          NUMBER(10, 0),
  enableTimestamp          TIMESTAMP,
  validFrom                TIMESTAMP,
  validTo                  TIMESTAMP,
  validityChangeTimestamp  TIMESTAMP,
  validityStatus           NUMBER(10, 0),
  currentStageOutcome      VARCHAR2(255 CHAR),
  fullObject               BLOB,
  objectRef_relation       VARCHAR2(157 CHAR),
  objectRef_targetOid      VARCHAR2(36 CHAR),
  objectRef_type           NUMBER(10, 0),
  orgRef_relation          VARCHAR2(157 CHAR),
  orgRef_targetOid         VARCHAR2(36 CHAR),
  orgRef_type              NUMBER(10, 0),
  outcome                  VARCHAR2(255 CHAR),
  remediedTimestamp        TIMESTAMP,
  reviewDeadline           TIMESTAMP,
  reviewRequestedTimestamp TIMESTAMP,
  stageNumber              NUMBER(10, 0),
  targetRef_relation       VARCHAR2(157 CHAR),
  targetRef_targetOid      VARCHAR2(36 CHAR),
  targetRef_type           NUMBER(10, 0),
  tenantRef_relation       VARCHAR2(157 CHAR),
  tenantRef_targetOid      VARCHAR2(36 CHAR),
  tenantRef_type           NUMBER(10, 0),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_definition (
  handlerUri                   VARCHAR2(255 CHAR),
  lastCampaignClosedTimestamp  TIMESTAMP,
  lastCampaignStartedTimestamp TIMESTAMP,
  name_norm                    VARCHAR2(255 CHAR),
  name_orig                    VARCHAR2(255 CHAR),
  ownerRef_relation            VARCHAR2(157 CHAR),
  ownerRef_targetOid           VARCHAR2(36 CHAR),
  ownerRef_type                NUMBER(10, 0),
  oid                          VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_wi (
  id                     NUMBER(10, 0)     NOT NULL,
  owner_id               NUMBER(10, 0)     NOT NULL,
  owner_owner_oid        VARCHAR2(36 CHAR) NOT NULL,
  closeTimestamp         TIMESTAMP,
  outcome                VARCHAR2(255 CHAR),
  outputChangeTimestamp  TIMESTAMP,
  performerRef_relation  VARCHAR2(157 CHAR),
  performerRef_targetOid VARCHAR2(36 CHAR),
  performerRef_type      NUMBER(10, 0),
  stageNumber            NUMBER(10, 0),
  PRIMARY KEY (id, owner_id, owner_owner_oid)
) INITRANS 30;

CREATE TABLE m_acc_cert_wi_reference (
  owner_id              NUMBER(10, 0)      NOT NULL,
  owner_owner_id        NUMBER(10, 0)      NOT NULL,
  owner_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  relation              VARCHAR2(157 CHAR) NOT NULL,
  targetOid             VARCHAR2(36 CHAR)  NOT NULL,
  targetType            NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)
) INITRANS 30;

CREATE TABLE m_assignment (
  id                      NUMBER(10, 0)     NOT NULL,
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
  lifecycleState          VARCHAR2(255 CHAR),
  modifierRef_relation    VARCHAR2(157 CHAR),
  modifierRef_targetOid   VARCHAR2(36 CHAR),
  modifierRef_type        NUMBER(10, 0),
  modifyChannel           VARCHAR2(255 CHAR),
  modifyTimestamp         TIMESTAMP,
  orderValue              NUMBER(10, 0),
  orgRef_relation         VARCHAR2(157 CHAR),
  orgRef_targetOid        VARCHAR2(36 CHAR),
  orgRef_type             NUMBER(10, 0),
  resourceRef_relation    VARCHAR2(157 CHAR),
  resourceRef_targetOid   VARCHAR2(36 CHAR),
  resourceRef_type        NUMBER(10, 0),
  targetRef_relation      VARCHAR2(157 CHAR),
  targetRef_targetOid     VARCHAR2(36 CHAR),
  targetRef_type          NUMBER(10, 0),
  tenantRef_relation      VARCHAR2(157 CHAR),
  tenantRef_targetOid     VARCHAR2(36 CHAR),
  tenantRef_type          NUMBER(10, 0),
  extId                   NUMBER(10, 0),
  extOid                  VARCHAR2(36 CHAR),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_assignment_ext_boolean (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  booleanValue                 NUMBER(1, 0)       NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue)
) INITRANS 30;

CREATE TABLE m_assignment_ext_date (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
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
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
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
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
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
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
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
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  stringValue                  VARCHAR2(255 CHAR) NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
) INITRANS 30;

CREATE TABLE m_assignment_extension (
  owner_id        NUMBER(10, 0)     NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR) NOT NULL,
  booleansCount   NUMBER(5, 0),
  datesCount      NUMBER(5, 0),
  longsCount      NUMBER(5, 0),
  polysCount      NUMBER(5, 0),
  referencesCount NUMBER(5, 0),
  stringsCount    NUMBER(5, 0),
  PRIMARY KEY (owner_id, owner_owner_oid)
) INITRANS 30;

CREATE TABLE m_assignment_policy_situation (
  assignment_id   NUMBER(10, 0)     NOT NULL,
  assignment_oid  VARCHAR2(36 CHAR) NOT NULL,
  policySituation VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_assignment_reference (
  owner_id        NUMBER(10, 0)      NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  reference_type  NUMBER(10, 0)      NOT NULL,
  relation        VARCHAR2(157 CHAR) NOT NULL,
  targetOid       VARCHAR2(36 CHAR)  NOT NULL,
  targetType      NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid)
) INITRANS 30;

CREATE TABLE m_audit_delta (
  checksum   VARCHAR2(32 CHAR) NOT NULL,
  record_id  NUMBER(19, 0)     NOT NULL,
  delta      CLOB,
  deltaOid   VARCHAR2(36 CHAR),
  deltaType  NUMBER(10, 0),
  fullResult CLOB,
  objectName_norm   VARCHAR2(255 CHAR),
  objectName_orig   VARCHAR2(255 CHAR),
  resourceName_norm VARCHAR2(255 CHAR),
  resourceName_orig VARCHAR2(255 CHAR),
  resourceOid       VARCHAR2(36 CHAR),
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

CREATE TABLE m_audit_item (
  changedItemPath VARCHAR2(900 CHAR) NOT NULL,
  record_id       NUMBER(19, 0)      NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
) INITRANS 30;

CREATE TABLE m_audit_prop_value (
  id        NUMBER(19, 0) NOT NULL,
  name      VARCHAR2(255 CHAR),
  record_id NUMBER(19, 0),
  value     VARCHAR2(1024 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_audit_ref_value (
  id              NUMBER(19, 0) NOT NULL,
  name            VARCHAR2(255 CHAR),
  oid             VARCHAR2(255 CHAR),
  record_id       NUMBER(19, 0),
  targetName_norm VARCHAR2(255 CHAR),
  targetName_orig VARCHAR2(255 CHAR),
  type            VARCHAR2(255 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_case (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
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
  id                  NUMBER(10, 0)     NOT NULL,
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
  hasPhoto                NUMBER(1, 0) DEFAULT 0 NOT NULL,
  oid                     VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_focus_photo (
  owner_oid VARCHAR2(36 CHAR) NOT NULL,
  photo     BLOB,
  PRIMARY KEY (owner_oid)
) INITRANS 30;

CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR2(36 CHAR) NOT NULL,
  policySituation VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_form (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
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
  id                  NUMBER(10, 0)     NOT NULL,
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
  booleansCount         NUMBER(5, 0),
  createChannel         VARCHAR2(255 CHAR),
  createTimestamp       TIMESTAMP,
  creatorRef_relation   VARCHAR2(157 CHAR),
  creatorRef_targetOid  VARCHAR2(36 CHAR),
  creatorRef_type       NUMBER(10, 0),
  datesCount            NUMBER(5, 0),
  fullObject            BLOB,
  lifecycleState        VARCHAR2(255 CHAR),
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

CREATE TABLE m_object_ext_boolean (
  eName        VARCHAR2(157 CHAR) NOT NULL,
  owner_oid    VARCHAR2(36 CHAR)  NOT NULL,
  ownerType    NUMBER(10, 0)      NOT NULL,
  booleanValue NUMBER(1, 0)       NOT NULL,
  dynamicDef   NUMBER(1, 0),
  eType        VARCHAR2(157 CHAR),
  valueType    NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, booleanValue)
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

CREATE TABLE m_object_text_info (
  owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  text      VARCHAR2(255 CHAR) NOT NULL,
  PRIMARY KEY (owner_oid, text)
) INITRANS 30;

CREATE TABLE m_org (
  costCenter       VARCHAR2(255 CHAR),
  displayOrder     NUMBER(10, 0),
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
  targetType     NUMBER(10, 0),
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

CREATE TABLE m_sequence (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_service (
  displayOrder  NUMBER(10, 0),
  locality_norm VARCHAR2(255 CHAR),
  locality_orig VARCHAR2(255 CHAR),
  name_norm     VARCHAR2(255 CHAR),
  name_orig     VARCHAR2(255 CHAR),
  oid           VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_service_type (
  service_oid VARCHAR2(36 CHAR) NOT NULL,
  serviceType VARCHAR2(255 CHAR)
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
  pendingOperationCount        NUMBER(10, 0),
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
  wfEndTimestamp           TIMESTAMP,
  wfObjectRef_relation     VARCHAR2(157 CHAR),
  wfObjectRef_targetOid    VARCHAR2(36 CHAR),
  wfObjectRef_type         NUMBER(10, 0),
  wfProcessInstanceId      VARCHAR2(255 CHAR),
  wfRequesterRef_relation  VARCHAR2(157 CHAR),
  wfRequesterRef_targetOid VARCHAR2(36 CHAR),
  wfRequesterRef_type      NUMBER(10, 0),
  wfStartTimestamp         TIMESTAMP,
  wfTargetRef_relation     VARCHAR2(157 CHAR),
  wfTargetRef_targetOid    VARCHAR2(36 CHAR),
  wfTargetRef_type         NUMBER(10, 0),
  oid                    VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_task_dependent (
  task_oid  VARCHAR2(36 CHAR) NOT NULL,
  dependent VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_trigger (
  id             NUMBER(10, 0)     NOT NULL,
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

CREATE TABLE m_value_policy (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier) INITRANS 30;

CREATE INDEX iRequestable ON m_abstract_role (requestable) INITRANS 30;

ALTER TABLE m_acc_cert_campaign
    ADD CONSTRAINT uc_acc_cert_campaign_name  UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iCaseObjectRefTargetOid ON m_acc_cert_case (objectRef_targetOid) INITRANS 30;

CREATE INDEX iCaseTargetRefTargetOid ON m_acc_cert_case (targetRef_targetOid) INITRANS 30;

CREATE INDEX iCaseTenantRefTargetOid ON m_acc_cert_case (tenantRef_targetOid) INITRANS 30;

CREATE INDEX iCaseOrgRefTargetOid ON m_acc_cert_case (orgRef_targetOid) INITRANS 30;

ALTER TABLE m_acc_cert_definition
    ADD CONSTRAINT uc_acc_cert_definition_name  UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iCertWorkItemRefTargetOid ON m_acc_cert_wi_reference (targetOid) INITRANS 30;

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus) INITRANS 30;

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus) INITRANS 30;

CREATE INDEX iTargetRefTargetOid ON m_assignment (targetRef_targetOid) INITRANS 30;

CREATE INDEX iTenantRefTargetOid ON m_assignment (tenantRef_targetOid) INITRANS 30;

CREATE INDEX iOrgRefTargetOid ON m_assignment (orgRef_targetOid) INITRANS 30;

CREATE INDEX iResourceRefTargetOid ON m_assignment (resourceRef_targetOid) INITRANS 30;

CREATE INDEX iAExtensionBoolean ON m_assignment_ext_boolean (extensionType, eName, booleanValue) INITRANS 30;

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue) INITRANS 30;

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue) INITRANS 30;

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig) INITRANS 30;

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid) INITRANS 30;

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue) INITRANS 30;

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid) INITRANS 30;

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue) INITRANS 30;

CREATE INDEX iChangedItemPath ON m_audit_item (changedItemPath) INITRANS 30;

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id) INITRANS 30;

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id) INITRANS 30;

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_connector_host
ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus) INITRANS 30;

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus) INITRANS 30;

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_generic_object
ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key) INITRANS 30;

ALTER TABLE m_node
ADD CONSTRAINT uc_node_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iObjectNameOrig ON m_object (name_orig) INITRANS 30;

CREATE INDEX iObjectNameNorm ON m_object (name_norm) INITRANS 30;

CREATE INDEX iObjectTypeClass ON m_object (objectTypeClass) INITRANS 30;

CREATE INDEX iObjectCreateTimestamp ON m_object (createTimestamp) INITRANS 30;

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState) INITRANS 30;

CREATE INDEX iExtensionBoolean ON m_object_ext_boolean (ownerType, eName, booleanValue) INITRANS 30;

CREATE INDEX iExtensionBooleanDef ON m_object_ext_boolean (owner_oid, ownerType) INITRANS 30;

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

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid) INITRANS 30;

CREATE INDEX iShadowDead ON m_shadow (dead) INITRANS 30;

CREATE INDEX iShadowKind ON m_shadow (kind) INITRANS 30;

CREATE INDEX iShadowIntent ON m_shadow (intent) INITRANS 30;

CREATE INDEX iShadowObjectClass ON m_shadow (objectClass) INITRANS 30;

CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType) INITRANS 30;

CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation) INITRANS 30;

CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount) INITRANS 30;

ALTER TABLE m_system_configuration
ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm) INITRANS 30;

CREATE INDEX iParent ON m_task (parent) INITRANS 30;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId) INITRANS 30;

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp) INITRANS 30;

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp) INITRANS 30;

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid) INITRANS 30;

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

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
