-- MID-7484
CREATE TABLE m_message_template (
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
    oid       VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (oid)
) INITRANS 30;
CREATE INDEX iMessageTemplateNameOrig
    ON m_message_template (name_orig) INITRANS 30;
ALTER TABLE m_message_template
    ADD CONSTRAINT uc_message_template_name UNIQUE (name_norm);
ALTER TABLE m_message_template
    ADD CONSTRAINT fk_message_template FOREIGN KEY (oid) REFERENCES m_object;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.5' WHERE name = 'databaseSchemaVersion';
COMMIT;
