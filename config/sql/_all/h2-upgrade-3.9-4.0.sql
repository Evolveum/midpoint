CREATE TABLE m_archetype (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_dashboard (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

CREATE INDEX iDashboardNameOrig
  ON m_dashboard (name_orig);
ALTER TABLE m_dashboard
  ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);

ALTER TABLE m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_shadow ADD COLUMN primaryIdentifierValue VARCHAR(255);

ALTER TABLE m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

ALTER TABLE m_audit_event ADD COLUMN requestIdentifier VARCHAR(255);

ALTER TABLE m_case ADD COLUMN parentRef_relation  VARCHAR(157);
ALTER TABLE m_case ADD COLUMN parentRef_targetOid VARCHAR(36);
ALTER TABLE m_case ADD COLUMN parentRef_type      INTEGER;
ALTER TABLE m_case ADD COLUMN targetRef_relation  VARCHAR(157);
ALTER TABLE m_case ADD COLUMN targetRef_targetOid VARCHAR(36);
ALTER TABLE m_case ADD COLUMN targetRef_type      INTEGER;

CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid);
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid);
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid);

-- 2019-06-07 13:00

DROP INDEX iTaskWfProcessInstanceId;
DROP INDEX iTaskWfStartTimestamp;
DROP INDEX iTaskWfEndTimestamp;
DROP INDEX iTaskWfRequesterOid;
DROP INDEX iTaskWfObjectOid;
DROP INDEX iTaskWfTargetOid;
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

ALTER TABLE m_case ADD COLUMN closeTimestamp         TIMESTAMP;
ALTER TABLE m_case ADD COLUMN requestorRef_relation  VARCHAR(157);
ALTER TABLE m_case ADD COLUMN requestorRef_targetOid VARCHAR(36);
ALTER TABLE m_case ADD COLUMN requestorRef_type      INTEGER;

CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';

-- 2019-06-25 09:00

CREATE TABLE m_audit_resource (
  resourceOid 	  VARCHAR(255) NOT NULL,
  record_id       BIGINT       NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);
CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid);
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id);
ALTER TABLE m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;

-- 2019-07-30 11:30

ALTER TABLE m_audit_item ALTER COLUMN changedItemPath VARCHAR(900);

-- 2019-08-30 12:32

ALTER TABLE m_case_wi_reference ADD COLUMN reference_type  INTEGER;
UPDATE m_case_wi_reference SET reference_type = 0 WHERE reference_type IS NULL;
ALTER TABLE m_case_wi_reference ALTER COLUMN reference_type  INTEGER NOT NULL;

ALTER TABLE m_case_wi_reference DROP CONSTRAINT fk_case_wi_reference_owner;
ALTER TABLE m_case_wi_reference DROP PRIMARY KEY;
ALTER TABLE m_case_wi_reference ADD PRIMARY KEY (owner_owner_oid, owner_id, reference_type, targetOid, relation);
ALTER TABLE m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi (owner_oid, id);

ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS booleansCount;
ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS datesCount;
ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS longsCount;
ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS polysCount;
ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS referencesCount;
ALTER TABLE m_assignment_extension DROP COLUMN IF EXISTS stringsCount;

ALTER TABLE m_object DROP COLUMN IF EXISTS booleansCount;
ALTER TABLE m_object DROP COLUMN IF EXISTS datesCount;
ALTER TABLE m_object DROP COLUMN IF EXISTS longsCount;
ALTER TABLE m_object DROP COLUMN IF EXISTS polysCount;
ALTER TABLE m_object DROP COLUMN IF EXISTS referencesCount;
ALTER TABLE m_object DROP COLUMN IF EXISTS stringsCount;

COMMIT;
