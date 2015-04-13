CREATE TABLE m_lookup_table (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE TABLE m_lookup_table_row (
  id                  NUMBER(10, 0)      NOT NULL,
  owner_oid           VARCHAR2(36 CHAR) NOT NULL,
  row_key             VARCHAR2(255 CHAR),
  label_norm          VARCHAR2(255 CHAR),
  label_orig          VARCHAR2(255 CHAR),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR2(255 CHAR),
  PRIMARY KEY (id, owner_oid)
) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm) INITRANS 30;

ALTER TABLE m_lookup_table
ADD CONSTRAINT fk_lookup_table
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT uc_row_key  unique (row_key) INITRANS 30;

ALTER TABLE m_lookup_table_row
ADD CONSTRAINT fk_lookup_table_owner
FOREIGN KEY (owner_oid)
REFERENCES m_lookup_table;

ALTER TABLE m_assignment_reference
DROP PRIMARY KEY;
ALTER TABLE m_assignment_reference
ADD PRIMARY KEY (owner_id, owner_owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_reference
DROP PRIMARY KEY;
ALTER TABLE m_reference
ADD PRIMARY KEY (owner_oid, reference_type, relation, targetOid);

ALTER TABLE m_assignment MODIFY (id NUMBER(10, 0));
ALTER TABLE m_assignment MODIFY (extId NUMBER(10, 0));
ALTER TABLE m_assignment_ext_date MODIFY (anyContainer_owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_ext_long MODIFY (anyContainer_owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_ext_poly MODIFY (anyContainer_owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_ext_reference MODIFY (anyContainer_owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_ext_string MODIFY (anyContainer_owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_extension MODIFY (owner_id NUMBER(10, 0));
ALTER TABLE m_assignment_reference MODIFY (owner_id NUMBER(10, 0));
ALTER TABLE m_exclusion MODIFY (id NUMBER(10, 0));
ALTER TABLE m_trigger MODIFY (id NUMBER(10, 0));

CREATE TABLE m_assignment_ext_boolean (
  eName                        VARCHAR2(157 CHAR) NOT NULL,
  anyContainer_owner_id        NUMBER(10, 0)      NOT NULL,
  anyContainer_owner_owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  booleanValue                 NUMBER(1, 0)       NOT NULL,
  extensionType                NUMBER(10, 0),
  dynamicDef                   NUMBER(1, 0),
  eType                        VARCHAR2(157 CHAR),
  valueType                    NUMBER(10, 0),
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue)
) INITRANS 30;

ALTER TABLE m_assignment_extension ADD booleansCount NUMBER(5, 0);

ALTER TABLE m_object ADD booleansCount NUMBER(5, 0);

CREATE TABLE m_object_ext_boolean (
  eName        VARCHAR2(157 CHAR) NOT NULL,
  owner_oid    VARCHAR2(36 CHAR)  NOT NULL,
  ownerType    NUMBER(10, 0)      NOT NULL,
  booleanValue NUMBER(1, 0)       NOT NULL,
  dynamicDef   NUMBER(1, 0),
  eType        VARCHAR2(157 CHAR),
  valueType    NUMBER(10, 0),
  PRIMARY KEY (eName, owner_oid, ownerType, booleanValue)
) INITRANS 30;

CREATE INDEX iAExtensionBoolean ON m_assignment_ext_boolean (extensionType, eName, booleanValue) INITRANS 30;

CREATE INDEX iExtensionBoolean ON m_object_ext_boolean (ownerType, eName, booleanValue) INITRANS 30;

CREATE INDEX iExtensionBooleanDef ON m_object_ext_boolean (owner_oid, ownerType) INITRANS 30;

ALTER TABLE m_assignment_ext_boolean
ADD CONSTRAINT fk_assignment_ext_boolean
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

ALTER TABLE m_object_ext_boolean
ADD CONSTRAINT fk_object_ext_boolean
FOREIGN KEY (owner_oid)
REFERENCES m_object;
