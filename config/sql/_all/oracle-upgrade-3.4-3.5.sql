ALTER TABLE m_object ADD (lifecycleState  VARCHAR2(255 CHAR));

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState) INITRANS 30;

ALTER TABLE m_assignment ADD (lifecycleState  VARCHAR2(255 CHAR));

CREATE TABLE m_assignment_policy_situation (
  assignment_id   NUMBER(10, 0)     NOT NULL,
  assignment_oid  VARCHAR2(36 CHAR) NOT NULL,
  policySituation VARCHAR2(255 CHAR)
) INITRANS 30;

CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR2(36 CHAR) NOT NULL,
  policySituation VARCHAR2(255 CHAR)
) INITRANS 30;

ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation
FOREIGN KEY (assignment_id, assignment_oid)
REFERENCES m_assignment;

ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation
FOREIGN KEY (focus_oid)
REFERENCES m_focus;

CREATE TABLE m_audit_item (
  changedItemPath VARCHAR2(900 CHAR) NOT NULL,
  record_id       NUMBER(19, 0)      NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
) INITRANS 30;

CREATE INDEX iChangedItemPath ON m_audit_item (changedItemPath) INITRANS 30;

ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

-- Quartz

ALTER TABLE qrtz_fired_triggers ADD SCHED_TIME NUMBER(13);
UPDATE QRTZ_FIRED_TRIGGERS SET SCHED_TIME = FIRED_TIME WHERE SCHED_TIME IS NULL;
ALTER TABLE qrtz_fired_triggers MODIFY (SCHED_TIME NOT NULL);

-- Activiti

update ACT_RU_EVENT_SUBSCR set PROC_DEF_ID_ = CONFIGURATION_ where EVENT_TYPE_ = 'message' and PROC_INST_ID_ is null and EXECUTION_ID_ is null;

update ACT_GE_PROPERTY set VALUE_ = '5.22.0.0' where NAME_ = 'schema.version';
