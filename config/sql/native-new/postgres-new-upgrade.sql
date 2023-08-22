/*
 * Copyright (C) 2010-2023 Evolveum and contributors
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

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_focus_identity_sourceResourceRefTargetOid_idx ON m_focus_identity (sourceResourceRefTargetOid);

ALTER TABLE m_focus ADD normalizedData JSONB;
CREATE INDEX m_focus_normalizedData_idx ON m_focus USING gin(normalizedData);
$aa$);

-- resource templates
call apply_change(10, $aa$
ALTER TABLE m_resource ADD template BOOLEAN;
$aa$);

-- MID-8053: "Active" connectors detection
call apply_change(11, $aa$
ALTER TABLE m_connector ADD available BOOLEAN;
$aa$);

-- SCHEMA-COMMIT 4.5: commit c5f19c9e

-- No changes for audit schema in 4.6
-- SCHEMA-COMMIT 4.6: commit 71f2df50

-- changes for 4.7

-- Simulations, enum type changes
call apply_change(12, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'MARK' AFTER 'LOOKUP_TABLE';
ALTER TYPE ReferenceType ADD VALUE IF NOT EXISTS 'PROCESSED_OBJECT_EVENT_MARK' AFTER 'PERSONA';
ALTER TYPE ReferenceType ADD VALUE IF NOT EXISTS 'OBJECT_EFFECTIVE_MARK' AFTER 'OBJECT_CREATE_APPROVER';
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SIMULATION_RESULT' AFTER 'SHADOW';
ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'SIMULATION_RESULT_PROCESSED_OBJECT' AFTER 'OPERATION_EXECUTION';
$aa$);

-- Simulations, tables
call apply_change(13, $aa$
CREATE TABLE m_simulation_result (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SIMULATION_RESULT') STORED
        CHECK (objectType = 'SIMULATION_RESULT'),
    partitioned boolean,
    rootTaskRefTargetOid UUID,
    rootTaskRefTargetType ObjectType,
    rootTaskRefRelationId INTEGER REFERENCES m_uri(id),
    startTimestamp TIMESTAMPTZ,
    endTimestamp TIMESTAMPTZ
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_simulation_result_oid_insert_tr BEFORE INSERT ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_simulation_result_update_tr BEFORE UPDATE ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_simulation_result_oid_delete_tr AFTER DELETE ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE TYPE ObjectProcessingStateType AS ENUM ('UNMODIFIED', 'ADDED', 'MODIFIED', 'DELETED');

CREATE TABLE m_simulation_result_processed_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    -- Owner does not have to be the direct parent of the container.
    -- use like this on the concrete table:
    -- ownerOid UUID NOT NULL REFERENCES m_object_oid(oid),
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    cid BIGINT NOT NULL,
    containerType ContainerType GENERATED ALWAYS AS ('SIMULATION_RESULT_PROCESSED_OBJECT') STORED
        CHECK (containerType = 'SIMULATION_RESULT_PROCESSED_OBJECT'),
    oid UUID,
    objectType ObjectType,
    nameOrig TEXT,
    nameNorm TEXT,
    state ObjectProcessingStateType,
    metricIdentifiers TEXT[],
    fullObject BYTEA,
    objectBefore BYTEA,
    objectAfter BYTEA,
    transactionId TEXT,
    focusRecordId BIGINT,

    PRIMARY KEY (ownerOid, cid)
) PARTITION BY LIST(ownerOid);

CREATE TABLE m_simulation_result_processed_object_default PARTITION OF m_simulation_result_processed_object DEFAULT;

CREATE OR REPLACE FUNCTION m_simulation_result_create_partition() RETURNS trigger AS
  $BODY$
    DECLARE
      partition TEXT;
    BEGIN
      partition := 'm_sr_processed_object_' || REPLACE(new.oid::text,'-','_');
      IF new.partitioned AND NOT EXISTS(SELECT relname FROM pg_class WHERE relname=partition) THEN
        RAISE NOTICE 'A partition has been created %',partition;
        EXECUTE 'CREATE TABLE ' || partition || ' partition of ' || 'm_simulation_result_processed_object' || ' for values in (''' || new.oid|| ''');';
      END IF;
      RETURN NULL;
    END;
  $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER m_simulation_result_create_partition AFTER INSERT ON m_simulation_result
 FOR EACH ROW EXECUTE FUNCTION m_simulation_result_create_partition();

--- Trigger which deletes processed objects partition when whole simulation is deleted

CREATE OR REPLACE FUNCTION m_simulation_result_delete_partition() RETURNS trigger AS
  $BODY$
    DECLARE
      partition TEXT;
    BEGIN
      partition := 'm_sr_processed_object_' || REPLACE(OLD.oid::text,'-','_');
      IF OLD.partitioned AND EXISTS(SELECT relname FROM pg_class WHERE relname=partition) THEN
        RAISE NOTICE 'A partition has been deleted %',partition;
        EXECUTE 'DROP TABLE IF EXISTS ' || partition || ';';
      END IF;
      RETURN OLD;
    END;
  $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER m_simulation_result_delete_partition BEFORE DELETE ON m_simulation_result
  FOR EACH ROW EXECUTE FUNCTION m_simulation_result_delete_partition();

CREATE TABLE m_processed_object_event_mark (
  ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
  ownerType ObjectType, -- GENERATED ALWAYS AS ('SIMULATION_RESULT') STORED,
  processedObjectCid INTEGER NOT NULL,
  referenceType ReferenceType GENERATED ALWAYS AS ('PROCESSED_OBJECT_EVENT_MARK') STORED,
  targetOid UUID NOT NULL, -- soft-references m_object
  targetType ObjectType NOT NULL,
  relationId INTEGER NOT NULL REFERENCES m_uri(id)

) PARTITION BY LIST(ownerOid);

CREATE TABLE m_processed_object_event_mark_default PARTITION OF m_processed_object_event_mark DEFAULT;

CREATE TABLE m_mark (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('MARK') STORED
        CHECK (objectType = 'MARK')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_mark_oid_insert_tr BEFORE INSERT ON m_mark
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_mark_update_tr BEFORE UPDATE ON m_mark
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_mark_oid_delete_tr AFTER DELETE ON m_mark
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- stores ObjectType/effectiveMarkRef
CREATE TABLE m_ref_object_effective_mark (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_EFFECTIVE_MARK') STORED
        CHECK (referenceType = 'OBJECT_EFFECTIVE_MARK'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_effective_mark_targetOidRelationId_idx
    ON m_ref_object_effective_mark (targetOid, relationId);
$aa$);

-- Minor index name fixes
call apply_change(14, $aa$
ALTER INDEX m_ref_object_create_approverTargetOidRelationId_idx
    RENAME TO m_ref_object_create_approver_targetOidRelationId_idx;
ALTER INDEX m_ref_object_modify_approverTargetOidRelationId_idx
    RENAME TO m_ref_object_modify_approver_targetOidRelationId_idx;
$aa$);

-- Making resource.abstract queryable
call apply_change(15, $aa$
ALTER TABLE m_resource ADD abstract BOOLEAN;
$aa$);

-- Task Affected Indexing (Changes to types)
call apply_change(16, $aa$
ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'AFFECTED_OBJECTS' AFTER 'ACCESS_CERTIFICATION_WORK_ITEM';
$aa$);

-- Task Affected Indexing (tables), empty now, replaced with change 19

call apply_change(17, $$ SELECT 1 $$, true);


-- Resource/super/resourceRef Indexing (tables)
call apply_change(18, $aa$
ALTER TABLE m_resource
ADD COLUMN superRefTargetOid UUID,
ADD COLUMN superRefTargetType ObjectType,
ADD COLUMN superRefRelationId INTEGER REFERENCES m_uri(id);
$aa$);

-- Fixed upgrade for task indexing
-- Drop tables should only affect development machines
call apply_change(19, $aa$
DROP TABLE IF EXISTS m_task_affected_resource_objects;
DROP TABLE IF EXISTS m_task_affected_objects;

CREATE TABLE m_task_affected_objects (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('AFFECTED_OBJECTS') STORED
     CHECK (containerType = 'AFFECTED_OBJECTS'),
    activityId INTEGER REFERENCES m_uri(id),
    type ObjectType,
    archetypeRefTargetOid UUID,
    archetypeRefTargetType ObjectType,
    archetypeRefRelationId INTEGER REFERENCES m_uri(id),
    objectClassId INTEGER REFERENCES m_uri(id),
    resourceRefTargetOid UUID,
    resourceRefTargetType ObjectType,
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    intent TEXT,
    kind ShadowKindType,
    PRIMARY KEY (ownerOid, cid)
) INHERITS(m_container);

$aa$)

call apply_change(20, $aa$
CREATE TYPE ExecutionModeType AS ENUM ('FULL', 'PREVIEW', 'SHADOW_MANAGEMENT_PREVIEW', 'DRY_RUN', 'NONE', 'BUCKET_ANALYSIS');
CREATE TYPE PredefinedConfigurationType AS ENUM ( 'PRODUCTION', 'DEVELOPMENT' );

ALTER TABLE m_task_affected_objects
  ADD COLUMN executionMode ExecutionModeType,
  ADD COLUMN predefinedConfigurationToUse PredefinedConfigurationType;
$aa$)


---
-- WRITE CHANGES ABOVE ^^
-- IMPORTANT: update apply_change number at the end of postgres-new.sql
-- to match the number used in the last change here!
-- Also update SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
