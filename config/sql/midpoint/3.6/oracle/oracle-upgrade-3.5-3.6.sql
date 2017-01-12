CREATE TABLE m_form (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_form
  ADD CONSTRAINT fk_form
FOREIGN KEY (oid)
REFERENCES m_object;

