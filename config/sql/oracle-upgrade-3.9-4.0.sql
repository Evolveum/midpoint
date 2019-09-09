CREATE TABLE m_archetype (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;
CREATE TABLE m_dashboard (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig) INITRANS 30;
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm) INITRANS 30;

CREATE INDEX iDashboardNameOrig
  ON m_dashboard (name_orig) INITRANS 30;
ALTER TABLE m_dashboard
  ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);
  
ALTER TABLE m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_shadow ADD primaryIdentifierValue VARCHAR2(255 CHAR);

CREATE UNIQUE INDEX iPrimaryIdentifierValueWithOC
    ON m_shadow (
        CASE WHEN primaryIdentifierValue IS NOT NULL AND objectClass IS NOT NULL AND resourceRef_targetOid IS NOT NULL THEN primaryIdentifierValue END,
        CASE WHEN primaryIdentifierValue IS NOT NULL AND objectClass IS NOT NULL AND resourceRef_targetOid IS NOT NULL THEN objectClass END,
        CASE WHEN primaryIdentifierValue IS NOT NULL AND objectClass IS NOT NULL AND resourceRef_targetOid IS NOT NULL THEN resourceRef_targetOid END);

ALTER TABLE m_audit_event ADD requestIdentifier VARCHAR2(255 CHAR);

ALTER TABLE m_case ADD (
  parentRef_relation  VARCHAR2(157 CHAR),
  parentRef_targetOid VARCHAR2(36 CHAR),
  parentRef_type      NUMBER(10, 0),
  targetRef_relation  VARCHAR2(157 CHAR),
  targetRef_targetOid VARCHAR2(36 CHAR),
  targetRef_type      NUMBER(10, 0));

CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid) INITRANS 30;
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid) INITRANS 30;
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid) INITRANS 30;

DROP INDEX iTaskWfProcessInstanceId;
DROP INDEX iTaskWfStartTimestamp;
DROP INDEX iTaskWfEndTimestamp;
DROP INDEX iTaskWfRequesterOid;
DROP INDEX iTaskWfObjectOid;
DROP INDEX iTaskWfTargetOid;
CREATE INDEX iTaskObjectOid ON m_task(objectRef_targetOid) INITRANS 30;

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

ALTER TABLE m_case ADD (
  closeTimestamp         TIMESTAMP,
  requestorRef_relation  VARCHAR2(157 CHAR),
  requestorRef_targetOid VARCHAR2(36 CHAR),
  requestorRef_type      NUMBER(10, 0)
  );

CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid) INITRANS 30;
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp) INITRANS 30;

declare
  already_exists  exception;
  columns_indexed exception;
  pragma exception_init( already_exists, -955 );
  pragma exception_init(columns_indexed, -1408);
begin
  execute immediate 'CREATE INDEX iUserEmployeeTypeOid ON M_USER_EMPLOYEE_TYPE(USER_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iUserOrganizationOid ON M_USER_ORGANIZATION(USER_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iUserOrganizationalUnitOid ON M_USER_ORGANIZATIONAL_UNIT(USER_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtBooleanItemId ON M_ASSIGNMENT_EXT_BOOLEAN(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtDateItemId ON M_ASSIGNMENT_EXT_DATE(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtLongItemId ON M_ASSIGNMENT_EXT_LONG(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtPolyItemId ON M_ASSIGNMENT_EXT_POLY(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtReferenceItemId ON M_ASSIGNMENT_EXT_REFERENCE(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentExtStringItemId ON M_ASSIGNMENT_EXT_STRING(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iAssignmentPolicySituationId ON M_ASSIGNMENT_POLICY_SITUATION(ASSIGNMENT_OID, ASSIGNMENT_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iConnectorTargetSystemOid ON M_CONNECTOR_TARGET_SYSTEM(CONNECTOR_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iFocusPolicySituationOid ON M_FOCUS_POLICY_SITUATION(FOCUS_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtBooleanItemId ON M_OBJECT_EXT_BOOLEAN(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtDateItemId ON M_OBJECT_EXT_DATE(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtLongItemId ON M_OBJECT_EXT_LONG(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtPolyItemId ON M_OBJECT_EXT_POLY(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtReferenceItemId ON M_OBJECT_EXT_REFERENCE(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectExtStringItemId ON M_OBJECT_EXT_STRING(ITEM_ID) INITRANS 30';
  execute immediate 'CREATE INDEX iObjectSubtypeOid ON M_OBJECT_SUBTYPE(OBJECT_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iOrgOrgTypeOid ON M_ORG_ORG_TYPE(ORG_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iServiceTypeOid ON M_SERVICE_TYPE(SERVICE_OID) INITRANS 30';
  execute immediate 'CREATE INDEX iTaskDependentOid ON M_TASK_DEPENDENT(TASK_OID) INITRANS 30';
exception
  when already_exists or columns_indexed then
    dbms_output.put_line('Index creation skipped; they probably already exist');
end;

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';

-- 2019-06-25 09:00

CREATE TABLE m_audit_resource (
  resourceOid     VARCHAR2(255 CHAR) NOT NULL,
  record_id       NUMBER(19, 0)      NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
) INITRANS 30;
CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid) INITRANS 30;
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id) INITRANS 30;
ALTER TABLE m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;

-- 2019-07-30 11:30

ALTER TABLE m_audit_item MODIFY changedItemPath VARCHAR2(900 CHAR);

-- 2019-08-30 12:32

ALTER TABLE m_case_wi_reference ADD reference_type NUMBER(10, 0) DEFAULT 0 NOT NULL;

ALTER TABLE m_case_wi_reference DROP PRIMARY KEY;
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

-- 2019-09-06 20:00

ALTER TABLE m_case_wi ADD createTimestamp TIMESTAMP;

COMMIT;