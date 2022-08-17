-- resource templates/inheritance
ALTER TABLE m_resource ADD template BOOLEAN;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.6' WHERE name = 'databaseSchemaVersion';
COMMIT;
