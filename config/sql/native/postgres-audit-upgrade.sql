--
-- Copyright (C) 2010-2023 Evolveum and contributors
--
-- Licensed under the EUPL-1.2 or later.
--

-- Developer documentation for SQL documentation annotations:
-- https://docs.evolveum.com/midpoint/devel/guides/sql-script-annotations/

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
-- @change: Updates the audit monthly partition creation procedure to support creating partitions in the past using a negative argument.
-- @since: 4.4.1
-- @affects: routine audit_create_monthly_partitions | Modified procedure | Supports creating audit partitions in past months using a negative argument.
-- @affects: table ma_audit_event_<month> | New generated partition | Creates monthly audit event partitions.
-- @affects: table ma_audit_delta_<month> | New generated partition | Creates monthly audit delta partitions.
-- @affects: table ma_audit_ref_<month> | New generated partition | Creates monthly audit reference partitions.
-- @affects: constraint ma_audit_delta_<month>_fk | New generated foreign key | Links monthly audit delta partitions to matching audit event partitions.
-- @affects: constraint ma_audit_ref_<month>_fk | New generated foreign key | Links monthly audit reference partitions to matching audit event partitions.
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
-- @change: Adds `MESSAGE_TEMPLATE` to audit object type values.
-- @since: 4.5
-- @affects: enum ObjectType | Modified enum type | Adds `MESSAGE_TEMPLATE`.
call apply_audit_change(2, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MESSAGE_TEMPLATE' AFTER 'LOOKUP_TABLE';
$aa$);

-- SCHEMA-COMMIT 4.6: commit 71f2df50

-- changes for 4.7
-- Simulation related changes
-- @change: Adds `SIMULATION_RESULT` and `MARK` object type values used by audit records.
-- @since: 4.7
-- @affects: enum ObjectType | Modified enum type | Adds `SIMULATION_RESULT` and `MARK`.
call apply_audit_change(3, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SIMULATION_RESULT' AFTER 'SHADOW';
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MARK' AFTER 'LOOKUP_TABLE';
$aa$);

-- changes for 4.8
-- Shadow auditing
-- @change: Adds `RESOURCE` audit stage and `DISCOVER_OBJECT` audit event type value.
-- @since: 4.8
-- @affects: enum AuditEventStageType | Modified enum type | Adds `RESOURCE` audit stage.
-- @affects: enum AuditEventTypeType | Modified enum type | Adds `DISCOVER_OBJECT` audit event type.
call apply_audit_change(4, $aa$
   ALTER TYPE AuditEventStageType ADD VALUE IF NOT EXISTS 'RESOURCE' AFTER 'EXECUTION';
   ALTER TYPE AuditEventTypeType ADD VALUE IF NOT EXISTS 'DISCOVER_OBJECT' AFTER 'RUN_TASK_IMMEDIATELY';
$aa$);

-- @change: Adds effective-principal audit columns and privilege modification type.
-- @since: 4.8
-- @affects: enum EffectivePrivilegesModificationType | New enum type | Stores effective-principal privilege modification values.
-- @affects: table ma_audit_event | Modified table | Adds effective-principal columns and privilege modification column.
call apply_audit_change(5, $aa$
   CREATE TYPE EffectivePrivilegesModificationType AS ENUM ('ELEVATION', 'FULL_ELEVATION', 'REDUCTION', 'OTHER');
   ALTER TABLE ma_audit_event
     ADD COLUMN effectivePrincipalOid UUID,
     ADD COLUMN effectivePrincipalType ObjectType,
     ADD COLUMN effectivePrincipalName TEXT,
     ADD COLUMN effectivePrivilegesModification EffectivePrivilegesModificationType;
$aa$);

-- @change: Adds shadow kind and intent audit delta columns.
-- @since: 4.8
-- @affects: enum ShadowKindType | New enum type | Adds shadow kind values when audit is installed separately.
-- @affects: table ma_audit_delta | Modified table | Adds shadowKind and shadowIntent columns.
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

-- @change: Adds role analysis cluster and role analysis session object type values.
-- @since: 4.8
-- @affects: enum ObjectType | Modified enum type | Adds `ROLE_ANALYSIS_CLUSTER` and `ROLE_ANALYSIS_SESSION`.
call apply_audit_change(7, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_CLUSTER' AFTER 'ROLE';
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_SESSION' AFTER 'ROLE_ANALYSIS_CLUSTER';
$aa$);

-- Information Disclosure

-- @change: Adds information disclosure audit event type value.
-- @since: 4.8
-- @affects: enum AuditEventTypeType | Modified enum type | Adds `INFORMATION_DISCLOSURE` audit event type.
call apply_audit_change(8, $aa$
ALTER TYPE AuditEventTypeType ADD VALUE IF NOT EXISTS 'INFORMATION_DISCLOSURE' AFTER 'DISCOVER_OBJECT';
$aa$);

-- Policy Type
-- @change: Adds policy object type value.
-- @since: 4.9
-- @affects: enum ObjectType | Modified enum type | Adds `POLICY`.
call apply_audit_change(9, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'POLICY' AFTER 'ORG';
$aa$);

-- Compatibility fix for standalone audit databases upgraded through older scripts.
-- `ROLE_ANALYSIS_OUTLIER` was present in postgres-audit.sql since 4.9,
-- but was missing from postgres-audit-upgrade.sql.
DO $$ BEGIN
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_OUTLIER' AFTER 'ROLE_ANALYSIS_SESSION';
END $$;

-- Schema Type
-- @change: Adds schema object type value.
-- @since: 4.10
-- @affects: enum ObjectType | Modified enum type | Adds `SCHEMA`.
call apply_audit_change(10, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SCHEMA' AFTER 'ROLE_ANALYSIS_OUTLIER';
$aa$);

-- Application Type
-- @change: Adds application object type value.
-- @since: 4.11
-- @affects: enum ObjectType | Modified enum type | Adds `APPLICATION`.
call apply_audit_change(11, $aa$
    ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'APPLICATION' AFTER 'ACCESS_CERTIFICATION_DEFINITION';
$aa$);

-- Connector Development Type
-- @change: Adds `CONNECTOR_DEVELOPMENT` object type value.
-- @since: 4.11
-- @affects: enum ObjectType | Modified enum type | Adds `CONNECTOR_DEVELOPMENT`.
call apply_audit_change(12, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'CONNECTOR_DEVELOPMENT' AFTER 'CONNECTOR';
$aa$);

-- Projection Holder Type
-- @change: Adds `PROJECTION_HOLDER` object type value.
-- @since: 4.11
-- @affects: enum ObjectType | Modified enum type | Adds `PROJECTION_HOLDER`.
call apply_audit_change(13, $aa$
    ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'PROJECTION_HOLDER' AFTER 'POLICY';
$aa$);

-- @change: Adds `WORK` shadow kind value.
-- @since: 4.11
-- @affects: enum ShadowKindType | Modified enum type | Adds `WORK` shadow kind value.
call apply_audit_change(14, $aa$
    ALTER TYPE ShadowKindType ADD VALUE IF NOT EXISTS 'WORK' AFTER 'GENERIC';
$aa$);

-- Audit payloads
-- @change: Adds generic audit payload persistence.
-- @since: 4.11
-- @affects: enum AuditEventTypeType | Modified enum type | Adds `EXTERNAL_SERVICE_CALL`.
-- @affects: table ma_audit_payload | New table | Stores payloads attached to audit events.
-- @affects: table ma_audit_payload_default | New default partition | Stores payloads outside monthly partitions.
-- @affects: table ma_audit_payload_<month> | New generated partition | Creates monthly audit payload partitions.
-- @affects: constraint ma_audit_payload_<month>_fk | New generated foreign key | Links monthly audit payload partitions to matching audit event partitions.
-- @affects: index ma_audit_payload_searchableText_idx | New index | Supports full-text-like payload searches.
-- @affects: routine audit_create_monthly_partitions | Modified procedure | Creates payload partitions and FKs.
call apply_audit_change(15, $aa$
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes

ALTER TYPE AuditEventTypeType ADD VALUE IF NOT EXISTS 'EXTERNAL_SERVICE_CALL' AFTER 'INFORMATION_DISCLOSURE';

CREATE TABLE ma_audit_payload (
    recordId BIGINT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    ordinal INTEGER NOT NULL,
    name TEXT NOT NULL,
    contentType TEXT,
    content JSONB,
    searchableText TEXT,

    PRIMARY KEY (recordId, timestamp, ordinal)
) PARTITION BY RANGE (timestamp);

CREATE INDEX ma_audit_payload_searchableText_idx ON ma_audit_payload USING gin(searchableText gin_trgm_ops);

CREATE TABLE ma_audit_payload_default PARTITION OF ma_audit_payload DEFAULT;

ALTER TABLE ma_audit_payload_default ADD CONSTRAINT ma_audit_payload_default_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event_default (id, timestamp)
    ON DELETE CASCADE;

DO $$
DECLARE
    eventPartitionName TEXT;
    tableSuffix TEXT;
    dateFrom TIMESTAMPTZ;
    dateTo TIMESTAMPTZ;
BEGIN
    FOR eventPartitionName IN
        SELECT c.relname
        FROM pg_inherits i
                 JOIN pg_class c ON c.oid = i.inhrelid
        WHERE i.inhparent = 'ma_audit_event'::regclass
          AND c.relname ~ '^ma_audit_event_[0-9]{6}$'
    LOOP
        tableSuffix := substring(eventPartitionName from '([0-9]{6})$');

        BEGIN
            PERFORM ('ma_audit_payload_' || tableSuffix)::regclass;
            RAISE NOTICE 'Audit payload partition % already exists, OK...', tableSuffix;
        EXCEPTION WHEN OTHERS THEN
            dateFrom := to_timestamp(tableSuffix, 'YYYYMM');
            dateTo := dateFrom + interval '1 month';

            EXECUTE format(
                'CREATE TABLE %I PARTITION OF ma_audit_payload FOR VALUES FROM (%L) TO (%L);',
                'ma_audit_payload_' || tableSuffix, dateFrom, dateTo);

            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (recordId, timestamp)' ||
                ' REFERENCES %I (id, timestamp) ON DELETE CASCADE',
                'ma_audit_payload_' || tableSuffix,
                'ma_audit_payload_' || tableSuffix || '_fk',
                'ma_audit_event_' || tableSuffix);
        END;
    END LOOP;
END $$;

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
                'CREATE TABLE %I PARTITION OF ma_audit_payload FOR VALUES FROM (%L) TO (%L);',
                'ma_audit_payload_' || tableSuffix, dateFrom, dateTo);

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
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (recordId, timestamp)' ||
                ' REFERENCES %I (id, timestamp) ON DELETE CASCADE',
                'ma_audit_payload_' || tableSuffix,
                'ma_audit_payload_' || tableSuffix || '_fk',
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
$aa$);


-- WRITE CHANGES ABOVE ^^

-- IMPORTANT: update apply_audit_change number at the end of postgres-audit.sql
-- to match the number used in the last change here!
-- Also update SqaleUtils.CURRENT_SCHEMA_AUDIT_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
