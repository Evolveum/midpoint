DROP TABLE m_org_closure;

DROP TABLE IF EXISTS m_org_incorrect;

CREATE TABLE m_org_closure (
  descendant_oid VARCHAR(36) NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL,
  val            INTEGER     NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
);

CREATE INDEX iDescendant ON m_org_closure (descendant_oid);

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid);

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object;
