CREATE TABLE m_service (
  displayOrder  INT,
  locality_norm NVARCHAR(255) COLLATE database_default,
  locality_orig NVARCHAR(255) COLLATE database_default,
  name_norm     NVARCHAR(255) COLLATE database_default,
  name_orig     NVARCHAR(255) COLLATE database_default,
  oid           NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_service_type (
  service_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  serviceType NVARCHAR(255) COLLATE database_default
);

ALTER TABLE m_service
  ADD CONSTRAINT fk_service
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_service_type
  ADD CONSTRAINT fk_service_type
FOREIGN KEY (service_oid)
REFERENCES m_service;