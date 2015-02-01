DROP TABLE m_org_closure;

IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'm_org_incorrect')
  DROP TABLE m_org_incorrect;

CREATE TABLE m_org_closure (
  ancestor_oid   NVARCHAR(36) COLLATE database_default NOT NULL,
  descendant_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  val            INT,
  PRIMARY KEY (ancestor_oid, descendant_oid)
);

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid);

CREATE INDEX iDescendant ON m_org_closure (descendant_oid);

CREATE INDEX iDescendantAncestor ON m_org_closure (descendant_oid, ancestor_oid);

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object;
