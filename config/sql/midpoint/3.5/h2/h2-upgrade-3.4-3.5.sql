ALTER TABLE m_object ADD lifecycleState VARCHAR(157);

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);