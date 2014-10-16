DROP TABLE m_org_closure;

drop table if exists m_org_incorrect;

--
-- Note: if getting
--    "[HY000][150] Create table 'midpoint/#sql-7ec_8' with foreign key constraint failed.
--    There is no index in the referenced table where the referenced columns appear as the first columns."
-- then check the character set/collation compatibility between ancestor_oid/descendant_oid and m_object(oid).
-- E.g. try to remove "DEFAULT CHARACTER SET utf8 COLLATE utf8_bin" from definition; or add the same to m_object.
--

CREATE TABLE m_org_closure (
  descendant_oid VARCHAR(36) NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL ,
  val            INTEGER NOT NULL ,
  PRIMARY KEY (descendant_oid, ancestor_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

ALTER TABLE m_org_closure
ADD INDEX fk_ancestor (ancestor_oid),
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object (oid);

ALTER TABLE m_org_closure
ADD INDEX fk_descendant (descendant_oid),
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object (oid);
