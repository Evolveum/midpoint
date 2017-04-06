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

CREATE TABLE m_audit_prop_value (
  id        NUMBER(19, 0) NOT NULL,
  name      VARCHAR2(255 CHAR),
  record_id NUMBER(19, 0),
  value     VARCHAR2(1024 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE TABLE m_audit_ref_value (
  id              NUMBER(19, 0) NOT NULL,
  name            VARCHAR2(255 CHAR),
  oid             VARCHAR2(255 CHAR),
  record_id       NUMBER(19, 0),
  targetName_norm VARCHAR2(255 CHAR),
  targetName_orig VARCHAR2(255 CHAR),
  type            VARCHAR2(255 CHAR),
  PRIMARY KEY (id)
) INITRANS 30;

CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id) INITRANS 30;

CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id) INITRANS 30;


ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

CREATE TABLE m_object_text_info (
  owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  text      VARCHAR2(255 CHAR) NOT NULL,
  PRIMARY KEY (owner_oid, text)
) INITRANS 30;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE TABLE m_case (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

ALTER TABLE m_case
  ADD CONSTRAINT uc_case_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_case
  ADD CONSTRAINT fk_case
FOREIGN KEY (oid)
REFERENCES m_object;

