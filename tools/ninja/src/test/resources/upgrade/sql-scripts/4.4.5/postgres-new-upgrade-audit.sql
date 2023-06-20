/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- This is the update script for the AUDIT database.
-- If you use audit and main repository in a single database, this still must be run as well.
-- It is safe to run this script repeatedly, so if you're not sure, just run it to be up to date.

-- Using psql is strongly recommended, don't use tools with messy autocommit behavior like pgAdmin!
-- Using flag to stop on first error is also recommended, for example:
-- psql -v ON_ERROR_STOP=1 -h localhost -U midaudit -W -d midaudit -f postgres-new-upgrade-audit.sql

-- SCHEMA-COMMIT is a commit which should be used to initialize the DB for testing changes below it.
-- Check out that commit and initialize a fresh DB with postgres-new-audit.sql to test upgrades.

DO $$
    BEGIN
        if to_regproc('apply_audit_change') is null then
            raise exception 'You are running AUDIT UPGRADE script, but the procedure ''apply_audit_change'' is missing.
Are you sure you are running this upgrade script on the correct database?
Current database name is ''%'', schema name is ''%''.
Perhaps you have separate audit database?', current_database(), current_schema();
        end if;
    END
$$;

-- SCHEMA-COMMIT 4.4: commit 69e8c29b

-- changes for 4.4.1

-- support for partition generation in the past using negative argument
call apply_audit_change(1, $aac$
-- Use negative futureCount for creating partitions for the past months if needed.
CREATE OR REPLACE PROCEDURE audit_create_monthly_partitions(futureCount int)
    LANGUAGE plpgsql
AS $$
DECLARE
    dateFrom TIMESTAMPTZ = date_trunc('month', current_timestamp);
    dateTo TIMESTAMPTZ;
    tableSuffix TEXT;
BEGIN
    -- noinspection SqlUnused
    FOR i IN 1..abs(futureCount) loop
        dateTo := dateFrom + interval '1 month';
        tableSuffix := to_char(dateFrom, 'YYYYMM');

        BEGIN
            -- PERFORM = select without using the result
            PERFORM ('ma_audit_event_' || tableSuffix)::regclass;
            RAISE NOTICE 'Tables for partition % already exist, OK...', tableSuffix;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Creating partitions for range: % - %', dateFrom, dateTo;

            -- values FROM are inclusive (>=), TO are exclusive (<)
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF ma_audit_event FOR VALUES FROM (%L) TO (%L);',
                    'ma_audit_event_' || tableSuffix, dateFrom, dateTo);
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF ma_audit_delta FOR VALUES FROM (%L) TO (%L);',
                    'ma_audit_delta_' || tableSuffix, dateFrom, dateTo);
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF ma_audit_ref FOR VALUES FROM (%L) TO (%L);',
                    'ma_audit_ref_' || tableSuffix, dateFrom, dateTo);

            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (recordId, timestamp)' ||
                    ' REFERENCES %I (id, timestamp) ON DELETE CASCADE',
                    'ma_audit_delta_' || tableSuffix,
                    'ma_audit_delta_' || tableSuffix || '_fk',
                    'ma_audit_event_' || tableSuffix);
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (recordId, timestamp)' ||
                    ' REFERENCES %I (id, timestamp) ON DELETE CASCADE',
                    'ma_audit_ref_' || tableSuffix,
                    'ma_audit_ref_' || tableSuffix || '_fk',
                    'ma_audit_event_' || tableSuffix);
        END;

        IF futureCount < 0 THEN
            -- going to the past
            dateFrom := dateFrom - interval '1 month';
        ELSE
            dateFrom := dateTo;
        END IF;

    END loop;
END $$;
$aac$);

-- WRITE CHANGES ABOVE ^^
-- IMPORTANT: update apply_audit_change number at the end of postgres-new-audit.sql
-- to match the number used in the last change here!
