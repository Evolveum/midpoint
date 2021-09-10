-- MID-7173
ALTER TABLE m_task ADD COLUMN schedulingState INTEGER;
ALTER TABLE m_task ADD COLUMN autoScalingMode INTEGER;
ALTER TABLE m_node ADD COLUMN operationalState INTEGER;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.4' WHERE name = 'databaseSchemaVersion';
