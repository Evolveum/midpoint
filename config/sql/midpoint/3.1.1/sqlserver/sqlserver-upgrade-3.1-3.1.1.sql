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

ALTER TABLE m_assignment_reference DROP CONSTRAINT fk_assignment_reference;

set @pkname = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_reference');
execute ('ALTER TABLE m_reference DROP CONSTRAINT ' + @pkname);

declare @n1 varchar(255);
set @n1 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment');
execute ('ALTER TABLE m_assignment DROP CONSTRAINT ' + @n1);

declare @n2 varchar(255);
set @n2 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_ext_date');
execute ('ALTER TABLE m_assignment_ext_date DROP CONSTRAINT ' + @n2);

ALTER TABLE m_assignment_ext_date DROP CONSTRAINT fk_assignment_ext_date;

declare @n3 varchar(255);
set @n3 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_ext_long');
execute ('ALTER TABLE m_assignment_ext_long DROP CONSTRAINT ' + @n3);

ALTER TABLE m_assignment_ext_long DROP CONSTRAINT fk_assignment_ext_long;

declare @n4 varchar(255);
set @n4 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_ext_poly');
execute ('ALTER TABLE m_assignment_ext_poly DROP CONSTRAINT ' + @n4);

ALTER TABLE m_assignment_ext_poly DROP CONSTRAINT fk_assignment_ext_poly;

declare @n5 varchar(255);
set @n5 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_ext_reference');
execute ('ALTER TABLE m_assignment_ext_reference DROP CONSTRAINT ' + @n5);

ALTER TABLE m_assignment_ext_reference DROP CONSTRAINT fk_assignment_ext_reference;

declare @n6 varchar(255);
set @n6 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_ext_string');
execute ('ALTER TABLE m_assignment_ext_string DROP CONSTRAINT ' + @n6);

ALTER TABLE m_assignment_ext_string DROP CONSTRAINT fk_assignment_ext_string;

declare @n7 varchar(255);
set @n7 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_assignment_extension');
execute ('ALTER TABLE m_assignment_extension DROP CONSTRAINT ' + @n7);

declare @n8 varchar(255);
set @n8 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_exclusion');
execute ('ALTER TABLE m_exclusion DROP CONSTRAINT ' + @n8);

declare @n9 varchar(255);
set @n9 = (SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'm_trigger');
execute ('ALTER TABLE m_trigger DROP CONSTRAINT ' + @n9);

ALTER TABLE m_assignment ALTER COLUMN id INT NOT NULL;
ALTER TABLE m_assignment ALTER COLUMN extId INT;
ALTER TABLE m_assignment_ext_date ALTER COLUMN anyContainer_owner_id INT NOT NULL;
ALTER TABLE m_assignment_ext_long ALTER COLUMN anyContainer_owner_id INT NOT NULL;
ALTER TABLE m_assignment_ext_poly ALTER COLUMN anyContainer_owner_id INT NOT NULL;
ALTER TABLE m_assignment_ext_reference ALTER COLUMN anyContainer_owner_id INT NOT NULL;
ALTER TABLE m_assignment_ext_string ALTER COLUMN anyContainer_owner_id INT NOT NULL;
ALTER TABLE m_assignment_extension ALTER COLUMN owner_id INT NOT NULL;
ALTER TABLE m_assignment_reference ALTER COLUMN owner_id INT NOT NULL;
ALTER TABLE m_exclusion ALTER COLUMN id INT NOT NULL;
ALTER TABLE m_trigger ALTER COLUMN id INT NOT NULL;

alter table m_assignment
add constraint PK_m_assignment primary key clustered (id, owner_oid);

alter table m_assignment_reference
add constraint PK_m_a_reference primary key clustered (owner_id, owner_owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_assignment_reference
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment;

alter table m_reference
add constraint PK_m_reference primary key clustered (owner_oid, reference_type, relation, targetOid);

alter table m_assignment_extension
add constraint PK_m_assignment_extension primary key clustered (owner_id, owner_owner_oid);

ALTER TABLE m_assignment_ext_date
ADD CONSTRAINT fk_assignment_ext_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

alter table m_assignment_ext_date
add constraint PK_m_assignment_ext_date primary key clustered (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue);

ALTER TABLE m_assignment_ext_long
ADD CONSTRAINT fk_assignment_ext_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

alter table m_assignment_ext_long
add constraint PK_m_assignment_ext_long primary key clustered (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue);

ALTER TABLE m_assignment_ext_poly
ADD CONSTRAINT fk_assignment_ext_poly
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

alter table m_assignment_ext_poly
add constraint PK_m_assignment_ext_poly primary key clustered (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig);

ALTER TABLE m_assignment_ext_reference
ADD CONSTRAINT fk_assignment_ext_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

alter table m_assignment_ext_reference
add constraint PK_m_assignment_ext_reference primary key clustered (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid);

ALTER TABLE m_assignment_ext_string
ADD CONSTRAINT fk_assignment_ext_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

alter table m_assignment_ext_string
add constraint PK_m_assignment_ext_string primary key clustered (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue);

alter table m_exclusion
add constraint PK_m_exclusion primary key clustered (id, owner_oid);

alter table m_trigger
add constraint PK_m_trigger primary key clustered (id, owner_oid);

go
