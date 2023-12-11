-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- 4.4 -> 4.5

-- MID-7484
CREATE TABLE m_message_template
(
    name_norm NVARCHAR(255) COLLATE database_default,
    name_orig NVARCHAR(255) COLLATE database_default,
    oid       NVARCHAR(36) COLLATE database_default NOT NULL,
    PRIMARY KEY (oid)
);
CREATE INDEX iMessageTemplateNameOrig
    ON m_message_template (name_orig);
ALTER TABLE m_message_template
    ADD CONSTRAINT uc_message_template_name UNIQUE (name_norm);
ALTER TABLE m_message_template
    ADD CONSTRAINT fk_message_template FOREIGN KEY (oid) REFERENCES m_object;

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.5' WHERE name = 'databaseSchemaVersion';

GO

-- 4.5 -> 4.6

-- resource templates/inheritance
ALTER TABLE m_resource ADD template BIT;

-- MID-8053: "Active" connectors detection
ALTER TABLE m_connector ADD available BIT;

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.6' WHERE name = 'databaseSchemaVersion';

GO

-- 4.6 -> 4.7

-- NOTHING

-- 4.7 -> 4.8

-- NOTHING
