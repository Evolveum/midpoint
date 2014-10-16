DROP TABLE m_org_closure;

BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE m_org_incorrect';
  EXCEPTION
  WHEN OTHERS THEN
  IF SQLCODE != -942 THEN
    RAISE;
  END IF;
END;

CREATE TABLE m_org_closure (
  descendant_oid VARCHAR2(36 CHAR) NOT NULL,
  ancestor_oid   VARCHAR2(36 CHAR) NOT NULL,
  val     NUMBER(10, 0) NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
) INITRANS 30;

BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE m_org_closure_temp_delta';
  EXCEPTION
  WHEN OTHERS THEN
  IF SQLCODE != -942 THEN
    RAISE;
  END IF;
END;

CREATE GLOBAL TEMPORARY TABLE m_org_closure_temp_delta (
  descendant_oid VARCHAR2(36 CHAR) NOT NULL,
  ancestor_oid VARCHAR2(36 CHAR) NOT NULL,
  val NUMBER (10, 0) NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
) ON COMMIT DELETE ROWS;

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid) INITRANS 30;

CREATE INDEX iDescendant ON m_org_closure (descendant_oid) INITRANS 30;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object;

