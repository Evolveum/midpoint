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