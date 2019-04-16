CREATE TABLE m_archetype (
  name_norm VARCHAR(191),
  name_orig VARCHAR(191),
  oid       VARCHAR(36) CHARSET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_bin
  ENGINE = InnoDB;

CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);

ALTER TABLE m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role(oid);

ALTER TABLE m_generic_object DROP FOREIGN KEY fk_generic_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus(oid);

ALTER TABLE m_shadow ADD COLUMN primaryIdentifierValue VARCHAR(191);

ALTER TABLE m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);

UPDATE m_global_metadata SET value = '4.0' WHERE name = 'databaseSchemaVersion';