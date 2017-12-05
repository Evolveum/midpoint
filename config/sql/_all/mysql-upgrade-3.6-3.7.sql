ALTER TABLE QRTZ_TRIGGERS ADD COLUMN EXECUTION_GROUP VARCHAR(200) NULL;
ALTER TABLE QRTZ_FIRED_TRIGGERS ADD COLUMN EXECUTION_GROUP VARCHAR(200) NULL;

CREATE TABLE m_function_library (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);

ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_abstract_role ADD COLUMN autoassign_enabled BIT;

CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);

ALTER TABLE m_task
  ADD CONSTRAINT uc_task_identifier UNIQUE (taskIdentifier);

ALTER TABLE m_audit_event ADD COLUMN attorneyName VARCHAR(255);
ALTER TABLE m_audit_event ADD COLUMN attorneyOid VARCHAR(36);
ALTER TABLE m_audit_event ADD COLUMN initiatorType INTEGER;

CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);
