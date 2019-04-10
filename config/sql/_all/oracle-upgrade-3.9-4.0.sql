CREATE TABLE m_archetype (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
) INITRANS 30;

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig) INITRANS 30;
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm) INITRANS 30;

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_shadow ADD primaryIdentifierValue VARCHAR2(255 CHAR);

ALTER TABLE m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';