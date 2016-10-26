ALTER TABLE m_object ADD lifecycleState NVARCHAR(255) COLLATE database_default;

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);