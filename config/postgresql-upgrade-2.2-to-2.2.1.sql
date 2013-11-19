CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD COLUMN fullFlag BOOLEAN;
ALTER TABLE m_shadow ADD COLUMN fullSynchronizationTimestamp TIMESTAMP;
