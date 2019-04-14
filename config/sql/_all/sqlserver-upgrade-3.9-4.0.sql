CREATE TABLE m_archetype (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE m_generic_object DROP CONSTRAINT fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;

ALTER TABLE m_acc_cert_campaign ADD primaryIdentifierValue NVARCHAR(255) COLLATE database_default;

CREATE UNIQUE NONCLUSTERED INDEX iPrimaryIdentifierValueWithOC
  ON m_shadow(primaryIdentifierValue, objectClass, resourceRef_targetOid)
  WHERE primaryIdentifierValue IS NOT NULL AND objectClass IS NOT NULL AND resourceRef_targetOid IS NOT NULL;

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';