CREATE TABLE m_object_collection (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iObjectCollectionNameOrig
  ON m_object_collection (name_orig);
ALTER TABLE m_object_collection
  ADD CONSTRAINT uc_object_collection_name UNIQUE (name_norm);

ALTER TABLE m_object_collection
  ADD CONSTRAINT fk_object_collection FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE m_acc_cert_campaign ADD COLUMN iteration INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE m_acc_cert_case ADD COLUMN iteration INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE m_acc_cert_wi ADD COLUMN iteration INTEGER DEFAULT 1 NOT NULL;

CREATE TABLE m_global_metadata (
  name  VARCHAR(255) NOT NULL,
  value VARCHAR(255),
  PRIMARY KEY (name)
);

INSERT INTO m_global_metadata VALUES ('databaseSchemaVersion', '3.9');