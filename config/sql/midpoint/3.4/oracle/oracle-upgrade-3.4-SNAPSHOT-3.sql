ALTER TABLE m_task ADD (
  wfEndTimestamp           TIMESTAMP,
  wfObjectRef_relation     VARCHAR2(157 CHAR),
  wfObjectRef_targetOid    VARCHAR2(36 CHAR),
  wfObjectRef_type         NUMBER(10, 0),
  wfProcessInstanceId      VARCHAR2(255 CHAR),
  wfRequesterRef_relation  VARCHAR2(157 CHAR),
  wfRequesterRef_targetOid VARCHAR2(36 CHAR),
  wfRequesterRef_type      NUMBER(10, 0),
  wfStartTimestamp         TIMESTAMP,
  wfTargetRef_relation     VARCHAR2(157 CHAR),
  wfTargetRef_targetOid    VARCHAR2(36 CHAR),
  wfTargetRef_type         NUMBER(10, 0)
);

CREATE INDEX iTaskWfProcessInstanceId ON m_task (wfProcessInstanceId) INITRANS 30;

CREATE INDEX iTaskWfStartTimestamp ON m_task (wfStartTimestamp) INITRANS 30;

CREATE INDEX iTaskWfEndTimestamp ON m_task (wfEndTimestamp) INITRANS 30;

CREATE INDEX iTaskWfRequesterOid ON m_task (wfRequesterRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfObjectOid ON m_task (wfObjectRef_targetOid) INITRANS 30;

CREATE INDEX iTaskWfTargetOid ON m_task (wfTargetRef_targetOid) INITRANS 30;

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue) INITRANS 30;
