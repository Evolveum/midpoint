ALTER TABLE m_object ADD (lifecycleState  VARCHAR2(255 CHAR));

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState) INITRANS 30;