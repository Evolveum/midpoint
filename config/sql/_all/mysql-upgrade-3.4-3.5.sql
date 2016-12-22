ALTER TABLE m_object
  ADD lifecycleState VARCHAR(255);

CREATE INDEX iObjectLifecycleState
  ON m_object (lifecycleState);

ALTER TABLE m_assignment
  ADD lifecycleState VARCHAR(255);

CREATE TABLE m_assignment_policy_situation (
  assignment_id   INTEGER     NOT NULL,
  assignment_oid  VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation
FOREIGN KEY (assignment_id, assignment_oid)
REFERENCES m_assignment (id, owner_oid);

ALTER TABLE m_focus_policy_situation
  ADD CONSTRAINT fk_focus_policy_situation
FOREIGN KEY (focus_oid)
REFERENCES m_focus (oid);

CREATE TABLE m_audit_item (
  changedItemPath VARCHAR(255) NOT NULL,
  record_id       BIGINT       NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE INDEX iChangedItemPath
  ON m_audit_item (changedItemPath);

ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

-- Quartz

ALTER TABLE QRTZ_FIRED_TRIGGERS ADD COLUMN SCHED_TIME BIGINT(13);
UPDATE QRTZ_FIRED_TRIGGERS SET SCHED_TIME = FIRED_TIME WHERE SCHED_TIME IS NULL;
ALTER TABLE QRTZ_FIRED_TRIGGERS MODIFY SCHED_TIME BIGINT(13) NOT NULL;

-- Activiti

update ACT_RU_EVENT_SUBSCR set PROC_DEF_ID_ = CONFIGURATION_ where EVENT_TYPE_ = 'message' and PROC_INST_ID_ is null and EXECUTION_ID_ is null;

update ACT_GE_PROPERTY set VALUE_ = '5.22.0.0' where NAME_ = 'schema.version';
