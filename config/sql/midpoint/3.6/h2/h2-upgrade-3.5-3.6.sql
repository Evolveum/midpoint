CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

