ALTER TABLE m_task ADD wfEndTimestamp           TIMESTAMP;
ALTER TABLE m_task ADD wfObjectRef_relation     VARCHAR(157);
ALTER TABLE m_task ADD wfObjectRef_targetOid    VARCHAR(36);
ALTER TABLE m_task ADD wfObjectRef_type         INTEGER;
ALTER TABLE m_task ADD wfProcessInstanceId      VARCHAR(255);
ALTER TABLE m_task ADD wfRequesterRef_relation  VARCHAR(157);
ALTER TABLE m_task ADD wfRequesterRef_targetOid VARCHAR(36);
ALTER TABLE m_task ADD wfRequesterRef_type      INTEGER;
ALTER TABLE m_task ADD wfStartTimestamp         TIMESTAMP;
ALTER TABLE m_task ADD wfTargetRef_relation     VARCHAR(157);
ALTER TABLE m_task ADD wfTargetRef_targetOid    VARCHAR(36);
ALTER TABLE m_task ADD wfTargetRef_type         INTEGER;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

