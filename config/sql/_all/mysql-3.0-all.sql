# use for db create
# CREATE DATABASE <database name>
#   CHARACTER SET utf8
#   DEFAULT CHARACTER SET utf8
#   COLLATE utf8_bin
#   DEFAULT COLLATE utf8_bin
# ;

# replace "ENGINE=InnoDB" with "DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB"
# replace "DATETIME" with "DATETIME(6)"

# remove iAncestor and iDescendant index, they are the same as FK for that fields

CREATE TABLE m_abstract_role (
  approvalProcess VARCHAR(255),
  requestable     BIT,
  oid             VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment (
  id                      SMALLINT    NOT NULL,
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
  modifierRef_relation    VARCHAR(157),
  modifierRef_targetOid   VARCHAR(36),
  modifierRef_type        INTEGER,
  modifyChannel           VARCHAR(255),
  modifyTimestamp         DATETIME(6),
  orderValue              INTEGER,
  targetRef_relation      VARCHAR(157),
  targetRef_targetOid     VARCHAR(36),
  targetRef_type          INTEGER,
  tenantRef_relation      VARCHAR(157),
  tenantRef_targetOid     VARCHAR(36),
  tenantRef_type          INTEGER,
  extId                   SMALLINT,
  extOid                  VARCHAR(36),
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_date (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  dateValue                    DATETIME(6)  NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_long (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  longValue                    BIGINT       NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_poly (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  norm                         VARCHAR(255),
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_reference (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  targetoid                    VARCHAR(36)  NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  relation                     VARCHAR(157),
  targetType                   INTEGER,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_string (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_extension (
  owner_id        SMALLINT    NOT NULL,
  owner_owner_oid VARCHAR(36) NOT NULL,
  datesCount      SMALLINT,
  longsCount      SMALLINT,
  polysCount      SMALLINT,
  referencesCount SMALLINT,
  stringsCount    SMALLINT,
  PRIMARY KEY (owner_id, owner_owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_reference (
  reference_type  INTEGER      NOT NULL,
  owner_id        SMALLINT     NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  containerType   INTEGER,
  PRIMARY KEY (owner_id, owner_owner_oid, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_audit_delta (
  checksum   VARCHAR(32) NOT NULL,
  record_id  BIGINT      NOT NULL,
  delta      LONGTEXT,
  deltaOid   VARCHAR(36),
  deltaType  INTEGER,
  fullResult LONGTEXT,
  status     INTEGER,
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
  ENGINE =InnoDB;

CREATE TABLE m_connector_host (
  hostname  VARCHAR(255),
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  port      VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_exclusion (
  id                  SMALLINT    NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  policy              INTEGER,
  targetRef_relation  VARCHAR(157),
  targetRef_targetOid VARCHAR(36),
  targetRef_type      INTEGER,
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

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
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_node (
  name_norm      VARCHAR(255),
  name_orig      VARCHAR(255),
  nodeIdentifier VARCHAR(255),
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object (
  oid                   VARCHAR(36) NOT NULL,
  createChannel         VARCHAR(255),
  createTimestamp       DATETIME(6),
  creatorRef_relation   VARCHAR(157),
  creatorRef_targetOid  VARCHAR(36),
  creatorRef_type       INTEGER,
  datesCount            SMALLINT,
  fullObject            LONGBLOB,
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
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_date (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  dateValue  DATETIME(6)  NOT NULL,
  dynamicDef BIT,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_long (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  longValue  BIGINT       NOT NULL,
  dynamicDef BIT,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_poly (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  orig       VARCHAR(255) NOT NULL,
  dynamicDef BIT,
  norm       VARCHAR(255),
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_reference (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  targetoid  VARCHAR(36)  NOT NULL,
  dynamicDef BIT,
  relation   VARCHAR(157),
  targetType INTEGER,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_string (
  eName       VARCHAR(157) NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INTEGER      NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  dynamicDef  BIT,
  eType       VARCHAR(157),
  valueType   INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INTEGER,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayName_norm VARCHAR(255),
  displayName_orig VARCHAR(255),
  displayOrder     INTEGER,
  identifier       VARCHAR(255),
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  name_norm        VARCHAR(255),
  name_orig        VARCHAR(255),
  tenant           BIT,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_closure (
  descendant_oid VARCHAR(36) NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL ,
  val            INTEGER NOT NULL ,
  PRIMARY KEY (descendant_oid, ancestor_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_org_type (
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_reference (
  reference_type INTEGER      NOT NULL,
  owner_oid      VARCHAR(36)  NOT NULL,
  relation       VARCHAR(157) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  containerType  INTEGER,
  PRIMARY KEY (owner_oid, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_report (
  export              INTEGER,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  orientation         INTEGER,
  parent              BIT,
  useHibernateSession BIT,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

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
  ENGINE =InnoDB;

CREATE TABLE m_resource (
  administrativeState        INTEGER,
  connectorRef_relation      VARCHAR(157),
  connectorRef_targetOid     VARCHAR(36),
  connectorRef_type          INTEGER,
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  o16_lastAvailabilityStatus INTEGER,
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_security_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

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
  ENGINE =InnoDB;

CREATE TABLE m_system_configuration (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task (
  binding                INTEGER,
  canRunOnNode           VARCHAR(255),
  category               VARCHAR(255),
  completionTimestamp    DATETIME(6),
  executionStatus        INTEGER,
  handlerUri             VARCHAR(255),
  lastRunFinishTimestamp DATETIME(6),
  lastRunStartTimestamp  DATETIME(6),
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  node                   VARCHAR(255),
  objectRef_relation     VARCHAR(157),
  objectRef_targetOid    VARCHAR(36),
  objectRef_type         INTEGER,
  ownerRef_relation      VARCHAR(157),
  ownerRef_targetOid     VARCHAR(36),
  ownerRef_type          INTEGER,
  parent                 VARCHAR(255),
  recurrence             INTEGER,
  status                 INTEGER,
  taskIdentifier         VARCHAR(255),
  threadStopAction       INTEGER,
  waitingReason          INTEGER,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task_dependent (
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_trigger (
  id             SMALLINT    NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue DATETIME(6),
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user (
  additionalName_norm  VARCHAR(255),
  additionalName_orig  VARCHAR(255),
  costCenter           VARCHAR(255),
  emailAddress         VARCHAR(255),
  employeeNumber       VARCHAR(255),
  familyName_norm      VARCHAR(255),
  familyName_orig      VARCHAR(255),
  fullName_norm        VARCHAR(255),
  fullName_orig        VARCHAR(255),
  givenName_norm       VARCHAR(255),
  givenName_orig       VARCHAR(255),
  hasPhoto             BIT         NOT NULL,
  honorificPrefix_norm VARCHAR(255),
  honorificPrefix_orig VARCHAR(255),
  honorificSuffix_norm VARCHAR(255),
  honorificSuffix_orig VARCHAR(255),
  locale               VARCHAR(255),
  locality_norm        VARCHAR(255),
  locality_orig        VARCHAR(255),
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  nickName_norm        VARCHAR(255),
  nickName_orig        VARCHAR(255),
  preferredLanguage    VARCHAR(255),
  status               INTEGER,
  telephoneNumber      VARCHAR(255),
  timezone             VARCHAR(255),
  title_norm           VARCHAR(255),
  title_orig           VARCHAR(255),
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organization (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_photo (
  owner_oid VARCHAR(36) NOT NULL,
  photo     LONGBLOB,
  PRIMARY KEY (owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE INDEX iRequestable ON m_abstract_role (requestable);

ALTER TABLE m_abstract_role
ADD INDEX fk_abstract_role (oid),
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (oid)
REFERENCES m_focus (oid);

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

ALTER TABLE m_assignment
ADD INDEX fk_assignment_owner (owner_oid),
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue);

ALTER TABLE m_assignment_ext_date
ADD INDEX fk_assignment_ext_date (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue);

ALTER TABLE m_assignment_ext_long
ADD INDEX fk_assignment_ext_long (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig);

ALTER TABLE m_assignment_ext_poly
ADD INDEX fk_assignment_ext_poly (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_poly
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid);

ALTER TABLE m_assignment_ext_reference
ADD INDEX fk_assignment_ext_reference (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue);

ALTER TABLE m_assignment_ext_string
ADD INDEX fk_assignment_ext_string (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid);

ALTER TABLE m_assignment_reference
ADD INDEX fk_assignment_reference (owner_id, owner_owner_oid),
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment (id, owner_oid);

ALTER TABLE m_audit_delta
ADD INDEX fk_audit_delta (record_id),
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

ALTER TABLE m_connector
ADD INDEX fk_connector (oid),
ADD CONSTRAINT fk_connector
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_connector_host
ADD INDEX fk_connector_host (oid),
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_connector_target_system
ADD INDEX fk_connector_target_system (connector_oid),
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_oid)
REFERENCES m_connector (oid);

ALTER TABLE m_exclusion
ADD INDEX fk_exclusion_owner (owner_oid),
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_focus
ADD INDEX fk_focus (oid),
ADD CONSTRAINT fk_focus
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_generic_object
ADD INDEX fk_generic_object (oid),
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_node
ADD INDEX fk_node (oid),
ADD CONSTRAINT fk_node
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iObjectCreateTimestamp ON m_object (createTimestamp);

CREATE INDEX iObjectTypeClass ON m_object (objectTypeClass);

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);

CREATE INDEX iExtensionDate ON m_object_ext_date (ownerType, eName, dateValue);

CREATE INDEX iExtensionDateDef ON m_object_ext_date (owner_oid, ownerType);

ALTER TABLE m_object_ext_date
ADD INDEX fk_object_ext_date (owner_oid),
ADD CONSTRAINT fk_object_ext_date
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionLong ON m_object_ext_long (ownerType, eName, longValue);

CREATE INDEX iExtensionLongDef ON m_object_ext_long (owner_oid, ownerType);

ALTER TABLE m_object_ext_long
ADD INDEX fk_object_ext_long (owner_oid),
ADD CONSTRAINT fk_object_ext_long
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionPolyString ON m_object_ext_poly (ownerType, eName, orig);

CREATE INDEX iExtensionPolyStringDef ON m_object_ext_poly (owner_oid, ownerType);

ALTER TABLE m_object_ext_poly
ADD INDEX fk_object_ext_poly (owner_oid),
ADD CONSTRAINT fk_object_ext_poly
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionReference ON m_object_ext_reference (ownerType, eName, targetoid);

CREATE INDEX iExtensionReferenceDef ON m_object_ext_reference (owner_oid, ownerType);

ALTER TABLE m_object_ext_reference
ADD INDEX fk_object_ext_reference (owner_oid),
ADD CONSTRAINT fk_object_ext_reference
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionString ON m_object_ext_string (ownerType, eName, stringValue);

CREATE INDEX iExtensionStringDef ON m_object_ext_string (owner_oid, ownerType);

ALTER TABLE m_object_ext_string
ADD INDEX fk_object_ext_string (owner_oid),
ADD CONSTRAINT fk_object_ext_string
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

ALTER TABLE m_object_template
ADD INDEX fk_object_template (oid),
ADD CONSTRAINT fk_object_template
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iDisplayOrder ON m_org (displayOrder);

ALTER TABLE m_org
ADD INDEX fk_org (oid),
ADD CONSTRAINT fk_org
FOREIGN KEY (oid)
REFERENCES m_abstract_role (oid);

ALTER TABLE m_org_closure
ADD INDEX fk_ancestor (ancestor_oid),
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object (oid);

ALTER TABLE m_org_closure
ADD INDEX fk_descendant (descendant_oid),
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object (oid);

ALTER TABLE m_org_org_type
ADD INDEX fk_org_org_type (org_oid),
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_oid)
REFERENCES m_org (oid);

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid);

ALTER TABLE m_reference
ADD INDEX fk_reference_owner (owner_oid),
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iReportParent ON m_report (parent);

ALTER TABLE m_report
ADD INDEX fk_report (oid),
ADD CONSTRAINT fk_report
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_report_output
ADD INDEX fk_report_output (oid),
ADD CONSTRAINT fk_report_output
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_resource
ADD INDEX fk_resource (oid),
ADD CONSTRAINT fk_resource
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_role
ADD INDEX fk_role (oid),
ADD CONSTRAINT fk_role
FOREIGN KEY (oid)
REFERENCES m_abstract_role (oid);

ALTER TABLE m_security_policy
ADD INDEX fk_security_policy (oid),
ADD CONSTRAINT fk_security_policy
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowDead ON m_shadow (dead);

ALTER TABLE m_shadow
ADD INDEX fk_shadow (oid),
ADD CONSTRAINT fk_shadow
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_system_configuration
ADD INDEX fk_system_configuration (oid),
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_task
ADD INDEX fk_task (oid),
ADD CONSTRAINT fk_task
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_task_dependent
ADD INDEX fk_task_dependent (task_oid),
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_oid)
REFERENCES m_task (oid);

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_trigger
ADD INDEX fk_trigger_owner (owner_oid),
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iFullName ON m_user (fullName_orig);

CREATE INDEX iFamilyName ON m_user (familyName_orig);

CREATE INDEX iGivenName ON m_user (givenName_orig);

CREATE INDEX iLocality ON m_user (locality_orig);

ALTER TABLE m_user
ADD INDEX fk_user (oid),
ADD CONSTRAINT fk_user
FOREIGN KEY (oid)
REFERENCES m_focus (oid);

ALTER TABLE m_user_employee_type
ADD INDEX fk_user_employee_type (user_oid),
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_organization
ADD INDEX fk_user_organization (user_oid),
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_organizational_unit
ADD INDEX fk_user_org_unit (user_oid),
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_photo
ADD INDEX fk_user_photo (owner_oid),
ADD CONSTRAINT fk_user_photo
FOREIGN KEY (owner_oid)
REFERENCES m_user (oid);

ALTER TABLE m_value_policy
ADD INDEX fk_value_policy (oid),
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE TABLE hibernate_sequence (
  next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS(
  SCHED_NAME VARCHAR(120) NOT NULL,
  JOB_NAME VARCHAR(200) NOT NULL,
  JOB_GROUP VARCHAR(200) NOT NULL,
  DESCRIPTION VARCHAR(250) NULL,
  JOB_CLASS_NAME VARCHAR(250) NOT NULL,
  IS_DURABLE VARCHAR(1) NOT NULL,
  IS_NONCONCURRENT VARCHAR(1) NOT NULL,
  IS_UPDATE_DATA VARCHAR(1) NOT NULL,
  REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
  JOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  JOB_NAME VARCHAR(200) NOT NULL,
  JOB_GROUP VARCHAR(200) NOT NULL,
  DESCRIPTION VARCHAR(250) NULL,
  NEXT_FIRE_TIME BIGINT(13) NULL,
  PREV_FIRE_TIME BIGINT(13) NULL,
  PRIORITY INTEGER NULL,
  TRIGGER_STATE VARCHAR(16) NOT NULL,
  TRIGGER_TYPE VARCHAR(8) NOT NULL,
  START_TIME BIGINT(13) NOT NULL,
  END_TIME BIGINT(13) NULL,
  CALENDAR_NAME VARCHAR(200) NULL,
  MISFIRE_INSTR SMALLINT(2) NULL,
  JOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  INDEX (SCHED_NAME,JOB_NAME, JOB_GROUP),
  FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
  REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  REPEAT_COUNT BIGINT(7) NOT NULL,
  REPEAT_INTERVAL BIGINT(12) NOT NULL,
  TIMES_TRIGGERED BIGINT(10) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  CRON_EXPRESSION VARCHAR(120) NOT NULL,
  TIME_ZONE_ID VARCHAR(80),
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  STR_PROP_1 VARCHAR(512) NULL,
  STR_PROP_2 VARCHAR(512) NULL,
  STR_PROP_3 VARCHAR(512) NULL,
  INT_PROP_1 INT NULL,
  INT_PROP_2 INT NULL,
  LONG_PROP_1 BIGINT NULL,
  LONG_PROP_2 BIGINT NULL,
  DEC_PROP_1 NUMERIC(13,4) NULL,
  DEC_PROP_2 NUMERIC(13,4) NULL,
  BOOL_PROP_1 VARCHAR(1) NULL,
  BOOL_PROP_2 VARCHAR(1) NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  BLOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  CALENDAR_NAME VARCHAR(200) NOT NULL,
  CALENDAR BLOB NOT NULL,
  PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  ENTRY_ID VARCHAR(95) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  INSTANCE_NAME VARCHAR(200) NOT NULL,
  FIRED_TIME BIGINT(13) NOT NULL,
  PRIORITY INTEGER NOT NULL,
  STATE VARCHAR(16) NOT NULL,
  JOB_NAME VARCHAR(200) NULL,
  JOB_GROUP VARCHAR(200) NULL,
  IS_NONCONCURRENT VARCHAR(1) NULL,
  REQUESTS_RECOVERY VARCHAR(1) NULL,
  PRIMARY KEY (SCHED_NAME,ENTRY_ID))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
  SCHED_NAME VARCHAR(120) NOT NULL,
  INSTANCE_NAME VARCHAR(200) NOT NULL,
  LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
  CHECKIN_INTERVAL BIGINT(13) NOT NULL,
  PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
  ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  LOCK_NAME VARCHAR(40) NOT NULL,
  PRIMARY KEY (SCHED_NAME,LOCK_NAME))
  ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

create table ACT_GE_PROPERTY (
  NAME_ varchar(64),
  VALUE_ varchar(300),
  REV_ integer,
  primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

insert into ACT_GE_PROPERTY
values ('schema.version', '5.13', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.13)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_GE_BYTEARRAY (
  ID_ varchar(64),
  REV_ integer,
  NAME_ varchar(255),
  DEPLOYMENT_ID_ varchar(64),
  BYTES_ LONGBLOB,
  GENERATED_ TINYINT,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_DEPLOYMENT (
  ID_ varchar(64),
  NAME_ varchar(255),
  CATEGORY_ varchar(255),
  DEPLOY_TIME_ timestamp,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_MODEL (
  ID_ varchar(64) not null,
  REV_ integer,
  NAME_ varchar(255),
  KEY_ varchar(255),
  CATEGORY_ varchar(255),
  CREATE_TIME_ timestamp null,
  LAST_UPDATE_TIME_ timestamp null,
  VERSION_ integer,
  META_INFO_ varchar(4000),
  DEPLOYMENT_ID_ varchar(64),
  EDITOR_SOURCE_VALUE_ID_ varchar(64),
  EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EXECUTION (
  ID_ varchar(64),
  REV_ integer,
  PROC_INST_ID_ varchar(64),
  BUSINESS_KEY_ varchar(255),
  PARENT_ID_ varchar(64),
  PROC_DEF_ID_ varchar(64),
  SUPER_EXEC_ varchar(64),
  ACT_ID_ varchar(255),
  IS_ACTIVE_ TINYINT,
  IS_CONCURRENT_ TINYINT,
  IS_SCOPE_ TINYINT,
  IS_EVENT_SCOPE_ TINYINT,
  SUSPENSION_STATE_ integer,
  CACHED_ENT_STATE_ integer,
  primary key (ID_),
  unique ACT_UNIQ_RU_BUS_KEY (PROC_DEF_ID_, BUSINESS_KEY_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_JOB (
  ID_ varchar(64) NOT NULL,
  REV_ integer,
  TYPE_ varchar(255) NOT NULL,
  LOCK_EXP_TIME_ timestamp NULL,
  LOCK_OWNER_ varchar(255),
  EXCLUSIVE_ boolean,
  EXECUTION_ID_ varchar(64),
  PROCESS_INSTANCE_ID_ varchar(64),
  PROC_DEF_ID_ varchar(64),
  RETRIES_ integer,
  EXCEPTION_STACK_ID_ varchar(64),
  EXCEPTION_MSG_ varchar(4000),
  DUEDATE_ timestamp NULL,
  REPEAT_ varchar(255),
  HANDLER_TYPE_ varchar(255),
  HANDLER_CFG_ varchar(4000),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_PROCDEF (
  ID_ varchar(64) not null,
  REV_ integer,
  CATEGORY_ varchar(255),
  NAME_ varchar(255),
  KEY_ varchar(255) not null,
  VERSION_ integer not null,
  DEPLOYMENT_ID_ varchar(64),
  RESOURCE_NAME_ varchar(4000),
  DGRM_RESOURCE_NAME_ varchar(4000),
  DESCRIPTION_ varchar(4000),
  HAS_START_FORM_KEY_ TINYINT,
  SUSPENSION_STATE_ integer,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_TASK (
  ID_ varchar(64),
  REV_ integer,
  EXECUTION_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  PROC_DEF_ID_ varchar(64),
  NAME_ varchar(255),
  PARENT_TASK_ID_ varchar(64),
  DESCRIPTION_ varchar(4000),
  TASK_DEF_KEY_ varchar(255),
  OWNER_ varchar(255),
  ASSIGNEE_ varchar(255),
  DELEGATION_ varchar(64),
  PRIORITY_ integer,
  CREATE_TIME_ timestamp,
  DUE_DATE_ datetime,
  SUSPENSION_STATE_ integer,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_IDENTITYLINK (
  ID_ varchar(64),
  REV_ integer,
  GROUP_ID_ varchar(255),
  TYPE_ varchar(255),
  USER_ID_ varchar(255),
  TASK_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  PROC_DEF_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_VARIABLE (
  ID_ varchar(64) not null,
  REV_ integer,
  TYPE_ varchar(255) not null,
  NAME_ varchar(255) not null,
  EXECUTION_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  TASK_ID_ varchar(64),
  BYTEARRAY_ID_ varchar(64),
  DOUBLE_ double,
  LONG_ bigint,
  TEXT_ varchar(4000),
  TEXT2_ varchar(4000),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EVENT_SUBSCR (
  ID_ varchar(64) not null,
  REV_ integer,
  EVENT_TYPE_ varchar(255) not null,
  EVENT_NAME_ varchar(255),
  EXECUTION_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  ACTIVITY_ID_ varchar(64),
  CONFIGURATION_ varchar(255),
  CREATED_ timestamp not null,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);

alter table ACT_GE_BYTEARRAY
add constraint ACT_FK_BYTEARR_DEPL
foreign key (DEPLOYMENT_ID_)
references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
add constraint ACT_UNIQ_PROCDEF
unique (KEY_,VERSION_);

alter table ACT_RU_EXECUTION
add constraint ACT_FK_EXE_PROCINST
foreign key (PROC_INST_ID_)
references ACT_RU_EXECUTION (ID_) on delete cascade on update cascade;

alter table ACT_RU_EXECUTION
add constraint ACT_FK_EXE_PARENT
foreign key (PARENT_ID_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_EXECUTION
add constraint ACT_FK_EXE_SUPER
foreign key (SUPER_EXEC_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_EXECUTION
add constraint ACT_FK_EXE_PROCDEF
foreign key (PROC_DEF_ID_)
references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_IDENTITYLINK
add constraint ACT_FK_TSKASS_TASK
foreign key (TASK_ID_)
references ACT_RU_TASK (ID_);

alter table ACT_RU_IDENTITYLINK
add constraint ACT_FK_ATHRZ_PROCEDEF
foreign key (PROC_DEF_ID_)
references ACT_RE_PROCDEF(ID_);

alter table ACT_RU_IDENTITYLINK
add constraint ACT_FK_IDL_PROCINST
foreign key (PROC_INST_ID_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
add constraint ACT_FK_TASK_EXE
foreign key (EXECUTION_ID_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
add constraint ACT_FK_TASK_PROCINST
foreign key (PROC_INST_ID_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
add constraint ACT_FK_TASK_PROCDEF
foreign key (PROC_DEF_ID_)
references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_VARIABLE
add constraint ACT_FK_VAR_EXE
foreign key (EXECUTION_ID_)
references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_VARIABLE
add constraint ACT_FK_VAR_PROCINST
foreign key (PROC_INST_ID_)
references ACT_RU_EXECUTION(ID_);

alter table ACT_RU_VARIABLE
add constraint ACT_FK_VAR_BYTEARRAY
foreign key (BYTEARRAY_ID_)
references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_JOB
add constraint ACT_FK_JOB_EXCEPTION
foreign key (EXCEPTION_STACK_ID_)
references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EVENT_SUBSCR
add constraint ACT_FK_EVENT_EXEC
foreign key (EXECUTION_ID_)
references ACT_RU_EXECUTION(ID_);

alter table ACT_RE_MODEL
add constraint ACT_FK_MODEL_SOURCE
foreign key (EDITOR_SOURCE_VALUE_ID_)
references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL
add constraint ACT_FK_MODEL_SOURCE_EXTRA
foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_)
references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL
add constraint ACT_FK_MODEL_DEPLOYMENT
foreign key (DEPLOYMENT_ID_)
references ACT_RE_DEPLOYMENT (ID_);

create table ACT_HI_PROCINST (
  ID_ varchar(64) not null,
  PROC_INST_ID_ varchar(64) not null,
  BUSINESS_KEY_ varchar(255),
  PROC_DEF_ID_ varchar(64) not null,
  START_TIME_ datetime not null,
  END_TIME_ datetime,
  DURATION_ bigint,
  START_USER_ID_ varchar(255),
  START_ACT_ID_ varchar(255),
  END_ACT_ID_ varchar(255),
  SUPER_PROCESS_INSTANCE_ID_ varchar(64),
  DELETE_REASON_ varchar(4000),
  primary key (ID_),
  unique (PROC_INST_ID_),
  unique ACT_UNIQ_HI_BUS_KEY (PROC_DEF_ID_, BUSINESS_KEY_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ACTINST (
  ID_ varchar(64) not null,
  PROC_DEF_ID_ varchar(64) not null,
  PROC_INST_ID_ varchar(64) not null,
  EXECUTION_ID_ varchar(64) not null,
  ACT_ID_ varchar(255) not null,
  TASK_ID_ varchar(64),
  CALL_PROC_INST_ID_ varchar(64),
  ACT_NAME_ varchar(255),
  ACT_TYPE_ varchar(255) not null,
  ASSIGNEE_ varchar(64),
  START_TIME_ datetime not null,
  END_TIME_ datetime,
  DURATION_ bigint,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_TASKINST (
  ID_ varchar(64) not null,
  PROC_DEF_ID_ varchar(64),
  TASK_DEF_KEY_ varchar(255),
  PROC_INST_ID_ varchar(64),
  EXECUTION_ID_ varchar(64),
  NAME_ varchar(255),
  PARENT_TASK_ID_ varchar(64),
  DESCRIPTION_ varchar(4000),
  OWNER_ varchar(255),
  ASSIGNEE_ varchar(255),
  START_TIME_ datetime not null,
  CLAIM_TIME_ datetime,
  END_TIME_ datetime,
  DURATION_ bigint,
  DELETE_REASON_ varchar(4000),
  PRIORITY_ integer,
  DUE_DATE_ datetime,
  FORM_KEY_ varchar(255),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_VARINST (
  ID_ varchar(64) not null,
  PROC_INST_ID_ varchar(64),
  EXECUTION_ID_ varchar(64),
  TASK_ID_ varchar(64),
  NAME_ varchar(255) not null,
  VAR_TYPE_ varchar(100),
  REV_ integer,
  BYTEARRAY_ID_ varchar(64),
  DOUBLE_ double,
  LONG_ bigint,
  TEXT_ varchar(4000),
  TEXT2_ varchar(4000),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_DETAIL (
  ID_ varchar(64) not null,
  TYPE_ varchar(255) not null,
  PROC_INST_ID_ varchar(64),
  EXECUTION_ID_ varchar(64),
  TASK_ID_ varchar(64),
  ACT_INST_ID_ varchar(64),
  NAME_ varchar(255) not null,
  VAR_TYPE_ varchar(255),
  REV_ integer,
  TIME_ datetime not null,
  BYTEARRAY_ID_ varchar(64),
  DOUBLE_ double,
  LONG_ bigint,
  TEXT_ varchar(4000),
  TEXT2_ varchar(4000),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_COMMENT (
  ID_ varchar(64) not null,
  TYPE_ varchar(255),
  TIME_ datetime not null,
  USER_ID_ varchar(255),
  TASK_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  ACTION_ varchar(255),
  MESSAGE_ varchar(4000),
  FULL_MSG_ LONGBLOB,
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ATTACHMENT (
  ID_ varchar(64) not null,
  REV_ integer,
  USER_ID_ varchar(255),
  NAME_ varchar(255),
  DESCRIPTION_ varchar(4000),
  TYPE_ varchar(255),
  TASK_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  URL_ varchar(4000),
  CONTENT_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_IDENTITYLINK (
  ID_ varchar(64),
  GROUP_ID_ varchar(255),
  TYPE_ varchar(255),
  USER_ID_ varchar(255),
  TASK_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);
create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);
create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);
create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);
create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);
create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);
create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);
create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);

create table ACT_ID_GROUP (
  ID_ varchar(64),
  REV_ integer,
  NAME_ varchar(255),
  TYPE_ varchar(255),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_MEMBERSHIP (
  USER_ID_ varchar(64),
  GROUP_ID_ varchar(64),
  primary key (USER_ID_, GROUP_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_USER (
  ID_ varchar(64),
  REV_ integer,
  FIRST_ varchar(255),
  LAST_ varchar(255),
  EMAIL_ varchar(255),
  PWD_ varchar(255),
  PICTURE_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_INFO (
  ID_ varchar(64),
  REV_ integer,
  USER_ID_ varchar(64),
  TYPE_ varchar(64),
  KEY_ varchar(255),
  VALUE_ varchar(255),
  PASSWORD_ LONGBLOB,
  PARENT_ID_ varchar(255),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

alter table ACT_ID_MEMBERSHIP
add constraint ACT_FK_MEMB_GROUP
foreign key (GROUP_ID_)
references ACT_ID_GROUP (ID_);

alter table ACT_ID_MEMBERSHIP
add constraint ACT_FK_MEMB_USER
foreign key (USER_ID_)
references ACT_ID_USER (ID_);

commit;
