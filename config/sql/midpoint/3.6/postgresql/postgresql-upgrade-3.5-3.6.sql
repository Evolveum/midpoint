CREATE TABLE m_object_text_info (
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
);

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;
