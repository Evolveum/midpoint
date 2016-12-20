ALTER TABLE m_object ADD lifecycleState NVARCHAR(255) COLLATE database_default;

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);

ALTER TABLE m_assignment ADD lifecycleState NVARCHAR(255) COLLATE database_default;

CREATE TABLE m_assignment_policy_situation (
  assignment_id   INT                                   NOT NULL,
  assignment_oid  NVARCHAR(36) COLLATE database_default NOT NULL,
  policySituation NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_focus_policy_situation (
  focus_oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  policySituation NVARCHAR(255) COLLATE database_default
);

ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation
FOREIGN KEY (assignment_id, assignment_oid)
REFERENCES m_assignment;

ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation
FOREIGN KEY (focus_oid)
REFERENCES m_focus;

CREATE TABLE m_audit_item (
  changedItemPath NVARCHAR(900) COLLATE database_default NOT NULL,
  record_id       BIGINT                                 NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
);

CREATE INDEX iChangedItemPath ON m_audit_item (changedItemPath);

ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

-- Quartz

ALTER TABLE QRTZ_FIRED_TRIGGERS ADD SCHED_TIME BIGINT;
GO;
UPDATE QRTZ_FIRED_TRIGGERS SET SCHED_TIME = FIRED_TIME WHERE SCHED_TIME IS NULL;
ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN SCHED_TIME BIGINT NOT NULL;

-- Activiti

update ACT_RU_EVENT_SUBSCR set PROC_DEF_ID_ = CONFIGURATION_ where EVENT_TYPE_ = 'message' and PROC_INST_ID_ is null and EXECUTION_ID_ is null;

update ACT_GE_PROPERTY set VALUE_ = '5.22.0.0' where NAME_ = 'schema.version';
