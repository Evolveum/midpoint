/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- This is the update script for the MAIN REPOSITORY, it will not work for a separate audit database.
-- It is safe to run this script repeatedly, so if you're not sure, just run it to be up to date.
-- DO NOT use explicit COMMIT commands inside the apply_change blocks - leave that to the procedure.
-- If necessary, split your changes into multiple apply_changes calls to enforce the commit
-- before another change - for example when adding values to the custom enum types.

-- Using psql is strongly recommended, don't use tools with messy autocommit behavior like pgAdmin!
-- Using flag to stop on first error is also recommended, for example:
-- psql -v ON_ERROR_STOP=1 -h localhost -U midpoint -W -d midpoint -f postgres-new-upgrade.sql

-- SCHEMA-COMMIT is a Git commit which should be used to initialize the DB for testing changes below it.
-- Check out that commit and initialize a fresh DB with postgres-new-audit.sql to test upgrades.

DO $$
    BEGIN
        if to_regproc('apply_change') is null then
            raise exception 'You are running MAIN UPGRADE script, but the procedure ''apply_change'' is missing.
Are you sure you are running this upgrade script on the correct database?
Current database name is ''%'', schema name is ''%''.', current_database(), current_schema();
        end if;
    END
$$;

-- SCHEMA-COMMIT 4.4: commit 20ad200b
-- see: https://github.com/Evolveum/midpoint/blob/20ad200bd10a114fd70d2d131c0d11b5cd920150/config/sql/native-new/postgres-new.sql

-- changes for 4.4.1

-- adding trigger to mark org closure for refresh when org is inserted/deleted
call apply_change(1, $aa$
-- The trigger that flags the view for refresh after m_org changes.
CREATE OR REPLACE FUNCTION mark_org_closure_for_refresh_org()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO m_global_metadata VALUES ('orgClosureRefreshNeeded', 'true')
    ON CONFLICT (name) DO UPDATE SET value = 'true';

    -- after trigger returns null
    RETURN NULL;
END $$;

-- Update is not necessary, it does not change relations between orgs.
-- If it does, it is handled by trigger on m_ref_object_parent_org.
CREATE TRIGGER m_org_mark_refresh_tr
    AFTER INSERT OR DELETE ON m_org
    FOR EACH ROW EXECUTE FUNCTION mark_org_closure_for_refresh_org();
CREATE TRIGGER m_org_mark_refresh_trunc_tr
    AFTER TRUNCATE ON m_org
    FOR EACH STATEMENT EXECUTE FUNCTION mark_org_closure_for_refresh_org();
$aa$);

-- SCHEMA-COMMIT 4.4.1: commit de18c14f

-- changes for 4.5

-- MID-7484
-- We add the new enum value in separate change, because it must be committed before it is used.
call apply_change(2, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MESSAGE_TEMPLATE' AFTER 'LOOKUP_TABLE';
$aa$);

call apply_change(3, $aa$
CREATE TABLE m_message_template (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('MESSAGE_TEMPLATE') STORED
        CHECK (objectType = 'MESSAGE_TEMPLATE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_message_template_oid_insert_tr BEFORE INSERT ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_message_template_update_tr BEFORE UPDATE ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_message_template_oid_delete_tr AFTER DELETE ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_message_template_nameOrig_idx ON m_message_template (nameOrig);
CREATE UNIQUE INDEX m_message_template_nameNorm_key ON m_message_template (nameNorm);
CREATE INDEX m_message_template_policySituation_idx
    ON m_message_template USING gin(policysituations gin__int_ops);
CREATE INDEX m_message_template_createTimestamp_idx ON m_message_template (createTimestamp);
CREATE INDEX m_message_template_modifyTimestamp_idx ON m_message_template (modifyTimestamp);
$aa$);

-- MID-7487 Identity matching
-- We add the new enum value in separate change, because it must be committed before it is used.
call apply_change(4, $aa$
CREATE TYPE CorrelationSituationType AS ENUM ('UNCERTAIN', 'EXISTING_OWNER', 'NO_OWNER', 'ERROR');
$aa$);

call apply_change(5, $aa$
ALTER TABLE m_shadow
ADD COLUMN correlationStartTimestamp TIMESTAMPTZ,
ADD COLUMN correlationEndTimestamp TIMESTAMPTZ,
ADD COLUMN correlationCaseOpenTimestamp TIMESTAMPTZ,
ADD COLUMN correlationCaseCloseTimestamp TIMESTAMPTZ,
ADD COLUMN correlationSituation CorrelationSituationType;

CREATE INDEX m_shadow_correlationStartTimestamp_idx ON m_shadow (correlationStartTimestamp);
CREATE INDEX m_shadow_correlationEndTimestamp_idx ON m_shadow (correlationEndTimestamp);
CREATE INDEX m_shadow_correlationCaseOpenTimestamp_idx ON m_shadow (correlationCaseOpenTimestamp);
CREATE INDEX m_shadow_correlationCaseCloseTimestamp_idx ON m_shadow (correlationCaseCloseTimestamp);
$aa$);

-- SCHEMA-COMMIT 4.5: commit c5f19c9e

-- changes for 4.6

-- MID-7746
-- We add the new enum value in separate change, because it must be committed before it is used.
call apply_change(6, $aa$
CREATE TYPE AdministrativeAvailabilityStatusType AS ENUM ('MAINTENANCE', 'OPERATIONAL');
$aa$);

call apply_change(7, $aa$
ALTER TABLE m_resource
ADD COLUMN administrativeOperationalStateAdministrativeAvailabilityStatus AdministrativeAvailabilityStatusType;
$aa$);

-- smart correlation
call apply_change(8, $aa$
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; -- fuzzy string match (levenshtein, etc.)

ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'FOCUS_IDENTITY' AFTER 'CASE_WORK_ITEM';
$aa$);

call apply_change(9, $aa$
CREATE TABLE m_focus_identity (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('FOCUS_IDENTITY') STORED
        CHECK (containerType = 'FOCUS_IDENTITY'),
    fullObject BYTEA,
    sourceResourceRefTargetOid UUID,
    itemsOriginal JSONB,
    itemsNormalized JSONB,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_focus_identity_sourceResourceRefTargetOid_idx ON m_focus_identity (sourceResourceRefTargetOid);
CREATE INDEX m_focus_identity_itemsOriginal_idx ON m_focus_identity USING gin(itemsOriginal);
CREATE INDEX m_focus_identity_itemsNormalized_idx ON m_focus_identity USING gin(itemsNormalized);
$aa$);

-- resource templates
call apply_change(10, $aa$
ALTER TABLE m_resource ADD template BOOLEAN;
$aa$);

-- MID-8053: "Active" connectors detection
call apply_change(11, $aa$
ALTER TABLE m_connector ADD available BOOLEAN;
$aa$);

-- SCHEMA-COMMIT 4.6: commit TODO

-- WRITE CHANGES ABOVE ^^
-- IMPORTANT: update apply_change number at the end of postgres-new.sql
-- to match the number used in the last change here!
