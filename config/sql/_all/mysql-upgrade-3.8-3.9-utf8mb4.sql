CREATE TABLE m_object_collection (
  name_norm VARCHAR(191),
  name_orig VARCHAR(191),
  oid       VARCHAR(36)  CHARSET utf8 COLLATE utf8_bin  NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_bin
  ENGINE = InnoDB;

CREATE INDEX iObjectCollectionNameOrig
  ON m_object_collection (name_orig);
ALTER TABLE m_object_collection
  ADD CONSTRAINT uc_object_collection_name UNIQUE (name_norm);

ALTER TABLE m_object_collection
  ADD CONSTRAINT fk_object_collection FOREIGN KEY (oid) REFERENCES m_object (oid);
