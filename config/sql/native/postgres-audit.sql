/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

-- USAGE NOTES: You can apply this to the main repository schema.
-- For separate audit use this in a separate database.
-- See the docs here: https://docs.evolveum.com/midpoint/reference/repository/native-audit
--
-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- Naming conventions:
-- M_ prefix is used for tables in main part of the repo, MA_ for audit tables (can be separate)
-- Constraints/indexes use table_column(s)_suffix convention, with PK for primary key,
-- FK foreign key, IDX for index, KEY for unique index.
-- TR is suffix for triggers.
-- Names are generally lowercase (despite prefix/suffixes above in uppercase ;-)).
-- Column names are Java style and match attribute names from M-classes (e.g. MAuditEvent).
--
-- Other notes:
-- TEXT is used instead of VARCHAR, see: https://dba.stackexchange.com/a/21496/157622

-- noinspection SqlResolveForFile @ operator-class/"gin__int_ops"

-- public schema is not used as of now, everything is in the current user schema
-- https://www.postgresql.org/docs/15/ddl-schemas.html#DDL-SCHEMAS-PATTERNS
-- see secure schema usage pattern

-- just in case CURRENT_USER schema was dropped (fastest way to remove all midpoint objects)
-- drop schema current_user cascade;
CREATE SCHEMA IF NOT EXISTS AUTHORIZATION CURRENT_USER;

-- CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes

-- region custom enum types
DO $$ BEGIN
    -- NOTE: Types in this block must be updated when changed in postgres-new.sql!
    CREATE TYPE ObjectType AS ENUM (
        'ABSTRACT_ROLE',
        'ACCESS_CERTIFICATION_CAMPAIGN',
        'ACCESS_CERTIFICATION_DEFINITION',
        'ARCHETYPE',
        'ASSIGNMENT_HOLDER',
        'CASE',
        'CONNECTOR',
        'CONNECTOR_HOST',
        'DASHBOARD',
        'FOCUS',
        'FORM',
        'FUNCTION_LIBRARY',
        'GENERIC_OBJECT',
        'LOOKUP_TABLE',
        'MARK',
        'MESSAGE_TEMPLATE',
        'NODE',
        'OBJECT',
        'OBJECT_COLLECTION',
        'OBJECT_TEMPLATE',
        'ORG',
        'POLICY',
        'REPORT',
        'REPORT_DATA',
        'RESOURCE',
        'ROLE',
        'ROLE_ANALYSIS_CLUSTER',
        'ROLE_ANALYSIS_SESSION',
        'ROLE_ANALYSIS_OUTLIER',
        'SECURITY_POLICY',
        'SEQUENCE',
        'SERVICE',
        'SHADOW',
        'SIMULATION_RESULT',
        'SYSTEM_CONFIGURATION',
        'TASK',
        'USER',
        'VALUE_POLICY');

    CREATE TYPE OperationResultStatusType AS ENUM ('SUCCESS', 'WARNING', 'PARTIAL_ERROR',
        'FATAL_ERROR', 'HANDLED_ERROR', 'NOT_APPLICABLE', 'IN_PROGRESS', 'UNKNOWN');
EXCEPTION WHEN duplicate_object THEN raise notice 'Main repo custom types already exist, OK...'; END $$;

CREATE TYPE AuditEventTypeType AS ENUM ('GET_OBJECT', 'ADD_OBJECT', 'MODIFY_OBJECT',
    'DELETE_OBJECT', 'EXECUTE_CHANGES_RAW', 'SYNCHRONIZATION', 'CREATE_SESSION',
    'TERMINATE_SESSION', 'WORK_ITEM', 'WORKFLOW_PROCESS_INSTANCE', 'RECONCILIATION',
    'SUSPEND_TASK', 'RESUME_TASK', 'RUN_TASK_IMMEDIATELY', 'DISCOVER_OBJECT', 'INFORMATION_DISCLOSURE');

CREATE TYPE AuditEventStageType AS ENUM ('REQUEST', 'EXECUTION', 'RESOURCE');

CREATE TYPE EffectivePrivilegesModificationType AS ENUM ('ELEVATION', 'FULL_ELEVATION', 'REDUCTION', 'OTHER');

CREATE TYPE ChangeType AS ENUM ('ADD', 'MODIFY', 'DELETE');



   -- We try to create ShadowKindType (necessary if audit is in separate database, if it is in same
   -- database as repository, type already exists.
DO $$ BEGIN
       CREATE TYPE ShadowKindType AS ENUM ('ACCOUNT', 'ENTITLEMENT', 'GENERIC', 'UNKNOWN');
   EXCEPTION
       WHEN duplicate_object THEN null;
END $$;
-- endregion

-- region management tables
-- Key -> value config table for internal use.
CREATE TABLE IF NOT EXISTS m_global_metadata (
    name TEXT PRIMARY KEY,
    value TEXT
);
-- endregion

-- region AUDIT
CREATE TABLE ma_audit_event (
    -- ID is generated as unique, but if provided, it is checked for uniqueness
    -- only in combination with timestamp because of partitioning.
    id BIGSERIAL NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    eventIdentifier TEXT,
    eventType AuditEventTypeType,
    eventStage AuditEventStageType,
    sessionIdentifier TEXT,
    requestIdentifier TEXT,
    taskIdentifier TEXT,
    taskOid UUID,
    hostIdentifier TEXT,
    nodeIdentifier TEXT,
    remoteHostAddress TEXT,
    initiatorOid UUID,
    initiatorType ObjectType,
    initiatorName TEXT,
    attorneyOid UUID,
    attorneyName TEXT,
    effectivePrincipalOid UUID,
    effectivePrincipalType ObjectType,
    effectivePrincipalName TEXT,
    effectivePrivilegesModification EffectivePrivilegesModificationType,
    targetOid UUID,
    targetType ObjectType,
    targetName TEXT,
    targetOwnerOid UUID,
    targetOwnerType ObjectType,
    targetOwnerName TEXT,
    channel TEXT, -- full URI, we do not want m_uri ID anymore
    outcome OperationResultStatusType,
    parameter TEXT,
    result TEXT,
    message TEXT,
    changedItemPaths TEXT[],
    resourceOids TEXT[],
    properties JSONB,
    -- ext JSONB, -- TODO extension container later

    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE INDEX ma_audit_event_timestamp_idx ON ma_audit_event (timestamp);
CREATE INDEX ma_audit_event_eventIdentifier_idx ON ma_audit_event (eventIdentifier);
CREATE INDEX ma_audit_event_sessionIdentifier_idx ON ma_audit_event (sessionIdentifier);
CREATE INDEX ma_audit_event_requestIdentifier_idx ON ma_audit_event (requestIdentifier);
-- This was originally eventStage + targetOid, but low variability eventStage can do more harm.
CREATE INDEX ma_audit_event_targetOid_idx ON ma_audit_event (targetOid);
-- TODO do we want to index every single column or leave the rest to full/partial scans?
-- Original repo/audit didn't have any more indexes either...
CREATE INDEX ma_audit_event_changedItemPaths_idx ON ma_audit_event USING gin(changeditempaths);
CREATE INDEX ma_audit_event_resourceOids_idx ON ma_audit_event USING gin(resourceOids);
CREATE INDEX ma_audit_event_properties_idx ON ma_audit_event USING gin(properties);
-- TODO trigram indexes for LIKE support? What columns? message, ...

CREATE TABLE ma_audit_delta (
    recordId BIGINT NOT NULL, -- references ma_audit_event.id
    timestamp TIMESTAMPTZ NOT NULL, -- references ma_audit_event.timestamp
    checksum TEXT NOT NULL,
    delta BYTEA,
    deltaOid UUID,
    deltaType ChangeType,
    fullResult BYTEA,
    objectNameNorm TEXT,
    objectNameOrig TEXT,
    resourceOid UUID,
    resourceNameNorm TEXT,
    resourceNameOrig TEXT,
    shadowKind ShadowKindType,
    shadowIntent TEXT,
    status OperationResultStatusType,

    PRIMARY KEY (recordId, timestamp, checksum)
) PARTITION BY RANGE (timestamp);

/* Similar FK is created PER PARTITION only, see audit_create_monthly_partitions
   or *_default tables:
ALTER TABLE ma_audit_delta ADD CONSTRAINT ma_audit_delta_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event (id, timestamp)
        ON DELETE CASCADE;

-- Primary key covers the need for FK(recordId, timestamp) as well, no need for explicit index.
*/

-- TODO: any unique combination within single recordId? name+oid+type perhaps?
CREATE TABLE ma_audit_ref (
    id BIGSERIAL NOT NULL, -- unique technical PK
    recordId BIGINT NOT NULL, -- references ma_audit_event.id
    timestamp TIMESTAMPTZ NOT NULL, -- references ma_audit_event.timestamp
    name TEXT, -- multiple refs can have the same name, conceptually it's a Map(name -> refs[])
    targetOid UUID,
    targetType ObjectType,
    targetNameOrig TEXT,
    targetNameNorm TEXT,

    PRIMARY KEY (id, timestamp) -- real PK must contain partition key (timestamp)
) PARTITION BY RANGE (timestamp);

/* Similar FK is created PER PARTITION only:
ALTER TABLE ma_audit_ref ADD CONSTRAINT ma_audit_ref_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event (id, timestamp)
        ON DELETE CASCADE;
*/
-- Index for FK mentioned above.
-- Index can be declared for partitioned table and will be partitioned automatically.
CREATE INDEX ma_audit_ref_recordId_timestamp_idx ON ma_audit_ref (recordId, timestamp);

-- Default tables used when no timestamp range partitions are created:
CREATE TABLE ma_audit_event_default PARTITION OF ma_audit_event DEFAULT;
CREATE TABLE ma_audit_delta_default PARTITION OF ma_audit_delta DEFAULT;
CREATE TABLE ma_audit_ref_default PARTITION OF ma_audit_ref DEFAULT;

/*
For info about what is and is not automatically created on the partition, see:
https://www.postgresql.org/docs/13/sql-createtable.html (search for "PARTITION OF parent_table")
In short, for our case PK and constraints are created automatically, but FK are not.
*/
ALTER TABLE ma_audit_delta_default ADD CONSTRAINT ma_audit_delta_default_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event_default (id, timestamp)
        ON DELETE CASCADE;
ALTER TABLE ma_audit_ref_default ADD CONSTRAINT ma_audit_ref_default_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event_default (id, timestamp)
        ON DELETE CASCADE;
-- endregion

-- region Schema versioning and upgrading
/*
Procedure applying a DB schema/data change for audit tables.
Use sequential change numbers to identify the changes.
This protects re-execution of the same change on the same database instance.
Use dollar-quoted string constant for a change, examples are lower, docs here:
https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-DOLLAR-QUOTING
The transaction is committed if the change is executed.
The change number is NOT semantic and uses different key than original 'databaseSchemaVersion'.
Semantic schema versioning is still possible, but now only for information purposes.

Example of an DB upgrade script (stuff between $$ can be multiline, here compressed for brevity):
CALL apply_audit_change(1, $$ create table x(a int); insert into x values (1); $$);
CALL apply_audit_change(2, $$ alter table x add column b text; insert into x values (2, 'two'); $$);
-- not a good idea in general, but "true" forces the execution; it never updates change # to lower
CALL apply_audit_change(1, $$ insert into x values (3, 'three'); $$, true);
*/
CREATE OR REPLACE PROCEDURE apply_audit_change(changeNumber int, change TEXT, force boolean = false)
    LANGUAGE plpgsql
AS $$
DECLARE
    lastChange int;
BEGIN
    SELECT value INTO lastChange FROM m_global_metadata WHERE name = 'schemaAuditChangeNumber';

    -- change is executed if the changeNumber is newer - or if forced
    IF lastChange IS NULL OR lastChange < changeNumber OR force THEN
        EXECUTE change;
        RAISE NOTICE 'Audit change #% executed!', changeNumber;

        IF lastChange IS NULL THEN
            INSERT INTO m_global_metadata (name, value) VALUES ('schemaAuditChangeNumber', changeNumber);
        ELSIF changeNumber > lastChange THEN
            -- even with force we never want to set lower change number, hence the IF above
            UPDATE m_global_metadata SET value = changeNumber WHERE name = 'schemaAuditChangeNumber';
        END IF;
        COMMIT;
    ELSE
        RAISE NOTICE 'Audit change #% skipped - not newer than the last change #%!', changeNumber, lastChange;
    END IF;
END $$;
-- endregion

-- https://www.postgresql.org/docs/current/runtime-config-query.html#GUC-ENABLE-PARTITIONWISE-JOIN
DO $$ BEGIN
    EXECUTE 'ALTER DATABASE "' || current_database() || '" SET enable_partitionwise_join TO on';
END; $$;

-- region partition creation procedures
-- Use negative futureCount for creating partitions for the past months if needed.
-- See also the comment below the procedure for more details.
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

/*
For info about what is and is not automatically created on the partition, see:
https://www.postgresql.org/docs/13/sql-createtable.html (search for "PARTITION OF parent_table")
In short, for our case PK and constraints are created automatically, but FK are not.
*/
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
-- endregion

/*
IMPORTANT: Only default partitions are created in this script!
Consider, whether you need partitioning before doing anything, for more read the docs:
https://docs.evolveum.com/midpoint/reference/repository/native-audit/#partitioning

Use something like this, if you desire monthly partitioning:
call audit_create_monthly_partitions(120);

This creates 120 monthly partitions into the future (10 years).
It can be safely called multiple times, so you can run it again anytime in the future.
If you forget to run, audit events will go to default partition so no data is lost,
however it may be complicated to organize it into proper partitions after the fact.

Create past partitions if needed, e.g. for migration. E.g., for last 12 months (including current):
call audit_create_monthly_partitions(-12);

Check the existing partitions with this SQL query:
select inhrelid::regclass as partition
from pg_inherits
where inhparent = 'ma_audit_event'::regclass;

Try this to see recent audit events with the real table where they are stored:
select tableoid::regclass::text AS table_name, *
from ma_audit_event
order by id desc
limit 50;
*/

-- Initializing the last change number used in postgres-new-upgrade.sql.
-- This is important to avoid applying any change more than once.
-- Also update SqaleUtils.CURRENT_SCHEMA_AUDIT_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
call apply_audit_change(9, $$ SELECT 1 $$, true);
