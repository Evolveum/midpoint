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