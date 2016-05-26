ALTER TABLE m_abstract_role ADD (
  displayName_norm   VARCHAR2(255 CHAR),
  displayName_orig   VARCHAR2(255 CHAR),
  identifier         VARCHAR2(255 CHAR),
  riskLevel          VARCHAR2(255 CHAR));

ALTER TABLE m_org DROP (displayName_norm, displayName_orig, identifier);

CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier) INITRANS 30;