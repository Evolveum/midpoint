ALTER TABLE m_case ADD (
  objectRef_relation  VARCHAR2(157 CHAR),
  objectRef_targetOid VARCHAR2(36 CHAR),
  objectRef_type      NUMBER(10, 0));

CREATE TABLE m_case_wi (
  id                     NUMBER(10, 0)     NOT NULL,
  owner_oid              VARCHAR2(36 CHAR) NOT NULL,
  closeTimestamp         TIMESTAMP,
  deadline               TIMESTAMP,
  outcome                VARCHAR2(255 CHAR),
  performerRef_relation  VARCHAR2(157 CHAR),
  performerRef_targetOid VARCHAR2(36 CHAR),
  performerRef_type      NUMBER(10, 0),
  stageNumber            NUMBER(10, 0),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

CREATE TABLE m_case_wi_reference (
  owner_id        NUMBER(10, 0)      NOT NULL,
  owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  relation        VARCHAR2(157 CHAR) NOT NULL,
  targetOid       VARCHAR2(36 CHAR)  NOT NULL,
  targetType      NUMBER(10, 0),
  PRIMARY KEY (owner_id, owner_owner_oid, relation, targetOid)
) INITRANS 30;

CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid) INITRANS 30;

ALTER TABLE m_case_wi
  ADD CONSTRAINT fk_case_wi_owner
FOREIGN KEY (owner_oid)
REFERENCES m_case;

ALTER TABLE m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_case_wi;

