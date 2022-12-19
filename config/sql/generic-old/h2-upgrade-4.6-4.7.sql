CREATE TABLE m_tag (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    uri       VARCHAR(255),
    oid       VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
);

CREATE INDEX iTagNameOrig
    ON m_tag (name_orig);
ALTER TABLE m_tag
    ADD CONSTRAINT uc_tag_name UNIQUE (name_norm);

ALTER TABLE m_tag
    ADD CONSTRAINT fk_tag FOREIGN KEY (oid) REFERENCES m_object;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.7' WHERE name = 'databaseSchemaVersion';
