ALTER TABLE m_case ADD objectRef_relation  NVARCHAR(157) COLLATE database_default;
ALTER TABLE m_case ADD objectRef_targetOid NVARCHAR(36) COLLATE database_default;
ALTER TABLE m_case ADD objectRef_type      INT;

CREATE TABLE m_case_wi (
  id                     INT                                   NOT NULL,
  owner_oid              NVARCHAR(36) COLLATE database_default NOT NULL,
  closeTimestamp         DATETIME2,
  deadline               DATETIME2,
  outcome                NVARCHAR(255) COLLATE database_default,
  performerRef_relation  NVARCHAR(157) COLLATE database_default,
  performerRef_targetOid NVARCHAR(36) COLLATE database_default,
  performerRef_type      INT,
  stageNumber            INT,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_case_wi_reference (
  owner_id        INT                                    NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  relation        NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid       NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetType      INT,
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
