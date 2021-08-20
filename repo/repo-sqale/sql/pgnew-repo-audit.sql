-- Copyright (C) 2010-2021 Evolveum and contributors
--
-- This work is dual-licensed under the Apache License 2.0
-- and European Union Public License. See LICENSE file for details.
--
-- USAGE NOTES: You can apply this to the main repository schema.
-- For separate audit use this in a separate database.
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

-- just in case PUBLIC schema was dropped (fastest way to remove all midpoint objects)
-- drop schema public cascade;
CREATE SCHEMA IF NOT EXISTS public;
--CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes TODO for ext with LIKE and fulltext

-- region custom enum types
DO $$ BEGIN
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
        'NODE',
        'OBJECT',
        'OBJECT_COLLECTION',
        'OBJECT_TEMPLATE',
        'ORG',
        'REPORT',
        'REPORT_DATA',
        'RESOURCE',
        'ROLE',
        'SECURITY_POLICY',
        'SEQUENCE',
        'SERVICE',
        'SHADOW',
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
    'SUSPEND_TASK', 'RESUME_TASK', 'RUN_TASK_IMMEDIATELY');

CREATE TYPE AuditEventStageType AS ENUM ('REQUEST', 'EXECUTION');

CREATE TYPE ChangeType AS ENUM ('ADD', 'MODIFY', 'DELETE');
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
    resourceOids UUID[],
    properties JSONB,
    customColumnProperties JSONB,

    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

/*
-- changed to properties JSONB, values are all considered multivalue and stored in []
CREATE TABLE m_audit_prop_value (
  id        BIGSERIAL NOT NULL,
  record_id BIGINT,
  name      TEXT,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);
*/

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
    resourceNameNorm TEXT,
    resourceNameOrig TEXT,
    resourceOid UUID,
    status OperationResultStatusType,

    PRIMARY KEY (recordId, timestamp, checksum)
) PARTITION BY RANGE (timestamp);

/* Similar FK is created PER PARTITION only
ALTER TABLE ma_audit_delta ADD CONSTRAINT ma_audit_delta_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event (id, timestamp)
        ON DELETE CASCADE;
*/
CREATE INDEX ma_audit_delta_recordId_timestamp_idx ON ma_audit_delta (recordId, timestamp);

-- TODO: any unique combination within single recordId? name+oid+type perhaps?
CREATE TABLE ma_audit_ref (
    id BIGSERIAL NOT NULL,
    recordId BIGINT NOT NULL, -- references ma_audit_event.id
    timestamp TIMESTAMPTZ NOT NULL, -- references ma_audit_event.timestamp
    name TEXT, -- multiple refs can have the same name, conceptually it's a Map(name -> refs[])
    oid UUID,
    targetNameOrig TEXT,
    targetNameNorm TEXT,
    targetType ObjectType,

    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

/* Similar FK is created PER PARTITION only
ALTER TABLE ma_audit_ref ADD CONSTRAINT ma_audit_ref_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event (id, timestamp)
        ON DELETE CASCADE;
*/
CREATE INDEX ma_audit_ref_recordId_timestamp_idx ON ma_audit_ref (recordId, timestamp);

/* TODO audit indexes
CREATE INDEX iAuditDeltaRecordId
  ON m_audit_delta (record_id);
CREATE INDEX iTimestampValue
  ON m_audit_event (timestampValue);
CREATE INDEX iAuditEventRecordEStageTOid
  ON m_audit_event (eventStage, targetOid);
CREATE INDEX iChangedItemPath
  ON m_audit_item (changedItemPath);
CREATE INDEX iAuditItemRecordId
  ON m_audit_item (record_id);
CREATE INDEX iAuditPropValRecordId
  ON m_audit_prop_value (record_id);
CREATE INDEX iAuditRefValRecordId
  ON m_audit_ref_value (record_id);
CREATE INDEX iAuditResourceOid
  ON m_audit_resource (resourceOid);
CREATE INDEX iAuditResourceOidRecordId
  ON m_audit_resource (record_id);

ALTER TABLE m_audit_delta
  ADD CONSTRAINT fk_audit_delta FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_item
  ADD CONSTRAINT fk_audit_item FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;
*/

-- Default tables used when no timestamp range partitions are created:
CREATE TABLE ma_audit_event_default PARTITION OF ma_audit_event DEFAULT;
CREATE TABLE ma_audit_delta_default PARTITION OF ma_audit_delta DEFAULT;
CREATE TABLE ma_audit_ref_default PARTITION OF ma_audit_ref DEFAULT;

ALTER TABLE ma_audit_delta_default ADD CONSTRAINT ma_audit_delta_default_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event_default (id, timestamp)
        ON DELETE CASCADE;
ALTER TABLE ma_audit_ref_default ADD CONSTRAINT ma_audit_ref_default_fk
    FOREIGN KEY (recordId, timestamp) REFERENCES ma_audit_event_default (id, timestamp)
        ON DELETE CASCADE;
-- endregion

-- region partition creation procedures
CREATE OR REPLACE PROCEDURE audit_create_monthly_partitions(futureCount int)
    LANGUAGE plpgsql
AS $$
DECLARE
    dateFrom TIMESTAMPTZ = date_trunc('month', current_timestamp);
    dateTo TIMESTAMPTZ;
    tableSuffix TEXT;
BEGIN
    FOR i IN 1..futureCount loop
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

        dateFrom := dateTo;
    END loop;
END $$;
-- endregion

-- region Schema versioning and upgrading
/*
See notes at the end of main repo schema.
This is necessary only when audit is separate, but is safe to run any time.
*/
CREATE OR REPLACE PROCEDURE apply_change(changeNumber int, change TEXT, force boolean = false)
    LANGUAGE plpgsql
AS $$
DECLARE
    lastChange int;
BEGIN
    SELECT value INTO lastChange FROM m_global_metadata WHERE name = 'schemaChangeNumber';

    -- change is executed if the changeNumber is newer - or if forced
    IF lastChange IS NULL OR lastChange < changeNumber OR force THEN
        EXECUTE change;
        RAISE NOTICE 'Change #% executed!', changeNumber;

        IF lastChange IS NULL THEN
            INSERT INTO m_global_metadata (name, value) VALUES ('schemaChangeNumber', changeNumber);
        ELSIF changeNumber > lastChange THEN
            -- even with force we never want to set lower change number, hence the IF above
            UPDATE m_global_metadata SET value = changeNumber WHERE name = 'schemaChangeNumber';
        END IF;
        COMMIT;
    ELSE
        RAISE NOTICE 'Change #% skipped, last change #% is newer!', changeNumber, lastChange;
    END IF;
END $$;
-- endregion

-- For Quartz tables see:
-- repo/task-quartz-impl/src/main/resources/com/evolveum/midpoint/task/quartzimpl/execution/tables_postgres.sql

-- region Experiments TODO remove when finished

-- Question about partitioning strategy for multiple tables: https://stackoverflow.com/q/68868322/658826

do $$
declare
    event_id bigint;
    ts timestamptz;
begin
    for i in 1000001..2000000 loop
        select current_timestamp + interval '3s' * i into ts;
        insert into ma_audit_event (timestamp, eventIdentifier)
            -- current value of serial: https://dba.stackexchange.com/a/3284/157622
        values (ts, currval(pg_get_serial_sequence('ma_audit_event', 'id')))
        returning id into event_id
        ;

        insert into ma_audit_delta (recordid, timestamp, checksum, delta)
        values (event_id, ts, 'cs1', random_bytea(100, 1000))
        ;
        insert into ma_audit_delta (recordid, timestamp, checksum, delta)
        values (event_id, ts, 'cs2', random_bytea(100, 1000))
        ;

        insert into ma_audit_ref (recordid, timestamp, name)
        values (event_id, ts, 'some-ref')
        ;
        insert into ma_audit_ref (recordid, timestamp, name)
        values (event_id, ts, 'some-ref')
        ;
        insert into ma_audit_ref (recordid, timestamp, name)
        values (event_id, ts, 'some-ref2')
        ;

    end loop;
end $$;

select count(*) from ma_audit_event;
select tableoid::regclass::text AS table_name, * from ma_audit_event order by id desc;

call audit_create_monthly_partitions(10)
;

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
select *
from ma_audit_event ae
         join ma_audit_delta ad on ae.id = ad.recordid
where ae.timestamp >= '2021-10-10' and ae.timestamp < '2021-10-20'
        and ad.timestamp >= '2021-10-10' and ad.timestamp < '2021-10-20'
-- and ... other ad. condition as necessary

-- endregion