-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- resource templates/inheritance
ALTER TABLE m_resource ADD template BIT;

-- MID-8053: "Active" connectors detection
ALTER TABLE m_connector ADD available BIT;

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.6' WHERE name = 'databaseSchemaVersion';

GO
