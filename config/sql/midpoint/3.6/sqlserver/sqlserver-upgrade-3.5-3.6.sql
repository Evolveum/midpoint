CREATE TABLE m_form (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

