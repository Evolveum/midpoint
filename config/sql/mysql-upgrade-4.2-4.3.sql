-- MID-6417
ALTER TABLE m_operation_execution ADD COLUMN recordType INTEGER;

-- MID-3669
ALTER TABLE m_focus ADD COLUMN lockoutStatus INTEGER;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

COMMIT;
