/*
 * Copyright (C) 2010-2023 Evolveum and contributors
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

-- SCHEMA-COMMIT 4.4.1: commit de18c14f

-- changes for 4.5

-- MID-7484
call apply_audit_change(2, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MESSAGE_TEMPLATE' AFTER 'LOOKUP_TABLE';
$aa$);

-- SCHEMA-COMMIT 4.6: commit 71f2df50

-- changes for 4.7
-- Simulation related changes
call apply_audit_change(3, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SIMULATION_RESULT' AFTER 'SHADOW';
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MARK' AFTER 'LOOKUP_TABLE';
$aa$);

-- changes for 4.8
-- Shadow auditing
call apply_audit_change(4, $aa$
   ALTER TYPE AuditEventStageType ADD VALUE IF NOT EXISTS 'RESOURCE' AFTER 'EXECUTION';
   ALTER TYPE AuditEventTypeType ADD VALUE IF NOT EXISTS 'DISCOVER_OBJECT' AFTER 'RUN_TASK_IMMEDIATELY';
$aa$);

call apply_audit_change(5, $aa$
   CREATE TYPE EffectivePrivilegesModificationType AS ENUM ('ELEVATION', 'FULL_ELEVATION', 'REDUCTION', 'OTHER');
   ALTER TABLE ma_audit_event
     ADD COLUMN effectivePrincipalOid UUID,
     ADD COLUMN effectivePrincipalType ObjectType,
     ADD COLUMN effectivePrincipalName TEXT,
     ADD COLUMN effectivePrivilegesModification EffectivePrivilegesModificationType;
$aa$);


call apply_audit_change(6, $aa$
   -- We try to create ShadowKindType (necessary if audit is in separate database, if it is in same
   -- database as repository, type already exists.
   DO $$ BEGIN
       CREATE TYPE ShadowKindType AS ENUM ('ACCOUNT', 'ENTITLEMENT', 'GENERIC', 'UNKNOWN');
   EXCEPTION
       WHEN duplicate_object THEN null;
   END $$;

   ALTER TABLE ma_audit_delta
     ADD COLUMN shadowKind ShadowKindType,
     ADD COLUMN shadowIntent TEXT;
$aa$);

-- Role Mining

call apply_audit_change(7, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_CLUSTER' AFTER 'ROLE';
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_SESSION' AFTER 'ROLE_ANALYSIS_CLUSTER';
$aa$);

-- Informatoin Disclosure
call apply_audit_change(8, $aa$
ALTER TYPE AuditEventTypeType ADD VALUE IF NOT EXISTS 'INFORMATION_DISCLOSURE' AFTER 'DISCOVER_OBJECT';
$aa$);


--- Policy Type
call apply_audit_change(9, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'POLICY' AFTER 'ORG';
$aa$);

--- Schema Type
call apply_audit_change(10, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SCHEMA' AFTER 'ROLE_ANALYSIS_OUTLIER';
$aa$);

--- Application Type
call apply_audit_change(11, $aa$
    ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'APPLICATION' AFTER 'ACCESS_CERTIFICATION_DEFINITION';
$aa$);

--- Connector Development Type
call apply_audit_change(12, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'CONNECTOR_DEVELOPMENT' AFTER 'CONNECTOR';
$aa$);



-- WRITE CHANGES ABOVE ^^

-- IMPORTANT: update apply_audit_change number at the end of postgres-audit.sql
-- to match the number used in the last change here!
-- Also update SqaleUtils.CURRENT_SCHEMA_AUDIT_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
