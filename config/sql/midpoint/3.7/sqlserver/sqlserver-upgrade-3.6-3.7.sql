CREATE TABLE m_function_library (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);

ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_abstract_role ADD autoassign_enabled BIT;

CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);

ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);

ALTER TABLE m_audit_event ADD attorneyName NVARCHAR(255) COLLATE database_default;
ALTER TABLE m_audit_event ADD attorneyOid NVARCHAR(36) COLLATE database_default;
ALTER TABLE m_audit_event ADD initiatorType INT;

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);
