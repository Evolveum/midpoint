-- resource templates/inheritance
ALTER TABLE m_resource ADD template BOOLEAN;

-- MID-8053: "Active" connectors detection
ALTER TABLE m_connector ADD available BOOLEAN;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.6' WHERE name = 'databaseSchemaVersion';
