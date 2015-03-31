CREATE TABLE m_lookup_table (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_lookup_table_row (
  id                  INT                                   NOT NULL,
  owner_oid           NVARCHAR(36) COLLATE database_default NOT NULL,
  row_key             NVARCHAR(255) COLLATE database_default,
  label_norm          NVARCHAR(255) COLLATE database_default,
  label_orig          NVARCHAR(255) COLLATE database_default,
  lastChangeTimestamp DATETIME2,
  row_value           NVARCHAR(255) COLLATE database_default,
  PRIMARY KEY (id, owner_oid)
);

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key UNIQUE (row_key);

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table_owner
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table;

declare @pkname varchar(255);
set @pkname = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_reference');

execute ('ALTER TABLE m_assignment_reference DROP CONSTRAINT ' + @pkname);
alter table m_assignment_reference add constraint PK_m_a_reference primary key clustered (owner_id, owner_owner_oid, reference_type, relation, targetOid);

set @pkname = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_reference');

execute ('ALTER TABLE m_reference DROP CONSTRAINT ' + @pkname);
alter table m_reference add constraint PK_m_reference primary key clustered (owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_assignment ALTER COLUMN id INT;
ALTER TABLE m_assignment ALTER COLUMN extId INT;
ALTER TABLE m_assignment_ext_date ALTER COLUMN anyContainer_owner_id INT;
ALTER TABLE m_assignment_ext_long ALTER COLUMN anyContainer_owner_id INT;
ALTER TABLE m_assignment_ext_poly ALTER COLUMN anyContainer_owner_id INT;
ALTER TABLE m_assignment_ext_reference ALTER COLUMN anyContainer_owner_id INT;
ALTER TABLE m_assignment_ext_string ALTER COLUMN anyContainer_owner_id INT;
ALTER TABLE m_assignment_extension ALTER COLUMN owner_id INT;
ALTER TABLE m_assignment_reference ALTER COLUMN owner_id INT;
ALTER TABLE m_exclusion ALTER COLUMN id INT;
ALTER TABLE m_trigger ALTER COLUMN id INT;

go

