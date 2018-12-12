CREATE TABLE m_archetype (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

ALTER TABLE IF EXISTS m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;

ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;


UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';