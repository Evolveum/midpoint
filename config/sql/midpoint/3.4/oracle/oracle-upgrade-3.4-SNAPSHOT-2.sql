ALTER TABLE m_abstract_role ADD (
  ownerRef_relation  VARCHAR2(157 CHAR),
  ownerRef_targetOid VARCHAR2(36 CHAR),
  ownerRef_type      NUMBER(10, 0)
);

