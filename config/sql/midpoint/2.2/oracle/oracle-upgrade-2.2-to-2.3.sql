CREATE INDEX iParent ON m_task (parent) INITRANS 30;

ALTER TABLE m_sync_situation_description ADD fullFlag NUMBER(1, 0);
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD expectedTotal NUMBER(19, 0);