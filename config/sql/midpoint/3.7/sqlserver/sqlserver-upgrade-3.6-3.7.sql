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

ALTER TABLE m_abstract_role ADD COLUMN autoassign_enabled BIT;

CREATE INDEX iAutoassignEnabled
  ON m_abstract_role (autoassign_enabled);

ALTER TABLE m_task
  ADD CONSTRAINT UK_59yhlpgtqu3a9wvnw0ujx4xl1 UNIQUE (taskIdentifier);