CREATE TABLE m_lookup_table (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_lookup_table_row (
  row_key             VARCHAR2(255 CHAR) NOT NULL,
  owner_oid           VARCHAR2(36 CHAR)  NOT NULL,
  label_norm          VARCHAR2(255 CHAR),
  label_orig          VARCHAR2(255 CHAR),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR2(255 CHAR),
  PRIMARY KEY (row_key, owner_oid)
) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table;
