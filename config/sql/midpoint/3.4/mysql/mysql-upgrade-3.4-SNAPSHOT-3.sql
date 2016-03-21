ALTER TABLE m_task
 ADD wfEndTimestamp           DATETIME(6),
 ADD wfObjectRef_relation     VARCHAR(157),
 ADD wfObjectRef_targetOid    VARCHAR(36),
 ADD wfObjectRef_type         INTEGER,
 ADD wfProcessInstanceId      VARCHAR(255),
 ADD wfRequesterRef_relation  VARCHAR(157),
 ADD wfRequesterRef_targetOid VARCHAR(36),
 ADD wfRequesterRef_type      INTEGER,
 ADD wfStartTimestamp         DATETIME(6),
 ADD wfTargetRef_relation     VARCHAR(157),
 ADD wfTargetRef_targetOid    VARCHAR(36),
 ADD wfTargetRef_type         INTEGER;

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId);

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp);

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp);

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid);

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid);

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid);

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);
