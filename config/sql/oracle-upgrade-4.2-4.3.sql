-- MID-6417
ALTER TABLE m_operation_execution ADD recordType NUMBER(10, 0);

-- MID-3669
ALTER TABLE m_focus ADD lockoutStatus NUMBER(10, 0);

-- MID-6691
CREATE INDEX iObjectExtString_OT_II_LVAL
    ON M_OBJECT_EXT_STRING(ownerType, ITEM_ID, (lower(stringvalue))) INITRANS 30;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

COMMIT;
