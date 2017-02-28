CREATE TABLE m_object_text_info (
  owner_oid VARCHAR2(36 CHAR)  NOT NULL,
  text      VARCHAR2(255 CHAR) NOT NULL,
  PRIMARY KEY (owner_oid, text)
) INITRANS 30;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;
