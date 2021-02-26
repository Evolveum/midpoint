-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- MID-6417
ALTER TABLE m_operation_execution ADD recordType INT;

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';
GO
