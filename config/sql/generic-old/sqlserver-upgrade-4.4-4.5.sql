-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- MID-7484
-- TODO

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.5' WHERE name = 'databaseSchemaVersion';

GO
