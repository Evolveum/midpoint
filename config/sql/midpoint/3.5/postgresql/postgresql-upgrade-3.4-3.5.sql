ALTER TABLE m_object ADD lifecycleState VARCHAR(255);

CREATE INDEX iObjectLifecycleState ON m_object (lifecycleState);

ALTER TABLE m_assignment ADD lifecycleState VARCHAR(255);

CREATE TABLE m_assignment_policy_situation (
  assignment_id   INT4        NOT NULL,
  assignment_oid  VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
);

CREATE TABLE m_focus_policy_situation (
  focus_oid       VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
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
  changedItemPath VARCHAR(900) NOT NULL,
  record_id       INT8         NOT NULL,
  PRIMARY KEY (changedItemPath, record_id)
);

CREATE INDEX iChangedItemPath ON m_audit_item (changedItemPath);

ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item
FOREIGN KEY (record_id)
REFERENCES m_audit_event;