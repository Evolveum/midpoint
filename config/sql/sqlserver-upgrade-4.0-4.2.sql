-- 2020-05-29 09:20

CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);

BEGIN TRANSACTION
UPDATE m_global_metadata SET value = '4.2' WHERE name = 'databaseSchemaVersion';
COMMIT;

-- 2020-06-25 11:35

UPDATE m_acc_cert_campaign SET definitionRef_type = convert(INT, definitionRef_targetType) where definitionRef_targetType is not null;

GO
