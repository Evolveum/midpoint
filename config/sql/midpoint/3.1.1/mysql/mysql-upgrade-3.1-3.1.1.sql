CREATE TABLE m_lookup_table (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

CREATE TABLE m_lookup_table_row (
  id                  SMALLINT    NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  row_key             VARCHAR(255),
  label_norm          VARCHAR(255),
  label_orig          VARCHAR(255),
  lastChangeTimestamp DATETIME,
  row_value           VARCHAR(255),
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table_owner
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table (oid);

ALTER TABLE m_assignment_reference
DROP PRIMARY KEY,
ADD PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_reference
DROP PRIMARY KEY,
ADD PRIMARY KEY (owner_oid, reference_type, relation, targetOid);