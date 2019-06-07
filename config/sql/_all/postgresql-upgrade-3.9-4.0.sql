CREATE TABLE m_archetype (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_dashboard (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

CREATE INDEX iDashboardNameOrig
  ON m_dashboard (name_orig);
ALTER TABLE m_dashboard
  ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);

ALTER TABLE IF EXISTS m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;

ALTER TABLE IF EXISTS m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_shadow ADD COLUMN primaryIdentifierValue VARCHAR(255);

ALTER TABLE IF EXISTS m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

ALTER TABLE m_audit_event ADD COLUMN requestIdentifier VARCHAR(255);

ALTER TABLE m_case ADD COLUMN parentRef_relation VARCHAR(157),
                   ADD COLUMN parentRef_targetOid VARCHAR(36),
                   ADD COLUMN parentRef_type INT4,
                   ADD COLUMN targetRef_relation VARCHAR(157),
                   ADD COLUMN targetRef_targetOid VARCHAR(36),
                   ADD COLUMN targetRef_type INT4;

-- This is no longer valid
-- ALTER TABLE IF EXISTS m_case
--  ADD CONSTRAINT uc_case_name UNIQUE (name_norm);
--
-- So use the following if you have it defined:
-- ALTER TABLE IF EXISTS m_case
--     DROP CONSTRAINT uc_case_name;

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';
