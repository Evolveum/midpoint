ALTER TABLE m_lookup_table_row
DROP CONSTRAINT uc_row_key;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);
