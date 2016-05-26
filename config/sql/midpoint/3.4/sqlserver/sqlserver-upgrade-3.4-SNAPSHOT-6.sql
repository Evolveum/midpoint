ALTER TABLE m_abstract_role ADD
  displayName_norm   NVARCHAR(255) COLLATE database_default,
  displayName_orig   NVARCHAR(255) COLLATE database_default,
  identifier         NVARCHAR(255) COLLATE database_default,
  riskLevel          NVARCHAR(255) COLLATE database_default;

ALTER TABLE m_org DROP COLUMN displayName_norm, displayName_orig, identifier;

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);
