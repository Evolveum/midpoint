-- MID-7484
CREATE TABLE m_message_template (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid       VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
);
CREATE INDEX iMessageTemplateNameOrig
    ON m_message_template (name_orig);
ALTER TABLE IF EXISTS m_message_template
    ADD CONSTRAINT uc_message_template_name UNIQUE (name_norm);
ALTER TABLE IF EXISTS m_message_template
    ADD CONSTRAINT fk_message_template FOREIGN KEY (oid) REFERENCES m_object;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.5' WHERE name = 'databaseSchemaVersion';
COMMIT;
