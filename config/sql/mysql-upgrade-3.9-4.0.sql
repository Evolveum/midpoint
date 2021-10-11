CREATE TABLE m_archetype (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;
CREATE TABLE m_dashboard (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

CREATE INDEX iDashboardNameOrig
  ON m_dashboard (name_orig);
ALTER TABLE m_dashboard
  ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);

ALTER TABLE m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object (oid);

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role(oid);

ALTER TABLE m_generic_object DROP FOREIGN KEY fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus(oid);

ALTER TABLE m_shadow ADD COLUMN primaryIdentifierValue VARCHAR(255);

ALTER TABLE m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

ALTER TABLE m_audit_event ADD COLUMN requestIdentifier VARCHAR(255);

ALTER TABLE m_case ADD COLUMN
  (
  parentRef_relation  VARCHAR(157),
  parentRef_targetOid VARCHAR(36),
  parentRef_type      INTEGER,
  targetRef_relation  VARCHAR(157),
  targetRef_targetOid VARCHAR(36),
  targetRef_type      INTEGER
  );

CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid);
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid);
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid);

-- 2019-06-07 13:00

DROP INDEX iTaskWfProcessInstanceId ON m_task;
DROP INDEX iTaskWfStartTimestamp ON m_task;
DROP INDEX iTaskWfEndTimestamp ON m_task;
DROP INDEX iTaskWfRequesterOid ON m_task;
DROP INDEX iTaskWfObjectOid ON m_task;
DROP INDEX iTaskWfTargetOid ON m_task;
CREATE INDEX iTaskObjectOid ON m_task(objectRef_targetOid);

ALTER TABLE m_task DROP COLUMN canRunOnNode;
ALTER TABLE m_task DROP COLUMN wfEndTimestamp;
ALTER TABLE m_task DROP COLUMN wfObjectRef_relation;
ALTER TABLE m_task DROP COLUMN wfObjectRef_targetOid;
ALTER TABLE m_task DROP COLUMN wfObjectRef_type;
ALTER TABLE m_task DROP COLUMN wfProcessInstanceId;
ALTER TABLE m_task DROP COLUMN wfRequesterRef_relation;
ALTER TABLE m_task DROP COLUMN wfRequesterRef_targetOid;
ALTER TABLE m_task DROP COLUMN wfRequesterRef_type;
ALTER TABLE m_task DROP COLUMN wfStartTimestamp;
ALTER TABLE m_task DROP COLUMN wfTargetRef_relation;
ALTER TABLE m_task DROP COLUMN wfTargetRef_targetOid;
ALTER TABLE m_task DROP COLUMN wfTargetRef_type;

ALTER TABLE m_case ADD COLUMN (
  closeTimestamp         DATETIME(6),
  requestorRef_relation  VARCHAR(157),
  requestorRef_targetOid VARCHAR(36),
  requestorRef_type      INTEGER
  );

CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';

-- 2019-06-25 09:00

CREATE TABLE m_audit_resource (
  resourceOid       VARCHAR(255) NOT NULL,
  record_id       BIGINT       NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
) DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid);
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id);
ALTER TABLE m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event (id);

-- 2019-08-30 12:32

ALTER TABLE m_case_wi_reference ADD COLUMN reference_type  INTEGER NOT NULL DEFAULT 0;

ALTER TABLE m_case_wi_reference DROP PRIMARY KEY, ADD PRIMARY KEY(owner_owner_oid, owner_id, reference_type, targetOid, relation);

ALTER TABLE m_assignment_extension DROP COLUMN booleansCount;
ALTER TABLE m_assignment_extension DROP COLUMN datesCount;
ALTER TABLE m_assignment_extension DROP COLUMN longsCount;
ALTER TABLE m_assignment_extension DROP COLUMN polysCount;
ALTER TABLE m_assignment_extension DROP COLUMN referencesCount;
ALTER TABLE m_assignment_extension DROP COLUMN stringsCount;

ALTER TABLE m_object DROP COLUMN booleansCount;
ALTER TABLE m_object DROP COLUMN datesCount;
ALTER TABLE m_object DROP COLUMN longsCount;
ALTER TABLE m_object DROP COLUMN polysCount;
ALTER TABLE m_object DROP COLUMN referencesCount;
ALTER TABLE m_object DROP COLUMN stringsCount;

DROP TABLE ACT_EVT_LOG;
DROP TABLE ACT_GE_PROPERTY;
DROP TABLE ACT_HI_ACTINST;
DROP TABLE ACT_HI_ATTACHMENT;
DROP TABLE ACT_HI_COMMENT;
DROP TABLE ACT_HI_DETAIL;
DROP TABLE ACT_HI_IDENTITYLINK;
DROP TABLE ACT_HI_PROCINST;
DROP TABLE ACT_HI_TASKINST;
DROP TABLE ACT_HI_VARINST;
DROP TABLE ACT_ID_INFO;
DROP TABLE ACT_ID_MEMBERSHIP;
DROP TABLE ACT_ID_GROUP;
DROP TABLE ACT_ID_USER;
DROP TABLE ACT_PROCDEF_INFO;
DROP TABLE ACT_RE_MODEL;
DROP TABLE ACT_RU_EVENT_SUBSCR;
DROP TABLE ACT_RU_IDENTITYLINK;
DROP TABLE ACT_RU_JOB;
DROP TABLE ACT_RU_TASK;
DROP TABLE ACT_RU_VARIABLE;
DROP TABLE ACT_GE_BYTEARRAY;
DROP TABLE ACT_RE_DEPLOYMENT;
DROP TABLE ACT_RU_EXECUTION;
DROP TABLE ACT_RE_PROCDEF;

-- 2019-09-04 10:25

ALTER TABLE m_case DROP INDEX uc_case_name;

-- 2019-09-06 20:00

ALTER TABLE m_case_wi ADD COLUMN createTimestamp DATETIME(6);

COMMIT;
