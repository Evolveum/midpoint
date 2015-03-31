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
  id                  INTEGER    NOT NULL,
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
ADD CONSTRAINT uc_row_key UNIQUE (row_key);

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

ALTER TABLE m_assignment MODIFY id INTEGER;
ALTER TABLE m_assignment MODIFY extId INTEGER;
ALTER TABLE m_assignment_ext_date MODIFY anyContainer_owner_id INTEGER;
ALTER TABLE m_assignment_ext_long MODIFY anyContainer_owner_id INTEGER;
ALTER TABLE m_assignment_ext_poly MODIFY anyContainer_owner_id INTEGER;
ALTER TABLE m_assignment_ext_reference MODIFY anyContainer_owner_id INTEGER;
ALTER TABLE m_assignment_ext_string MODIFY anyContainer_owner_id INTEGER;
ALTER TABLE m_assignment_extension MODIFY owner_id INTEGER;
ALTER TABLE m_assignment_reference MODIFY owner_id INTEGER;
ALTER TABLE m_exclusion MODIFY id INTEGER;
ALTER TABLE m_trigger MODIFY id INTEGER;
