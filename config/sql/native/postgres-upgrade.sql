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

$aa$);

call apply_change(20, $aa$
CREATE TYPE ExecutionModeType AS ENUM ('FULL', 'PREVIEW', 'SHADOW_MANAGEMENT_PREVIEW', 'DRY_RUN', 'NONE', 'BUCKET_ANALYSIS');
CREATE TYPE PredefinedConfigurationType AS ENUM ( 'PRODUCTION', 'DEVELOPMENT' );

ALTER TABLE m_task_affected_objects
  ADD COLUMN executionMode ExecutionModeType,
  ADD COLUMN predefinedConfigurationToUse PredefinedConfigurationType;
$aa$);

call apply_change(21, $aa$
ALTER TABLE m_user
  ADD COLUMN personalNumber TEXT;
$aa$);


-- Role Mining --

call apply_change(22, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_CLUSTER' AFTER 'ROLE';
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_SESSION' AFTER 'ROLE_ANALYSIS_CLUSTER';
$aa$);

call apply_change(23, $aa$
CREATE TABLE m_role_analysis_cluster (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_CLUSTER') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_CLUSTER'),
        parentRefTargetOid UUID,
        parentRefTargetType ObjectType,
        parentRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_role_analysis_cluster_oid_insert_tr BEFORE INSERT ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_role_analysis_cluster_update_tr BEFORE UPDATE ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_role_analysis_cluster_oid_delete_tr AFTER DELETE ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_role_analysis_cluster_parentRefTargetOid_idx ON m_role_analysis_cluster (parentRefTargetOid);
CREATE INDEX m_role_analysis_cluster_parentRefTargetType_idx ON m_role_analysis_cluster (parentRefTargetType);
CREATE INDEX m_role_analysis_cluster_parentRefRelationId_idx ON m_role_analysis_cluster (parentRefRelationId);


CREATE TABLE m_role_analysis_session (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_SESSION') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_SESSION')
        )
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_role_analysis_session_oid_insert_tr BEFORE INSERT ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_role_analysis_session_update_tr BEFORE UPDATE ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_role_analysis_session_oid_delete_tr AFTER DELETE ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();
$aa$);

-- Display Name for Connector Type

call apply_change(24, $aa$
    ALTER TABLE m_connector ADD  displayNameOrig TEXT;
    ALTER TABLE m_connector ADD displayNameNorm TEXT;
$aa$);

call apply_change(25, $aa$
CREATE OR REPLACE PROCEDURE m_refresh_org_closure(force boolean = false)
    LANGUAGE plpgsql
AS $$
DECLARE
    flag_val text;
BEGIN
    -- We use advisory session lock only for the check + refresh, then release it immediately.
    -- This can still dead-lock two transactions in a single thread on the select/delete combo,
    -- (I mean, who would do that?!) but works fine for parallel transactions.
    PERFORM pg_advisory_lock(47);
    BEGIN
        SELECT value INTO flag_val FROM m_global_metadata WHERE name = 'orgClosureRefreshNeeded';
        IF flag_val = 'true' OR force THEN
            REFRESH MATERIALIZED VIEW m_org_closure;
            DELETE FROM m_global_metadata WHERE name = 'orgClosureRefreshNeeded';
        END IF;
        PERFORM pg_advisory_unlock(47);
    EXCEPTION WHEN OTHERS THEN
        -- Whatever happens we definitely want to release the lock.
        PERFORM pg_advisory_unlock(47);
        RAISE;
    END;
END;
$$;
$aa$);

-- Assignments have separate full object
call apply_change(26, $aa$
    ALTER TABLE m_assignment ADD COLUMN fullObject BYTEA;
    ALTER TABLE m_operation_execution ADD COLUMN fullObject BYTEA;
    ALTER TABLE m_ref_projection ADD COLUMN fullObject BYTEA;
    ALTER TABLE m_ref_role_membership ADD COLUMN fullObject BYTEA;
$aa$);

--- Policy Type

call apply_change(27, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'POLICY' AFTER 'ORG';
$aa$);
call apply_change(28, $aa$
    CREATE TABLE m_policy (
        oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
        objectType ObjectType GENERATED ALWAYS AS ('POLICY') STORED
            CHECK (objectType = 'POLICY')
    )
        INHERITS (m_abstract_role);

    CREATE TRIGGER m_policy_oid_insert_tr BEFORE INSERT ON m_policy
        FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
    CREATE TRIGGER m_policy_update_tr BEFORE UPDATE ON m_policy
        FOR EACH ROW EXECUTE FUNCTION before_update_object();
    CREATE TRIGGER m_policy_oid_delete_tr AFTER DELETE ON m_policy
        FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

    CREATE INDEX m_policy_nameOrig_idx ON m_policy (nameOrig);
    CREATE UNIQUE INDEX m_policy_nameNorm_key ON m_policy (nameNorm);
    CREATE INDEX m_policy_subtypes_idx ON m_policy USING gin(subtypes);
    CREATE INDEX m_policy_identifier_idx ON m_policy (identifier);
    CREATE INDEX m_policy_validFrom_idx ON m_policy (validFrom);
    CREATE INDEX m_policy_validTo_idx ON m_policy (validTo);
    CREATE INDEX m_policy_fullTextInfo_idx ON m_policy USING gin(fullTextInfo gin_trgm_ops);
    CREATE INDEX m_policy_createTimestamp_idx ON m_policy (createTimestamp);
    CREATE INDEX m_policy_modifyTimestamp_idx ON m_policy (modifyTimestamp);
$aa$);

--- Schema Type

call apply_change(29, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'SCHEMA' AFTER 'ROLE_ANALYSIS_SESSION';
$aa$);

call apply_change(30, $aa$
CREATE TABLE m_schema (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SCHEMA') STORED
       CHECK (objectType = 'SCHEMA')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_schema_oid_insert_tr BEFORE INSERT ON m_schema
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_schema_update_tr BEFORE UPDATE ON m_schema
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_schema_oid_delete_tr AFTER DELETE ON m_schema
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

$aa$);

-- associations (maybe temporary)
call apply_change(31, $aa$
ALTER TYPE ShadowKindType ADD VALUE IF NOT EXISTS 'ASSOCIATED' AFTER 'GENERIC';
$aa$);


-- value metatada for assignments and inducements
call apply_change(32, $aa$
ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'ASSIGNMENT_METADATA' AFTER 'ASSIGNMENT';
$aa$);

call apply_change(33, $aa$
CREATE TABLE m_assignment_metadata (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    ownerType ObjectType,
    assignmentCid INTEGER NOT NULL,
    containerType ContainerType GENERATED ALWAYS AS ('ASSIGNMENT_METADATA') STORED
        CHECK (containerType = 'ASSIGNMENT_METADATA'),

    -- Storage metadata
    creatorRefTargetOid UUID,
    creatorRefTargetType ObjectType,
    creatorRefRelationId INTEGER REFERENCES m_uri(id),
    createChannelId INTEGER REFERENCES m_uri(id),
    createTimestamp TIMESTAMPTZ,
    modifierRefTargetOid UUID,
    modifierRefTargetType ObjectType,
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    modifyChannelId INTEGER REFERENCES m_uri(id),
    modifyTimestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, assignmentCid, cid)
) INHERITS(m_container);

CREATE INDEX m_assignment_metadata_createTimestamp_idx ON m_assignment_metadata (createTimestamp);
CREATE INDEX m_assignment_metadata_modifyTimestamp_idx ON m_assignment_metadata (modifyTimestamp);

ALTER TABLE m_assignment_ref_create_approver ADD COLUMN metadataCid INTEGER;

-- Primary key should also consider metadata

ALTER TABLE "m_assignment_ref_create_approver" DROP CONSTRAINT "m_assignment_ref_create_approver_pkey";

ALTER TABLE "m_assignment_ref_create_approver" ADD CONSTRAINT "m_assignment_ref_create_approver_pkey"
  UNIQUE ("owneroid", "assignmentcid", "metadatacid", "referencetype", "relationid", "targetoid");


ALTER TABLE m_assignment_ref_modify_approver ADD COLUMN metadataCid INTEGER;

ALTER TABLE "m_assignment_ref_modify_approver" DROP CONSTRAINT "m_assignment_ref_modify_approver_pkey";

ALTER TABLE "m_assignment_ref_modify_approver" ADD CONSTRAINT "m_assignment_ref_modify_approver_pkey"
  UNIQUE ("owneroid", "assignmentcid", "metadatacid", "referencetype", "relationid", "targetoid");

$aa$);
call apply_change(34, $aa$
ALTER TABLE "m_assignment_metadata"
ADD CONSTRAINT "m_assignment_metadata_owneroid_assignmentcid_cid" PRIMARY KEY ("owneroid", "assignmentcid", "cid"),
DROP CONSTRAINT "m_assignment_metadata_pkey";

ALTER TABLE "m_assignment_metadata"
ADD FOREIGN KEY ("owneroid", "assignmentcid") REFERENCES "m_assignment" ("owneroid", "cid") ON DELETE CASCADE;

$aa$);

call apply_change(35, $aa$
ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'ROLE_ANALYSIS_OUTLIER' AFTER 'ROLE_ANALYSIS_SESSION';
$aa$);

call apply_change(36, $aa$
CREATE TABLE m_role_analysis_outlier (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_OUTLIER') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_OUTLIER')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_role_analysis_outlier_oid_insert_tr BEFORE INSERT ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_role_analysis_outlier_update_tr BEFORE UPDATE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_role_analysis_outlier_oid_delete_tr AFTER DELETE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();
$aa$);

call apply_change(37, $aa$
    CREATE TABLE m_shadow_ref_attribute (
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        ownerType ObjectType NOT NULL,

        pathId INTEGER NOT NULL,
        resourceOid UUID,
        ownerObjectClassId INTEGER,
        targetOid UUID NOT NULL, -- soft-references m_object
        targetType ObjectType NOT NULL,
        relationId INTEGER NOT NULL REFERENCES m_uri(id)
    );

    CREATE INDEX m_shadow_ref_attribute_ownerOid_idx ON m_shadow_ref_attribute (ownerOid);
$aa$);

call apply_change(38, $aa$
ALTER TYPE ReferenceType ADD VALUE IF NOT EXISTS 'TASK_AFFECTED_OBJECT' AFTER 'ROLE_MEMBERSHIP';
$aa$);

call apply_change(39, $aa$
CREATE TABLE m_ref_task_affected_object (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    affectedObjectCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('TASK_AFFECTED_OBJECT') STORED
        CHECK (referenceType = 'TASK_AFFECTED_OBJECT')
)
    INHERITS (m_reference);

ALTER TABLE m_ref_task_affected_object ADD CONSTRAINT m_ref_task_affected_object_id_fk
    FOREIGN KEY (ownerOid, affectedObjectCid) REFERENCES m_task_affected_objects (ownerOid, cid)
        ON DELETE CASCADE;

CREATE INDEX m_ref_task_affected_object_targetOidRelationId_idx
    ON m_ref_task_affected_object (targetOid, relationId);
$aa$);



call apply_change(40, $aa$

    ALTER TYPE ReferenceType ADD VALUE IF NOT EXISTS 'ASSIGNMENT_EFFECTIVE_MARK' AFTER 'ASSIGNMENT_MODIFY_APPROVER';
$aa$);
call apply_change(41, $aa$
    CREATE TABLE m_ref_assignment_effective_mark (
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        assignmentCid INTEGER NOT NULL,
        referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_EFFECTIVE_MARK') STORED
            CHECK (referenceType = 'ASSIGNMENT_EFFECTIVE_MARK'),
        PRIMARY KEY (ownerOid, assignmentCid, relationId, targetOid)
    )
    INHERITS (m_reference);

    CREATE INDEX m_ref_assignment_effective_mark_targetOidRelationId_idx
        ON m_ref_assignment_effective_mark (targetOid, relationId);
$aa$);

call apply_change(42,$aa$
    ALTER TABLE m_shadow NO INHERIT m_object;
    ALTER TABLE m_shadow RENAME TO m_shadow_default;

    ALTER TABLE m_shadow_default
    ALTER resourceRefTargetOid TYPE uuid,
    ALTER resourceRefTargetOid SET NOT NULL;

    DROP TRIGGER m_shadow_oid_insert_tr ON m_shadow_default;
    DROP TRIGGER m_shadow_update_tr ON m_shadow_default;
    DROP TRIGGER m_shadow_oid_delete_tr ON m_shadow_default;

    CREATE TABLE m_shadow (
        oid UUID NOT NULL REFERENCES m_object_oid(oid),
        objectType ObjectType
                GENERATED ALWAYS AS ('SHADOW') STORED
            CONSTRAINT m_shadow_objecttype_check
                CHECK (objectType = 'SHADOW'),
        nameOrig TEXT NOT NULL,
        nameNorm TEXT NOT NULL,
        fullObject BYTEA,
        tenantRefTargetOid UUID,
        tenantRefTargetType ObjectType,
        tenantRefRelationId INTEGER REFERENCES m_uri(id),
        lifecycleState TEXT,
        cidSeq BIGINT NOT NULL DEFAULT 1, -- sequence for container id, next free cid
        version INTEGER NOT NULL DEFAULT 1,
        policySituations INTEGER[], -- soft-references m_uri, only EQ filter
        subtypes TEXT[], -- only EQ filter
        fullTextInfo TEXT,

        ext JSONB,
        creatorRefTargetOid UUID,
        creatorRefTargetType ObjectType,
        creatorRefRelationId INTEGER REFERENCES m_uri(id),
        createChannelId INTEGER REFERENCES m_uri(id),
        createTimestamp TIMESTAMPTZ,
        modifierRefTargetOid UUID,
        modifierRefTargetType ObjectType,
        modifierRefRelationId INTEGER REFERENCES m_uri(id),
        modifyChannelId INTEGER REFERENCES m_uri(id),
        modifyTimestamp TIMESTAMPTZ,

        -- these are purely DB-managed metadata, not mapped to in midPoint
        db_created TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
        db_modified TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, -- updated in update trigger

        objectClassId INTEGER REFERENCES m_uri(id),
        resourceRefTargetOid UUID,
        resourceRefTargetType ObjectType,
        resourceRefRelationId INTEGER REFERENCES m_uri(id),
        intent TEXT,
        tag TEXT,
        kind ShadowKindType,
        dead BOOLEAN,
        exist BOOLEAN,
        fullSynchronizationTimestamp TIMESTAMPTZ,
        pendingOperationCount INTEGER NOT NULL,
        primaryIdentifierValue TEXT,
        synchronizationSituation SynchronizationSituationType,
        synchronizationTimestamp TIMESTAMPTZ,
        attributes JSONB,
        -- correlation
        correlationStartTimestamp TIMESTAMPTZ,
        correlationEndTimestamp TIMESTAMPTZ,
        correlationCaseOpenTimestamp TIMESTAMPTZ,
        correlationCaseCloseTimestamp TIMESTAMPTZ,
        correlationSituation CorrelationSituationType
    ) PARTITION BY LIST (resourceRefTargetOid);
    CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
        FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
    CREATE TRIGGER m_shadow_update_tr BEFORE UPDATE ON m_shadow
        FOR EACH ROW EXECUTE FUNCTION before_update_object();
    CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
        FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

    ALTER TABLE m_shadow ATTACH PARTITION m_shadow_default DEFAULT;

    DROP VIEW IF EXISTS m_object_view ;
    CREATE VIEW m_object_view
    AS SELECT
        oid,
        objectType,
        nameOrig,
        nameNorm,
        fullObject,
        tenantRefTargetOid,
        tenantRefTargetType,
        tenantRefRelationId,
        lifecycleState,
        cidSeq,
        version,
        policySituations,
        subtypes,
        fullTextInfo,
        ext,
        creatorRefTargetOid,
        creatorRefTargetType,
        creatorRefRelationId,
        createChannelId,
        createTimestamp,
        modifierRefTargetOid,
        modifierRefTargetType,
        modifierRefRelationId,
        modifyChannelId,
        modifyTimestamp,
        db_created,
        db_modified
    from m_object
    UNION SELECT
        oid,
        objectType,
        nameOrig,
        nameNorm,
        fullObject,
        tenantRefTargetOid,
        tenantRefTargetType,
        tenantRefRelationId,
        lifecycleState,
        cidSeq,
        version,
        policySituations,
        subtypes,
        fullTextInfo,
        ext,
        creatorRefTargetOid,
        creatorRefTargetType,
        creatorRefRelationId,
        createChannelId,
        createTimestamp,
        modifierRefTargetOid,
        modifierRefTargetType,
        modifierRefRelationId,
        modifyChannelId,
        modifyTimestamp,
        db_created,
        db_modified
    from m_shadow;
$aa$);



call apply_change(43,$aa$
    CREATE OR REPLACE FUNCTION m_shadow_create_partition() RETURNS trigger AS $BODY$
        DECLARE
          resource UUID;
          partitionParent TEXT;
          partitionName TEXT;
          sourceTable TEXT;
          tableOid TEXT;
        BEGIN
          IF NEW.resourceOid IS NULL THEN
            /* Do not create new partition */
            IF new."table" != 'm_shadow_default' THEN
                RAISE EXCEPTION 'Only m_shadow_default partition is supported for any resource';
            END IF;
            RETURN NULL;
          END IF;
          tableOid := REPLACE(new.resourceOid::text,'-','_');
          partitionParent := 'm_shadow_' || tableOid;

          IF NOT new.partition THEN
            IF new.resourceOid IS NULL THEN
              RAISE EXCEPTION 'Can not create partionioned table without resource oid';
            END IF;
            EXECUTE format('CREATE TABLE %I (like m_shadow INCLUDING ALL ) PARTITION BY LIST(objectClassId); ', new."table");
            RETURN new;
          END IF;


          /* Real partitions holding data */
          IF new.objectClassId IS NOT NULL THEN
            sourceTable := (SELECT p.table FROM m_shadow_partition_def AS p WHERE p.resourceOid = new.resourceOid AND p.objectClassId IS NULL AND p.partition LIMIT 1);
          END IF;

          IF sourceTable IS NULL THEN
            sourceTable := 'm_shadow_default';
          END IF;

          /* We should check if resource and resource default table exists */

          /* Create Partition table */
          EXECUTE format('CREATE TABLE %I (like %I INCLUDING ALL ); ', new."table", sourceTable);
          EXECUTE format('ALTER TABLE %I ALTER objecttype DROP EXPRESSION;', new."table");

          /* Move data to new partition */
          IF new.objectClassId IS NULL THEN
            EXECUTE format('INSERT into %I SELECT * FROM %I
                where resourceRefTargetOid = ''%s''',
                new."table", sourceTable, new.resourceOid);
          ELSE
            EXECUTE format('INSERT into %I SELECT * FROM %I
                where resourceRefTargetOid = ''%s'' AND objectClassId = %s',
                new."table", sourceTable, new.resourceOid, new.objectClassId);
          END IF;
          EXECUTE format('ALTER TABLE %I DROP objecttype;', new.table);
          EXECUTE format('ALTER TABLE %I ADD COLUMN objecttype ObjectType
            GENERATED ALWAYS AS (''SHADOW'') STORED
                CONSTRAINT m_shadow_objecttype_check
                    CHECK (objectType = ''SHADOW'')', new.table);

          /* We should skip drop triggers for m_oid table (also probably in resource default table (if exists)) */
          EXECUTE format('ALTER TABLE %I DISABLE TRIGGER ALL;', sourceTable);
          IF new.objectClassId IS NULL THEN
            EXECUTE format('DELETE FROM %I
                where resourceRefTargetOid = ''%s''', sourceTable, new.resourceOid);
          ELSE
            EXECUTE format('DELETE FROM %I
                where resourceRefTargetOid = ''%s'' AND objectClassId = %s', sourceTable, new.resourceOid, new.objectClassId);
          END IF;
          /* Reenable triggers in original table */
          EXECUTE format('ALTER TABLE %I ENABLE TRIGGER ALL;', sourceTable);

          IF new.objectClassId IS  NULL THEN
            /* Attach table as default partition */
            EXECUTE FORMAT ('ALTER TABLE %I ATTACH PARTITION %I DEFAULT', partitionParent, new."table");
          ELSE
            EXECUTE FORMAT ('ALTER TABLE %I ATTACH PARTITION %I FOR VALUES IN (%s)', partitionParent, new."table", new.objectClassId);
            /* Attach table as objectClass partiion */
          END IF;
          RETURN new;
        END;
      $BODY$
    LANGUAGE plpgsql;

    CREATE OR REPLACE FUNCTION m_shadow_delete_partition() RETURNS trigger AS $BODY$
            BEGIN
                EXECUTE format('DROP TABLE IF EXISTS  %I;', OLD."table" );
                RETURN OLD;
            END

        $BODY$
    LANGUAGE plpgsql;


    CREATE OR REPLACE FUNCTION m_shadow_update_partition() RETURNS trigger AS $BODY$
            BEGIN
                IF new.partition THEN
                    return new;
                END IF;

                IF old.attached = new.attached THEN
                    return new;
                END IF;
                IF new.attached THEN
                    EXECUTE FORMAT ('ALTER TABLE m_shadow ATTACH PARTITION %I FOR VALUES IN (''%s'')', new."table", new.resourceOid);
                END IF;
                RETURN new;
            END

        $BODY$
    LANGUAGE plpgsql;



    DROP TABLE IF EXISTS "m_shadow_partition_def";
    CREATE TABLE m_shadow_partition_def (
        resourceOid uuid,
        objectClassId integer,
        "table" text NOT NULL,
        partition boolean NOT NULL,
        attached boolean NOT NULL
    ) WITH (oids = false);

    CREATE TRIGGER "m_shadow_partition_def_bi" BEFORE INSERT ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_create_partition();
    CREATE TRIGGER "m_shadow_partition_def_bu" BEFORE UPDATE ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_update_partition();
    CREATE TRIGGER "m_shadow_partition_def_bd" BEFORE DELETE ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_delete_partition();
$aa$);

call apply_change(44, $aa$
ALTER TYPE ShadowKindType RENAME VALUE 'ASSOCIATED' TO 'ASSOCIATION';
$aa$);

call apply_change(45, $aa$
    ALTER TABLE m_shadow
       ADD COLUMN disableReasonId INTEGER REFERENCES m_uri(id),
       ADD COLUMN  enableTimestamp TIMESTAMPTZ,
       ADD COLUMN   disableTimestamp TIMESTAMPTZ;
$aa$);

call apply_change(46, $aa$
    ALTER TABLE m_role_analysis_outlier
       ADD COLUMN targetObjectRefTargetOid UUID,
       ADD COLUMN targetObjectRefTargetType ObjectType,
       ADD COLUMN targetObjectRefRelationId INTEGER REFERENCES m_uri(id);

       CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetOid_idx
               ON m_role_analysis_outlier (targetObjectRefTargetOid);

       CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetType_idx
               ON m_role_analysis_outlier (targetObjectRefTargetType);

       CREATE INDEX m_role_analysis_outlier_targetObjectRefRelationId_idx
               ON m_role_analysis_outlier (targetObjectRefRelationId);
$aa$);

call apply_change(47, $aa$
    ALTER TABLE m_shadow
       ADD COLUMN lastLoginTimestamp TIMESTAMPTZ;

   CREATE INDEX m_shadow_default_lastLoginTimestamp_idx
                      ON m_shadow_default (lastLoginTimestamp);
$aa$);


call apply_change(48, $aa$
    ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'CLUSTER_DETECTED_PATTERN' AFTER 'CASE_WORK_ITEM';
    ALTER TYPE ContainerType ADD VALUE IF NOT EXISTS 'OUTLIER_PARTITION' AFTER 'OPERATION_EXECUTION';
$aa$);

call apply_change(49, $aa$
    CREATE TABLE m_role_analysis_cluster_detected_pattern (
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        containerType ContainerType GENERATED ALWAYS AS ('CLUSTER_DETECTED_PATTERN') STORED
            CHECK (containerType = 'CLUSTER_DETECTED_PATTERN'),
        ---
        reductionCount double precision,
        PRIMARY KEY (ownerOid, cid)
    )
        INHERITS(m_container);

    CREATE INDEX m_role_analysis_cluster_detected_pattern_reductionCount_idx ON m_role_analysis_cluster_detected_pattern (reductionCount);

    CREATE TABLE m_role_analysis_outlier_partition (
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        containerType ContainerType GENERATED ALWAYS AS ('OUTLIER_PARTITION') STORED
            CHECK (containerType = 'OUTLIER_PARTITION'),
        ---
        clusterRefOid UUID,
        clusterRefTargetType ObjectType,
        clusterRefRelationId INTEGER REFERENCES m_uri(id),
        PRIMARY KEY (ownerOid, cid)
    )
        INHERITS(m_container);

    CREATE INDEX m_role_analysis_outlier_partition_clusterRefOid_idx ON m_role_analysis_outlier_partition (clusterRefOid);

    ALTER TABLE m_role_analysis_outlier ADD COLUMN overallConfidence double precision;

    CREATE INDEX m_role_analysis_outlier_overallConfidence_idx ON m_role_analysis_outlier (overallConfidence);
$aa$);

call apply_change(50, $aa$
    ALTER TABLE m_role_analysis_outlier_partition
       ADD COLUMN overallConfidence double precision;

   CREATE INDEX  m_role_analysis_outlier_partition_overallConfidence_idx
    ON m_role_analysis_outlier_partition (overallConfidence);

$aa$);

call apply_change(51, $aa$
    CREATE OR REPLACE FUNCTION m_shadow_create_partition() RETURNS trigger AS $BODY$
    DECLARE
      resource UUID;
      partitionParent TEXT;
      partitionName TEXT;
      sourceTable TEXT;
      tableOid TEXT;
    BEGIN
      IF NEW.resourceOid IS NULL THEN
        /* Do not create new partition */
        IF new."table" != 'm_shadow_default' THEN
            RAISE EXCEPTION 'Only m_shadow_default partition is supported for any resource';
        END IF;
        RETURN NULL;
      END IF;
      tableOid := REPLACE(new.resourceOid::text,'-','_');
      partitionParent := 'm_shadow_' || tableOid;

      IF NOT new.partition THEN
        IF new.resourceOid IS NULL THEN
          RAISE EXCEPTION 'Can not create partitioned table without resource oid';
        END IF;
        EXECUTE format('CREATE TABLE %I (like m_shadow INCLUDING ALL ) PARTITION BY LIST(objectClassId); ', new."table");
        RETURN new;
      END IF;

      /* Real partitions holding data */
      IF new.objectClassId IS NOT NULL THEN
        sourceTable := (SELECT p.table FROM m_shadow_partition_def AS p WHERE p.resourceOid = new.resourceOid AND p.objectClassId IS NULL AND p.partition LIMIT 1);
      END IF;

      IF sourceTable IS NULL THEN
        sourceTable := 'm_shadow_default';
      END IF;

      /* We should check if resource and resource default table exists */

      /* Create Partition table */
      EXECUTE format('CREATE TABLE %I (like %I INCLUDING ALL ); ', new."table", sourceTable);
      EXECUTE format('ALTER TABLE %I ALTER objecttype DROP EXPRESSION;', new."table");

      /* Move data to new partition */
      IF new.objectClassId IS NULL THEN
        EXECUTE format('INSERT into %I SELECT * FROM %I
            where resourceRefTargetOid = ''%s''',
            new."table", sourceTable, new.resourceOid);
      ELSE
        EXECUTE format('INSERT into %I SELECT * FROM %I
            where resourceRefTargetOid = ''%s'' AND objectClassId = %s',
            new."table", sourceTable, new.resourceOid, new.objectClassId);
      END IF;
      EXECUTE format('ALTER TABLE %I DROP objecttype;', new.table);
      EXECUTE format('ALTER TABLE %I ADD COLUMN objecttype ObjectType
        GENERATED ALWAYS AS (''SHADOW'') STORED
            CONSTRAINT m_shadow_objecttype_check
                CHECK (objectType = ''SHADOW'')', new.table);

      /* We should skip drop triggers for m_oid table (also probably in resource default table (if exists)) */
      EXECUTE format('ALTER TABLE %I DISABLE TRIGGER USER;', sourceTable);
      IF new.objectClassId IS NULL THEN
        EXECUTE format('DELETE FROM %I
            where resourceRefTargetOid = ''%s''', sourceTable, new.resourceOid);
      ELSE
        EXECUTE format('DELETE FROM %I
            where resourceRefTargetOid = ''%s'' AND objectClassId = %s', sourceTable, new.resourceOid, new.objectClassId);
      END IF;
      /* Reenable triggers in original table */
      EXECUTE format('ALTER TABLE %I ENABLE TRIGGER USER;', sourceTable);

      IF new.objectClassId IS  NULL THEN
        /* Attach table as default partition */
        EXECUTE FORMAT ('ALTER TABLE %I ATTACH PARTITION %I DEFAULT', partitionParent, new."table");
      ELSE
        EXECUTE FORMAT ('ALTER TABLE %I ATTACH PARTITION %I FOR VALUES IN (%s)', partitionParent, new."table", new.objectClassId);
        /* Attach table as objectClass partition */
      END IF;

      RETURN new;
    END;
  $BODY$
LANGUAGE plpgsql;

$aa$);

--- MIDPILOT CHANGES

--- Schema Type

call apply_change(52, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'APPLICATION' AFTER 'ACCESS_CERTIFICATION_DEFINITION';
$aa$);

call apply_change(53, $aa$
CREATE TABLE m_application (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('APPLICATION') STORED
        CHECK (objectType = 'APPLICATION')
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_application_oid_insert_tr BEFORE INSERT ON m_application
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_application_update_tr BEFORE UPDATE ON m_application
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_application_oid_delete_tr AFTER DELETE ON m_application
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_application_nameOrig_idx ON m_application (nameOrig);
CREATE UNIQUE INDEX m_application_nameNorm_key ON m_application (nameNorm);
CREATE INDEX m_application_subtypes_idx ON m_application USING gin(subtypes);
CREATE INDEX m_application_identifier_idx ON m_application (identifier);
CREATE INDEX m_application_validFrom_idx ON m_application (validFrom);
CREATE INDEX m_application_validTo_idx ON m_application (validTo);
CREATE INDEX m_application_fullTextInfo_idx ON m_application USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_application_createTimestamp_idx ON m_application (createTimestamp);
CREATE INDEX m_application_modifyTimestamp_idx ON m_application (modifyTimestamp);

$aa$);

call apply_change(54, $aa$
   ALTER TYPE ObjectType ADD VALUE IF NOT EXISTS 'CONNECTOR_DEVELOPMENT' AFTER 'CONNECTOR';
$aa$);

call apply_change(55, $aa$
CREATE TABLE m_connector_development (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_DEVELOPMENT') STORED
        CHECK (objectType = 'CONNECTOR_DEVELOPMENT')
)
    INHERITS (m_object);

CREATE TRIGGER m_connector_development_oid_insert_tr BEFORE INSERT ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_connector_development_update_tr BEFORE UPDATE ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_connector_development_oid_delete_tr AFTER DELETE ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();
$aa$);

call apply_change(56, $aa$
    DROP INDEX IF EXISTS m_connector_typeVersion_key;
    DROP INDEX IF EXISTS m_connector_typeversionhost_key;
$aa$);
call apply_change(57, $aa$
CREATE INDEX m_connector_typeVersion_key
    ON m_connector (connectorType, connectorVersion)
    WHERE connectorHostRefTargetOid IS NULL;
CREATE INDEX m_connector_typeVersionHost_key
    ON m_connector (connectorType, connectorVersion, connectorHostRefTargetOid)
    WHERE connectorHostRefTargetOid IS NOT NULL;
$aa$);

---
-- WRITE CHANGES ABOVE ^^
-- IMPORTANT: update apply_change number at the end of postgres-new.sql
-- to match the number used in the last change here!
-- Also update SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
