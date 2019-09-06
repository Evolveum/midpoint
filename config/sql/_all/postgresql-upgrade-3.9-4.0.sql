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

ALTER TABLE IF EXISTS m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE IF EXISTS m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_shadow ADD COLUMN primaryIdentifierValue VARCHAR(255);

ALTER TABLE IF EXISTS m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

ALTER TABLE m_audit_event ADD COLUMN requestIdentifier VARCHAR(255);

ALTER TABLE m_case ADD COLUMN parentRef_relation VARCHAR(157),
                   ADD COLUMN parentRef_targetOid VARCHAR(36),
                   ADD COLUMN parentRef_type INT4,
                   ADD COLUMN targetRef_relation VARCHAR(157),
                   ADD COLUMN targetRef_targetOid VARCHAR(36),
                   ADD COLUMN targetRef_type INT4;

-- This is no longer valid
-- ALTER TABLE IF EXISTS m_case
--  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);
--
-- So use the following if you have it defined:
-- ALTER TABLE IF EXISTS m_case
--     DROP CONSTRAINT uc_case_name;

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

ALTER TABLE m_case
  ADD COLUMN closeTimestamp         TIMESTAMP,
  ADD COLUMN requestorRef_relation  VARCHAR(157),
  ADD COLUMN requestorRef_targetOid VARCHAR(36),
  ADD COLUMN requestorRef_type      INT4;

CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);

-- Indices for foreign keys; maintained manually
CREATE INDEX iUserEmployeeTypeOid ON M_USER_EMPLOYEE_TYPE(USER_OID);
CREATE INDEX iUserOrganizationOid ON M_USER_ORGANIZATION(USER_OID);
CREATE INDEX iUserOrganizationalUnitOid ON M_USER_ORGANIZATIONAL_UNIT(USER_OID);
CREATE INDEX iAssignmentExtBooleanItemId ON M_ASSIGNMENT_EXT_BOOLEAN(ITEM_ID);
CREATE INDEX iAssignmentExtDateItemId ON M_ASSIGNMENT_EXT_DATE(ITEM_ID);
CREATE INDEX iAssignmentExtLongItemId ON M_ASSIGNMENT_EXT_LONG(ITEM_ID);
CREATE INDEX iAssignmentExtPolyItemId ON M_ASSIGNMENT_EXT_POLY(ITEM_ID);
CREATE INDEX iAssignmentExtReferenceItemId ON M_ASSIGNMENT_EXT_REFERENCE(ITEM_ID);
CREATE INDEX iAssignmentExtStringItemId ON M_ASSIGNMENT_EXT_STRING(ITEM_ID);
CREATE INDEX iAssignmentPolicySituationId ON M_ASSIGNMENT_POLICY_SITUATION(ASSIGNMENT_OID, ASSIGNMENT_ID);
CREATE INDEX iConnectorTargetSystemOid ON M_CONNECTOR_TARGET_SYSTEM(CONNECTOR_OID);
CREATE INDEX iFocusPolicySituationOid ON M_FOCUS_POLICY_SITUATION(FOCUS_OID);
CREATE INDEX iObjectExtBooleanItemId ON M_OBJECT_EXT_BOOLEAN(ITEM_ID);
CREATE INDEX iObjectExtDateItemId ON M_OBJECT_EXT_DATE(ITEM_ID);
CREATE INDEX iObjectExtLongItemId ON M_OBJECT_EXT_LONG(ITEM_ID);
CREATE INDEX iObjectExtPolyItemId ON M_OBJECT_EXT_POLY(ITEM_ID);
CREATE INDEX iObjectExtReferenceItemId ON M_OBJECT_EXT_REFERENCE(ITEM_ID);
CREATE INDEX iObjectExtStringItemId ON M_OBJECT_EXT_STRING(ITEM_ID);
CREATE INDEX iObjectSubtypeOid ON M_OBJECT_SUBTYPE(OBJECT_OID);
CREATE INDEX iOrgOrgTypeOid ON M_ORG_ORG_TYPE(ORG_OID);
CREATE INDEX iServiceTypeOid ON M_SERVICE_TYPE(SERVICE_OID);
CREATE INDEX iTaskDependentOid ON M_TASK_DEPENDENT(TASK_OID);

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';

-- 2019-06-25 09:00

CREATE TABLE m_audit_resource (
  resourceOid 	  VARCHAR(255) NOT NULL,
  record_id       INT8         NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);
CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid);
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id);
ALTER TABLE IF EXISTS m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;

-- 2019-07-30 11:30

ALTER TABLE m_audit_item ALTER COLUMN changedItemPath TYPE VARCHAR (900);

-- 2019-08-30 12:32

ALTER TABLE m_case_wi_reference ADD reference_type INT4 DEFAULT 0 NOT NULL;

ALTER TABLE m_case_wi_reference DROP CONSTRAINT m_case_wi_reference_pkey;
ALTER TABLE m_case_wi_reference ADD PRIMARY KEY(owner_owner_oid, owner_id, reference_type, targetOid, relation);

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

ALTER TABLE m_case DROP CONSTRAINT uc_case_name;

COMMIT;
