CREATE TABLE m_service (
  displayOrder  NUMBER(10, 0),
  locality_norm VARCHAR2(255 CHAR),
  locality_orig VARCHAR2(255 CHAR),
  name_norm     VARCHAR2(255 CHAR),
  name_orig     VARCHAR2(255 CHAR),
  oid           VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_service_type (
  service_oid VARCHAR2(36 CHAR) NOT NULL,
  serviceType VARCHAR2(255 CHAR)
) INITRANS 30;

ALTER TABLE m_service
  ADD CONSTRAINT fk_service
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type
FOREIGN KEY (service_oid)
REFERENCES m_service;