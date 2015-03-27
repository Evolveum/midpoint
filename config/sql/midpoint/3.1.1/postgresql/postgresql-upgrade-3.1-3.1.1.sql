CREATE TABLE m_lookup_table (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_lookup_table_row (
  id                  INT2        NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  row_key             VARCHAR(255),
  label_norm          VARCHAR(255),
  label_orig          VARCHAR(255),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR(255),
  PRIMARY KEY (id, owner_oid)
);

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table_owner
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table;

ALTER TABLE m_assignment_reference
DROP CONSTRAINT m_assignment_reference_pkey,
ADD CONSTRAINT m_assignment_reference_pkey PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_reference
DROP CONSTRAINT m_reference_pkey,
ADD CONSTRAINT m_reference_pkey PRIMARY KEY (owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_assignment ALTER COLUMN id TYPE INT4;
ALTER TABLE m_assignment ALTER COLUMN extId TYPE INT4;
ALTER TABLE m_assignment_ext_date ALTER COLUMN anyContainer_owner_id TYPE INT4;
ALTER TABLE m_assignment_ext_long ALTER COLUMN anyContainer_owner_id TYPE INT4;
ALTER TABLE m_assignment_ext_poly ALTER COLUMN anyContainer_owner_id TYPE INT4;
ALTER TABLE m_assignment_ext_reference ALTER COLUMN anyContainer_owner_id TYPE INT4;
ALTER TABLE m_assignment_ext_string ALTER COLUMN anyContainer_owner_id TYPE INT4;
ALTER TABLE m_assignment_extension ALTER COLUMN owner_id TYPE INT4;
ALTER TABLE m_assignment_reference ALTER COLUMN owner_id TYPE INT4;
ALTER TABLE m_exclusion ALTER COLUMN id TYPE INT4;
ALTER TABLE m_lookup_table_row ALTER COLUMN id TYPE INT4;
ALTER TABLE m_trigger ALTER COLUMN id TYPE INT4;
