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
  ADD CONSTRAINT UK_59yhlpgtqu3a9wvnw0ujx4xl1 UNIQUE (taskIdentifier);
