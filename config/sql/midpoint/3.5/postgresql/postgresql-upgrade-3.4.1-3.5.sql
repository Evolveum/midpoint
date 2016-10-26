ALTER TABLE m_object ADD lifecycleState VARCHAR(255);

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);