CREATE TABLE m_function_library (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_abstract_role ADD autoassign_enabled NUMBER(1, 0);

CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled) INITRANS 30;

ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier) INITRANS 30;

ALTER TABLE m_audit_event ADD attorneyName VARCHAR2(255 CHAR);
ALTER TABLE m_audit_event ADD attorneyOid VARCHAR2(36 CHAR);
ALTER TABLE m_audit_event ADD initiatorType NUMBER(10, 0);

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid) INITRANS 30;
