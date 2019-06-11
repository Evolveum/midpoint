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
