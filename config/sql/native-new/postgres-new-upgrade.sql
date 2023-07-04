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

-- SCHEMA-COMMIT 4.4: commit 69e8c29b

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

-- WRITE CHANGES ABOVE ^^
-- IMPORTANT: update apply_change number at the end of postgres-new.sql
-- to match the number used in the last change here!
-- Also update SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
