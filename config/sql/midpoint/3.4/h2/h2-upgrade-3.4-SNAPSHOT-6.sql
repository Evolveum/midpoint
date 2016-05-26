ALTER TABLE m_abstract_role ADD displayName_norm   VARCHAR(255);
ALTER TABLE m_abstract_role ADD displayName_orig   VARCHAR(255);
ALTER TABLE m_abstract_role ADD identifier         VARCHAR(255);
ALTER TABLE m_abstract_role ADD riskLevel          VARCHAR(255);

ALTER TABLE m_org DROP COLUMN displayName_norm;
ALTER TABLE m_org DROP COLUMN displayName_orig;
ALTER TABLE m_org DROP COLUMN identifier;

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);
