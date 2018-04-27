CREATE TABLE m_audit_event_NEW (
  id                BIGINT IDENTITY NOT NULL,
  attorneyName      NVARCHAR(255) COLLATE database_default,
  attorneyOid       NVARCHAR(36) COLLATE database_default,
  channel           NVARCHAR(255) COLLATE database_default,
  eventIdentifier   NVARCHAR(255) COLLATE database_default,
  eventStage        INT,
  eventType         INT,
  hostIdentifier    NVARCHAR(255) COLLATE database_default,
  initiatorName     NVARCHAR(255) COLLATE database_default,
  initiatorOid      NVARCHAR(36) COLLATE database_default,
  initiatorType     INT,
  message           NVARCHAR(1024) COLLATE database_default,
  nodeIdentifier    NVARCHAR(255) COLLATE database_default,
  outcome           INT,
  parameter         NVARCHAR(255) COLLATE database_default,
  remoteHostAddress NVARCHAR(255) COLLATE database_default,
  result            NVARCHAR(255) COLLATE database_default,
  sessionIdentifier NVARCHAR(255) COLLATE database_default,
  targetName        NVARCHAR(255) COLLATE database_default,
  targetOid         NVARCHAR(36) COLLATE database_default,
  targetOwnerName   NVARCHAR(255) COLLATE database_default,
  targetOwnerOid    NVARCHAR(36) COLLATE database_default,
  targetType        INT,
  taskIdentifier    NVARCHAR(255) COLLATE database_default,
  taskOID           NVARCHAR(255) COLLATE database_default,
  timestampValue    DATETIME2,
  -- no primary key for now
);

CREATE TABLE m_audit_prop_value_NEW (
  id        BIGINT IDENTITY NOT NULL,
  name      NVARCHAR(255) COLLATE database_default,
  record_id BIGINT,
  value     NVARCHAR(1024) COLLATE database_default,
  -- no primary key for now
);

CREATE TABLE m_audit_ref_value_NEW (
  id              BIGINT IDENTITY NOT NULL,
  name            NVARCHAR(255) COLLATE database_default,
  oid             NVARCHAR(255) COLLATE database_default,
  record_id       BIGINT,
  targetName_norm NVARCHAR(255) COLLATE database_default,
  targetName_orig NVARCHAR(255) COLLATE database_default,
  type            NVARCHAR(255) COLLATE database_default,
  -- no primary key for now
);

-- copying tables (can take a long time)
SET IDENTITY_INSERT m_audit_event_NEW ON;
INSERT INTO m_audit_event_NEW (id, attorneyName, attorneyOid, channel, eventIdentifier, eventStage, eventType, hostIdentifier,
                               initiatorName, initiatorOid, initiatorType, message, nodeIdentifier, outcome, parameter, remoteHostAddress,
                               result, sessionIdentifier, targetName, targetOid, targetOwnerName, targetOwnerOid, targetType,
                               taskIdentifier, taskOID, timestampValue)
  SELECT
    id, attorneyName, attorneyOid, channel, eventIdentifier, eventStage, eventType, hostIdentifier, initiatorName, initiatorOid,
    initiatorType, message, nodeIdentifier, outcome, parameter, remoteHostAddress, result, sessionIdentifier, targetName, targetOid,
    targetOwnerName, targetOwnerOid, targetType, taskIdentifier, taskOID, timestampValue
  FROM m_audit_event;
SET IDENTITY_INSERT m_audit_event_NEW OFF;

SET IDENTITY_INSERT m_audit_prop_value_NEW ON;
INSERT INTO m_audit_prop_value_NEW (id, name, record_id, [value])
  SELECT id, [name], record_id, [value] FROM m_audit_prop_value;
SET IDENTITY_INSERT m_audit_prop_value_NEW OFF;

SET IDENTITY_INSERT m_audit_ref_value_NEW ON;
INSERT INTO m_audit_ref_value_NEW (id, name, oid, record_id, targetName_norm, targetName_orig, type)
  SELECT id, [name], oid, record_id, targetName_norm, targetName_orig, type FROM m_audit_ref_value;
SET IDENTITY_INSERT m_audit_ref_value_NEW OFF;

ALTER TABLE m_audit_delta DROP CONSTRAINT fk_audit_delta;
ALTER TABLE m_audit_item DROP CONSTRAINT fk_audit_item;

-- renaming new tables to old ones
DROP TABLE m_audit_prop_value;
EXEC sp_rename 'm_audit_prop_value_NEW', 'm_audit_prop_value'
DROP TABLE m_audit_ref_value;
EXEC sp_rename 'm_audit_ref_value_NEW', 'm_audit_ref_value'
DROP TABLE m_audit_event;
EXEC sp_rename 'm_audit_event_NEW', 'm_audit_event'

-- restoring indices and foreign key constraints
ALTER TABLE m_audit_event ADD PRIMARY KEY (id);
ALTER TABLE m_audit_prop_value ADD PRIMARY KEY (id);
ALTER TABLE m_audit_ref_value ADD PRIMARY KEY (id);

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue);
CREATE INDEX iAuditPropValRecordId ON m_audit_prop_value (record_id);
CREATE INDEX iAuditRefValRecordId ON m_audit_ref_value (record_id);

ALTER TABLE m_audit_delta ADD CONSTRAINT fk_audit_delta FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_item ADD CONSTRAINT fk_audit_item FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_prop_value ADD CONSTRAINT fk_audit_prop_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_ref_value ADD CONSTRAINT fk_audit_ref_value FOREIGN KEY (record_id) REFERENCES m_audit_event;

DROP TABLE m_exclusion;