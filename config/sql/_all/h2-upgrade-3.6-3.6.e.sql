ALTER TABLE m_case ADD objectRef_relation VARCHAR(157);
ALTER TABLE m_case ADD objectRef_targetOid VARCHAR(36);
ALTER TABLE m_case ADD objectRef_type INTEGER;

CREATE TABLE m_case_wi (
  id                     INTEGER     NOT NULL,
  owner_oid              VARCHAR(36) NOT NULL,
  closeTimestamp         TIMESTAMP,
  deadline               TIMESTAMP,
  outcome                VARCHAR(255),
  performerRef_relation  VARCHAR(157),
  performerRef_targetOid VARCHAR(36),
  performerRef_type      INTEGER,
  stageNumber            INTEGER,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_case_wi_reference (
  owner_id        INTEGER      NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_id, owner_owner_oid, relation, targetOid)
);

CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);

ALTER TABLE m_case_wi
  ADD CONSTRAINT fk_case_wi_owner
FOREIGN KEY (owner_oid)
REFERENCES m_case;

ALTER TABLE m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_case_wi;
