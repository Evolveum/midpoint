CREATE TABLE m_object_collection (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iObjectCollectionNameOrig
  ON m_object_collection (name_orig);
ALTER TABLE m_object_collection
  ADD CONSTRAINT uc_object_collection_name UNIQUE (name_norm);

ALTER TABLE m_object_collection
  ADD CONSTRAINT fk_object_collection FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE m_acc_cert_campaign ADD iteration INT DEFAULT 1 NOT NULL;
ALTER TABLE m_acc_cert_case ADD iteration INT DEFAULT 1 NOT NULL;
ALTER TABLE m_acc_cert_wi ADD iteration INT DEFAULT 1 NOT NULL;

CREATE TABLE m_global_metadata (
  name  NVARCHAR(255) COLLATE database_default NOT NULL,
  value NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (name)
);

INSERT INTO m_global_metadata VALUES ('databaseSchemaVersion', '3.9');