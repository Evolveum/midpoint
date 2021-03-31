-- MID-6417
ALTER TABLE m_operation_execution ADD COLUMN recordType INT4;

-- MID-3669
ALTER TABLE m_focus ADD COLUMN lockoutStatus INT4;

-- MID-6691
CREATE INDEX CONCURRENTLY IF NOT EXISTS iObjectExtStringOwnerTypeItemIdLcValue
    ON M_OBJECT_EXT_STRING(ownerType, ITEM_ID, (lower(stringvalue)));
/* TODO what if this is better?
CREATE INDEX CONCURRENTLY IF NOT EXISTS iObjectExtStringItemIdLcValue0
    ON M_OBJECT_EXT_STRING(ITEM_ID, (lower(stringvalue)))
         WHERE ownerType = 0;
*/

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

COMMIT;
