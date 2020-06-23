-- 2020-05-29 09:20

CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);

BEGIN TRANSACTION
UPDATE m_global_metadata SET value = '4.2' WHERE name = 'databaseSchemaVersion';
COMMIT;

GO
