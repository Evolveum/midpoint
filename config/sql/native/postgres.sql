/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- Naming conventions:
-- M_ prefix is used for tables in main part of the repo, MA_ for audit tables (can be separate)
-- Constraints/indexes use table_column(s)_suffix convention, with PK for primary key,
-- FK foreign key, IDX for index, KEY for unique index.
-- TR is suffix for triggers.
-- Names are generally lowercase (despite prefix/suffixes above in uppercase ;-)).
-- Column names are Java style and match attribute names from M-classes (e.g. MObject).
--
-- Other notes:
-- TEXT is used instead of VARCHAR, see: https://dba.stackexchange.com/a/21496/157622
-- We prefer "CREATE UNIQUE INDEX" to "ALTER TABLE ... ADD CONSTRAINT", unless the column
-- is marked as UNIQUE directly - then the index is implied, don't create it explicitly.
--
-- For Audit tables see 'postgres-new-audit.sql' right next to this file.
-- For Quartz tables see 'postgres-new-quartz.sql'.

-- noinspection SqlResolveForFile @ operator-class/"gin__int_ops"

-- public schema is not used as of now, everything is in the current user schema
-- https://www.postgresql.org/docs/15/ddl-schemas.html#DDL-SCHEMAS-PATTERNS
-- see secure schema usage pattern

-- just in case CURRENT_USER schema was dropped (fastest way to remove all midpoint objects)
-- drop schema current_user cascade;
CREATE SCHEMA IF NOT EXISTS AUTHORIZATION CURRENT_USER;

CREATE EXTENSION IF NOT EXISTS intarray; -- support for indexing INTEGER[] columns
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; -- fuzzy string match (levenshtein, etc.)

-- region custom enum types
-- Some enums are from schema, some are only defined in repo-sqale.
-- All Java enum types must be registered in SqaleRepoContext constructor.

-- First purely repo-sqale enums (these have M prefix in Java, the rest of the name is the same):
CREATE TYPE ContainerType AS ENUM (
    'ACCESS_CERTIFICATION_CASE',
    'ACCESS_CERTIFICATION_WORK_ITEM',
    'AFFECTED_OBJECTS',
    'ASSIGNMENT',
    'ASSIGNMENT_METADATA',
    'CASE_WORK_ITEM',
    'CLUSTER_DETECTED_PATTERN',
    'FOCUS_IDENTITY',
    'INDUCEMENT',
    'LOOKUP_TABLE_ROW',
    'OPERATION_EXECUTION',
    'OUTLIER_PARTITION',
    'SIMULATION_RESULT_PROCESSED_OBJECT',
    'TRIGGER');

-- NOTE: Keep in sync with the same enum in postgres-new-audit.sql!
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
    'SCHEMA',
    'SECURITY_POLICY',
    'SEQUENCE',
    'SERVICE',
    'SHADOW',
    'SIMULATION_RESULT',
    'SYSTEM_CONFIGURATION',
    'TASK',
    'USER',
    'VALUE_POLICY');

CREATE TYPE ReferenceType AS ENUM (
    'ARCHETYPE',
    'ASSIGNMENT_CREATE_APPROVER',
    'ASSIGNMENT_MODIFY_APPROVER',
    'ASSIGNMENT_EFFECTIVE_MARK',
    'ACCESS_CERT_WI_ASSIGNEE',
    'ACCESS_CERT_WI_CANDIDATE',
    'CASE_WI_ASSIGNEE',
    'CASE_WI_CANDIDATE',
    'DELEGATED',
    'INCLUDE',
    'OBJECT_CREATE_APPROVER',
    'OBJECT_EFFECTIVE_MARK',
    'OBJECT_MODIFY_APPROVER',
    'OBJECT_PARENT_ORG',
    'PERSONA',
    'PROCESSED_OBJECT_EVENT_MARK',
    'PROJECTION',
    'RESOURCE_BUSINESS_CONFIGURATION_APPROVER',
    'ROLE_MEMBERSHIP',
    'TASK_AFFECTED_OBJECT');

CREATE TYPE ExtItemHolderType AS ENUM (
    'EXTENSION',
    'ATTRIBUTES');

CREATE TYPE ExtItemCardinality AS ENUM (
    'SCALAR',
    'ARRAY');

-- Schema based enums have the same name like their enum classes (I like the Type suffix here):
CREATE TYPE AccessCertificationCampaignStateType AS ENUM (
    'CREATED', 'IN_REVIEW_STAGE', 'REVIEW_STAGE_DONE', 'IN_REMEDIATION', 'CLOSED');

CREATE TYPE ActivationStatusType AS ENUM ('ENABLED', 'DISABLED', 'ARCHIVED');

CREATE TYPE AdministrativeAvailabilityStatusType AS ENUM ('MAINTENANCE', 'OPERATIONAL');

CREATE TYPE AvailabilityStatusType AS ENUM ('DOWN', 'UP', 'BROKEN');

CREATE TYPE CorrelationSituationType AS ENUM ('UNCERTAIN', 'EXISTING_OWNER', 'NO_OWNER', 'ERROR');

CREATE TYPE ExecutionModeType AS ENUM ('FULL', 'PREVIEW', 'SHADOW_MANAGEMENT_PREVIEW', 'DRY_RUN', 'NONE', 'BUCKET_ANALYSIS');

CREATE TYPE LockoutStatusType AS ENUM ('NORMAL', 'LOCKED');

CREATE TYPE NodeOperationalStateType AS ENUM ('UP', 'DOWN', 'STARTING');

CREATE TYPE OperationExecutionRecordTypeType AS ENUM ('SIMPLE', 'COMPLEX');

-- NOTE: Keep in sync with the same enum in postgres-new-audit.sql!
CREATE TYPE OperationResultStatusType AS ENUM ('SUCCESS', 'WARNING', 'PARTIAL_ERROR',
    'FATAL_ERROR', 'HANDLED_ERROR', 'NOT_APPLICABLE', 'IN_PROGRESS', 'UNKNOWN');

CREATE TYPE OrientationType AS ENUM ('PORTRAIT', 'LANDSCAPE');

CREATE TYPE PredefinedConfigurationType AS ENUM ( 'PRODUCTION', 'DEVELOPMENT' );

CREATE TYPE ResourceAdministrativeStateType AS ENUM ('ENABLED', 'DISABLED');

CREATE TYPE ShadowKindType AS ENUM ('ACCOUNT', 'ENTITLEMENT', 'GENERIC', 'ASSOCIATION', 'UNKNOWN');

CREATE TYPE SynchronizationSituationType AS ENUM (
    'DELETED', 'DISPUTED', 'LINKED', 'UNLINKED', 'UNMATCHED');

CREATE TYPE TaskAutoScalingModeType AS ENUM ('DISABLED', 'DEFAULT');

CREATE TYPE TaskBindingType AS ENUM ('LOOSE', 'TIGHT');

CREATE TYPE TaskExecutionStateType AS ENUM ('RUNNING', 'RUNNABLE', 'WAITING', 'SUSPENDED', 'CLOSED');

CREATE TYPE TaskRecurrenceType AS ENUM ('SINGLE', 'RECURRING');

CREATE TYPE TaskSchedulingStateType AS ENUM ('READY', 'WAITING', 'SUSPENDED', 'CLOSED');

CREATE TYPE TaskWaitingReasonType AS ENUM ('OTHER_TASKS', 'OTHER');

CREATE TYPE ThreadStopActionType AS ENUM ('RESTART', 'RESCHEDULE', 'SUSPEND', 'CLOSE');

CREATE TYPE TimeIntervalStatusType AS ENUM ('BEFORE', 'IN', 'AFTER');
-- endregion

-- region OID-pool table
-- To support gen_random_uuid() pgcrypto extension must be enabled for the database (not for PG 13).
-- select * from pg_available_extensions order by name;
DO $$
BEGIN
    PERFORM pg_get_functiondef('gen_random_uuid()'::regprocedure);
    RAISE NOTICE 'gen_random_uuid already exists, skipping create EXTENSION pgcrypto';
EXCEPTION WHEN undefined_function THEN
    CREATE EXTENSION pgcrypto;
END
$$;

-- "OID pool", provides generated OID, can be referenced by FKs.
CREATE TABLE m_object_oid (
    oid UUID PRIMARY KEY DEFAULT gen_random_uuid()
);
-- endregion

-- region Functions/triggers
-- BEFORE INSERT trigger - must be declared on all concrete m_object sub-tables.
CREATE OR REPLACE FUNCTION insert_object_oid()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.oid IS NOT NULL THEN
        INSERT INTO m_object_oid VALUES (NEW.oid);
    ELSE
        INSERT INTO m_object_oid DEFAULT VALUES RETURNING oid INTO NEW.oid;
    END IF;
    -- before trigger must return NEW row to do something
    RETURN NEW;
END
$$;

-- AFTER DELETE trigger - must be declared on all concrete m_object sub-tables.
CREATE OR REPLACE FUNCTION delete_object_oid()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    delete from m_object_oid where oid = OLD.oid;
    -- after trigger returns null
    RETURN NULL;
END
$$;

-- BEFORE UPDATE trigger - must be declared on all concrete m_object sub-tables.
-- Checks that OID is not changed and updates db_modified column.
CREATE OR REPLACE FUNCTION before_update_object()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.oid = OLD.oid THEN
        NEW.db_modified = current_timestamp;
        -- must return NEW, NULL would skip the update
        RETURN NEW;
    END IF;

    -- OID changed, forbidden
    RAISE EXCEPTION 'UPDATE on "%" tried to change OID "%" to "%". OID is immutable and cannot be changed.',
        TG_TABLE_NAME, OLD.oid, NEW.oid;
END
$$;
-- endregion

-- region Enumeration/code/management tables
-- Key -> value config table for internal use.
CREATE TABLE m_global_metadata (
    name TEXT PRIMARY KEY,
    value TEXT
);

-- Catalog of often used URIs, typically channels and relation Q-names.
-- Never update values of "uri" manually to change URI for some objects
-- (unless you really want to migrate old URI to a new one).
-- URI can be anything, for QNames the format is based on QNameUtil ("prefix-url#localPart").
CREATE TABLE m_uri (
    id SERIAL NOT NULL PRIMARY KEY,
    uri TEXT NOT NULL UNIQUE
);

-- There can be more constants pre-filled, but that adds overhead, let the first-start do it.
-- Nothing in the application code should rely on anything inserted here, not even for 0=default.
-- Pinning 0 to default relation is merely for convenience when reading the DB tables.
INSERT INTO m_uri (id, uri)
    VALUES (0, 'http://midpoint.evolveum.com/xml/ns/public/common/org-3#default');
-- endregion

-- region for abstract tables m_object/container/reference
-- Purely abstract table (no entries are allowed). Represents ObjectType+ArchetypeHolderType.
-- See https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/objecttype/
-- Following is recommended for each concrete table (see m_resource for example):
-- 1) override OID like this (PK+FK): oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
-- 2) define object type class (change value as needed):
--   objectType ObjectType GENERATED ALWAYS AS ('XY') STORED CHECK (objectType = 'XY'),
--   The CHECK part helps with query optimization when the column is uses in WHERE.
-- 3) add three triggers <table_name>_oid_{insert|update|delete}_tr
-- 4) add indexes for nameOrig and nameNorm columns (nameNorm as unique)
-- 5) the rest varies on the concrete table, other indexes or constraints, etc.
-- 6) any required FK must be created on the concrete table, even for inherited columns
CREATE TABLE m_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    oid UUID NOT NULL,
    -- objectType will be overridden with GENERATED value in concrete table
    -- CHECK helps optimizer to avoid this table when different type is asked, mind that
    -- WHERE objectType = 'OBJECT' never returns anything (unlike select * from m_object).
    -- We don't want this check to be inherited as it would prevent any inserts of other types.

    -- PG16: ObjectType column will be added later, it needs to have different definition for PG < 16 and PG >= 16
    -- and it is not possible to achieve that definition with ALTER COLUMN statement
    -- objectType ObjectType NOT NULL CHECK (objectType = 'OBJECT') NO INHERIT,
    nameOrig TEXT NOT NULL,
    nameNorm TEXT NOT NULL,
    fullObject BYTEA,
    tenantRefTargetOid UUID,
    tenantRefTargetType ObjectType,
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    lifecycleState TEXT, -- TODO what is this? how many distinct values?
    cidSeq BIGINT NOT NULL DEFAULT 1, -- sequence for container id, next free cid
    version INTEGER NOT NULL DEFAULT 1,
    -- complex DB columns, add indexes as needed per concrete table, e.g. see m_user
    -- TODO compare with [] in JSONB, check performance, indexing, etc. first
    policySituations INTEGER[], -- soft-references m_uri, only EQ filter
    subtypes TEXT[], -- only EQ filter
    fullTextInfo TEXT,
    /*
    Extension items are stored as JSON key:value pairs, where key is m_ext_item.id (as string)
    and values are stored as follows (this is internal and has no effect on how query is written):
    - string and boolean are stored as-is
    - any numeric type integral/float/precise is stored as NUMERIC (JSONB can store that)
    - enum as toString() or name() of the Java enum instance
    - date-time as Instant.toString() ISO-8601 long date-timeZ (UTC), cut to 3 fraction digits
    - poly-string is stored as sub-object {"o":"orig-value","n":"norm-value"}
    - reference is stored as sub-object {"o":"oid","t":"targetType","r":"relationId"}
    - - where targetType is ObjectType and relationId is from m_uri.id, just like for ref columns
    */
    ext JSONB,
    -- metadata
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

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
);




-- Important objectType column needs to be non-generated on PG < 16 and generated on PG>=16
--
-- Before Postgres 16: if parent column was generated, child columns must use same value
-- After 16: Parent must be generated, if children are generated, they may have different generation values
do $$
declare
  pg16 int;
begin

-- Are we on Posgres 16 or newer? we cannot use VERSION() since it returns formated string
SELECT 1 FROM "pg_settings" into pg16 WHERE "name" = 'server_version_num' AND "setting" >= '160000';
  if pg16 then
       ALTER TABLE m_object ADD COLUMN objectType ObjectType GENERATED ALWAYS AS ('OBJECT') STORED NOT NULL CHECK (objectType = 'OBJECT') NO INHERIT;
    else
       -- PG 15 and lower
       ALTER TABLE m_object ADD COLUMN objectType ObjectType NOT NULL CHECK (objectType = 'OBJECT') NO INHERIT;
  end if;
end $$;




-- No indexes here, always add indexes and referential constraints on concrete sub-tables.

-- Represents AssignmentHolderType (all objects except shadows)
-- extending m_object, but still abstract, hence the CHECK (false)
CREATE TABLE m_assignment_holder (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'ASSIGNMENT_HOLDER') NO INHERIT,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_object);

-- Purely abstract table (no entries are allowed). Represents Containerable/PrismContainerValue.
-- Allows querying all separately persisted containers, but not necessary for the application.
CREATE TABLE m_container (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    -- Owner does not have to be the direct parent of the container.
    ownerOid UUID NOT NULL,
    -- use like this on the concrete table:
    -- ownerOid UUID NOT NULL REFERENCES m_object_oid(oid),

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    cid BIGINT NOT NULL,
    -- containerType will be overridden with GENERATED value in concrete table
    -- containerType will be added by ALTER because we need different definition between PG Versions
    -- containerType ContainerType NOT NULL,

    CHECK (FALSE) NO INHERIT
    -- add on concrete table (additional columns possible): PRIMARY KEY (ownerOid, cid)
);
-- Abstract reference table, for object but also other container references.
CREATE TABLE m_reference (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    ownerType ObjectType NOT NULL,
    -- referenceType will be overridden with GENERATED value in concrete table
    -- referenceType will be added by ALTER because we need different definition between PG Versions

    targetOid UUID NOT NULL, -- soft-references m_object
    targetType ObjectType NOT NULL,
    relationId INTEGER NOT NULL REFERENCES m_uri(id),

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
    -- add PK (referenceType is the same per table): PRIMARY KEY (ownerOid, relationId, targetOid)
);

-- Important: referenceType, containerType column needs to be non-generated on PG < 16 and generated on PG>=16
--
-- Before Postgres 16: if parent column was generated, child columns must use same value
-- After 16: Parent must be generated, if children are generated, they may have different generation values
do $$
declare
  pg16 int;
begin

-- Are we on Postgres 16 or newer? we cannot use VERSION() since it returns formated string
SELECT 1 FROM "pg_settings" into pg16 WHERE "name" = 'server_version_num' AND "setting" >= '160000';
  if pg16 then
       ALTER TABLE m_reference ADD COLUMN referenceType ReferenceType  GENERATED ALWAYS AS (NULL) STORED NOT NULL;
       ALTER TABLE m_container ADD COLUMN containerType ContainerType  GENERATED ALWAYS AS (NULL) STORED NOT NULL;
    else
       -- PG 15 and lower
       ALTER TABLE m_reference ADD COLUMN referenceType ReferenceType NOT NULL;
       ALTER TABLE m_container ADD COLUMN containerType ContainerType NOT NULL;
  end if;
end $$;

-- Add this index for each sub-table (reference type is not necessary, each sub-table has just one).
-- CREATE INDEX m_reference_targetOidRelationId_idx ON m_reference (targetOid, relationId);


-- references related to ObjectType and AssignmentHolderType
-- stores AssignmentHolderType/archetypeRef
CREATE TABLE m_ref_archetype (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (referenceType = 'ARCHETYPE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_archetype_targetOidRelationId_idx
    ON m_ref_archetype (targetOid, relationId);

-- stores AssignmentHolderType/delegatedRef
CREATE TABLE m_ref_delegated (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('DELEGATED') STORED
        CHECK (referenceType = 'DELEGATED'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_delegated_targetOidRelationId_idx
    ON m_ref_delegated (targetOid, relationId);

-- stores ObjectType/metadata/createApproverRef
CREATE TABLE m_ref_object_create_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_CREATE_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_CREATE_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_create_approver_targetOidRelationId_idx
    ON m_ref_object_create_approver (targetOid, relationId);


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


-- stores ObjectType/metadata/modifyApproverRef
CREATE TABLE m_ref_object_modify_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_MODIFY_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_MODIFY_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_modify_approver_targetOidRelationId_idx
    ON m_ref_object_modify_approver (targetOid, relationId);

-- stores AssignmentHolderType/roleMembershipRef
CREATE TABLE m_ref_role_membership (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ROLE_MEMBERSHIP') STORED
        CHECK (referenceType = 'ROLE_MEMBERSHIP'),
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_role_membership_targetOidRelationId_idx
    ON m_ref_role_membership (targetOid, relationId);
-- endregion

-- region FOCUS related tables
-- Represents FocusType (Users, Roles, ...), see https://docs.evolveum.com/midpoint/reference/schema/focus-and-projections/
-- extending m_object, but still abstract, hence the CHECK (false)
CREATE TABLE m_focus (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'FOCUS') NO INHERIT,
    costCenter TEXT,
    emailAddress TEXT,
    photo BYTEA, -- will be TOAST-ed if necessary
    locale TEXT,
    localityOrig TEXT,
    localityNorm TEXT,
    preferredLanguage TEXT,
    telephoneNumber TEXT,
    timezone TEXT,
    -- credential/password/metadata
    passwordCreateTimestamp TIMESTAMPTZ,
    passwordModifyTimestamp TIMESTAMPTZ,
    -- activation
    administrativeStatus ActivationStatusType,
    effectiveStatus ActivationStatusType,
    enableTimestamp TIMESTAMPTZ,
    disableTimestamp TIMESTAMPTZ,
    disableReason TEXT,
    validityStatus TimeIntervalStatusType,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    archiveTimestamp TIMESTAMPTZ,
    lockoutStatus LockoutStatusType,
    normalizedData JSONB,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_assignment_holder);

-- for each concrete sub-table indexes must be added, validFrom, validTo, etc.

-- stores FocusType/personaRef
CREATE TABLE m_ref_persona (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PERSONA') STORED
        CHECK (referenceType = 'PERSONA'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_persona_targetOidRelationId_idx
    ON m_ref_persona (targetOid, relationId);

-- stores FocusType/linkRef ("projection" is newer and better term)
CREATE TABLE m_ref_projection (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PROJECTION') STORED
        CHECK (referenceType = 'PROJECTION'),
    fullObject BYTEA,
    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_projection_targetOidRelationId_idx
    ON m_ref_projection (targetOid, relationId);

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

-- Represents GenericObjectType, see https://docs.evolveum.com/midpoint/reference/schema/generic-objects/
CREATE TABLE m_generic_object (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('GENERIC_OBJECT') STORED
        CHECK (objectType = 'GENERIC_OBJECT')
)
    INHERITS (m_focus);

CREATE TRIGGER m_generic_object_oid_insert_tr BEFORE INSERT ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_generic_object_update_tr BEFORE UPDATE ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_generic_object_oid_delete_tr AFTER DELETE ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_generic_object_nameOrig_idx ON m_generic_object (nameOrig);
CREATE UNIQUE INDEX m_generic_object_nameNorm_key ON m_generic_object (nameNorm);
CREATE INDEX m_generic_object_subtypes_idx ON m_generic_object USING gin(subtypes);
CREATE INDEX m_generic_object_validFrom_idx ON m_generic_object (validFrom);
CREATE INDEX m_generic_object_validTo_idx ON m_generic_object (validTo);
CREATE INDEX m_generic_object_fullTextInfo_idx
    ON m_generic_object USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_generic_object_createTimestamp_idx ON m_generic_object (createTimestamp);
CREATE INDEX m_generic_object_modifyTimestamp_idx ON m_generic_object (modifyTimestamp);
-- endregion

-- region USER related tables
-- Represents UserType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/usertype/
CREATE TABLE m_user (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('USER') STORED
        CHECK (objectType = 'USER'),
    additionalNameOrig TEXT,
    additionalNameNorm TEXT,
    employeeNumber TEXT,
    familyNameOrig TEXT,
    familyNameNorm TEXT,
    fullNameOrig TEXT,
    fullNameNorm TEXT,
    givenNameOrig TEXT,
    givenNameNorm TEXT,
    honorificPrefixOrig TEXT,
    honorificPrefixNorm TEXT,
    honorificSuffixOrig TEXT,
    honorificSuffixNorm TEXT,
    nickNameOrig TEXT,
    nickNameNorm TEXT,
    personalNumber TEXT,
    titleOrig TEXT,
    titleNorm TEXT,
    organizations JSONB, -- array of {o,n} objects (poly-strings)
    organizationUnits JSONB -- array of {o,n} objects (poly-strings)
)
    INHERITS (m_focus);

CREATE TRIGGER m_user_oid_insert_tr BEFORE INSERT ON m_user
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_user_update_tr BEFORE UPDATE ON m_user
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_user_oid_delete_tr AFTER DELETE ON m_user
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_user_nameOrig_idx ON m_user (nameOrig);
CREATE UNIQUE INDEX m_user_nameNorm_key ON m_user (nameNorm);
CREATE INDEX m_user_policySituation_idx ON m_user USING gin(policysituations gin__int_ops);
CREATE INDEX m_user_ext_idx ON m_user USING gin(ext);
CREATE INDEX m_user_fullNameOrig_idx ON m_user (fullNameOrig);
CREATE INDEX m_user_familyNameOrig_idx ON m_user (familyNameOrig);
CREATE INDEX m_user_givenNameOrig_idx ON m_user (givenNameOrig);
CREATE INDEX m_user_employeeNumber_idx ON m_user (employeeNumber);
CREATE INDEX m_user_subtypes_idx ON m_user USING gin(subtypes);
CREATE INDEX m_user_organizations_idx ON m_user USING gin(organizations);
CREATE INDEX m_user_organizationUnits_idx ON m_user USING gin(organizationUnits);
CREATE INDEX m_user_validFrom_idx ON m_user (validFrom);
CREATE INDEX m_user_validTo_idx ON m_user (validTo);
CREATE INDEX m_user_fullTextInfo_idx ON m_user USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_user_createTimestamp_idx ON m_user (createTimestamp);
CREATE INDEX m_user_modifyTimestamp_idx ON m_user (modifyTimestamp);
-- endregion

-- region ROLE related tables
-- Represents AbstractRoleType, see https://docs.evolveum.com/midpoint/architecture/concepts/abstract-role/
CREATE TABLE m_abstract_role (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'ABSTRACT_ROLE') NO INHERIT,
    autoAssignEnabled BOOLEAN,
    displayNameOrig TEXT,
    displayNameNorm TEXT,
    identifier TEXT,
    requestable BOOLEAN,
    riskLevel TEXT,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_focus);

/*
TODO: add for sub-tables, role, org... all? how many services?
 identifier is OK (TEXT), but booleans are useless unless used in WHERE
CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);
CREATE INDEX iRequestable ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled ON m_abstract_role(autoassign_enabled);
*/

-- Represents RoleType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/roletype/
CREATE TABLE m_role (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE') STORED
        CHECK (objectType = 'ROLE')
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_role_oid_insert_tr BEFORE INSERT ON m_role
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_role_update_tr BEFORE UPDATE ON m_role
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_role_oid_delete_tr AFTER DELETE ON m_role
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_role_nameOrig_idx ON m_role (nameOrig);
CREATE UNIQUE INDEX m_role_nameNorm_key ON m_role (nameNorm);
CREATE INDEX m_role_subtypes_idx ON m_role USING gin(subtypes);
CREATE INDEX m_role_identifier_idx ON m_role (identifier);
CREATE INDEX m_role_validFrom_idx ON m_role (validFrom);
CREATE INDEX m_role_validTo_idx ON m_role (validTo);
CREATE INDEX m_role_fullTextInfo_idx ON m_role USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_role_createTimestamp_idx ON m_role (createTimestamp);
CREATE INDEX m_role_modifyTimestamp_idx ON m_role (modifyTimestamp);


-- Represents PolicyType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/policytype/
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



-- Represents ServiceType, see https://docs.evolveum.com/midpoint/reference/deployment/service-account-management/
CREATE TABLE m_service (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SERVICE') STORED
        CHECK (objectType = 'SERVICE'),
    displayOrder INTEGER
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_service_oid_insert_tr BEFORE INSERT ON m_service
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_service_update_tr BEFORE UPDATE ON m_service
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_service_oid_delete_tr AFTER DELETE ON m_service
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_service_nameOrig_idx ON m_service (nameOrig);
CREATE UNIQUE INDEX m_service_nameNorm_key ON m_service (nameNorm);
CREATE INDEX m_service_subtypes_idx ON m_service USING gin(subtypes);
CREATE INDEX m_service_identifier_idx ON m_service (identifier);
CREATE INDEX m_service_validFrom_idx ON m_service (validFrom);
CREATE INDEX m_service_validTo_idx ON m_service (validTo);
CREATE INDEX m_service_fullTextInfo_idx ON m_service USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_service_createTimestamp_idx ON m_service (createTimestamp);
CREATE INDEX m_service_modifyTimestamp_idx ON m_service (modifyTimestamp);

-- Represents ArchetypeType, see https://docs.evolveum.com/midpoint/reference/schema/archetypes/
CREATE TABLE m_archetype (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (objectType = 'ARCHETYPE')
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_archetype_oid_insert_tr BEFORE INSERT ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_archetype_update_tr BEFORE UPDATE ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_archetype_oid_delete_tr AFTER DELETE ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_archetype_nameOrig_idx ON m_archetype (nameOrig);
CREATE UNIQUE INDEX m_archetype_nameNorm_key ON m_archetype (nameNorm);
CREATE INDEX m_archetype_subtypes_idx ON m_archetype USING gin(subtypes);
CREATE INDEX m_archetype_identifier_idx ON m_archetype (identifier);
CREATE INDEX m_archetype_validFrom_idx ON m_archetype (validFrom);
CREATE INDEX m_archetype_validTo_idx ON m_archetype (validTo);
CREATE INDEX m_archetype_fullTextInfo_idx ON m_archetype USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_archetype_createTimestamp_idx ON m_archetype (createTimestamp);
CREATE INDEX m_archetype_modifyTimestamp_idx ON m_archetype (modifyTimestamp);
-- endregion

-- region Organization hierarchy support
-- Represents OrgType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/orgtype/
CREATE TABLE m_org (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ORG') STORED
        CHECK (objectType = 'ORG'),
    displayOrder INTEGER,
    tenant BOOLEAN
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_org_oid_insert_tr BEFORE INSERT ON m_org
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_org_update_tr BEFORE UPDATE ON m_org
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_org_oid_delete_tr AFTER DELETE ON m_org
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_org_nameOrig_idx ON m_org (nameOrig);
CREATE UNIQUE INDEX m_org_nameNorm_key ON m_org (nameNorm);
CREATE INDEX m_org_displayOrder_idx ON m_org (displayOrder);
CREATE INDEX m_org_subtypes_idx ON m_org USING gin(subtypes);
CREATE INDEX m_org_identifier_idx ON m_org (identifier);
CREATE INDEX m_org_validFrom_idx ON m_org (validFrom);
CREATE INDEX m_org_validTo_idx ON m_org (validTo);
CREATE INDEX m_org_fullTextInfo_idx ON m_org USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_org_createTimestamp_idx ON m_org (createTimestamp);
CREATE INDEX m_org_modifyTimestamp_idx ON m_org (modifyTimestamp);

-- stores ObjectType/parentOrgRef
CREATE TABLE m_ref_object_parent_org (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_PARENT_ORG') STORED
        CHECK (referenceType = 'OBJECT_PARENT_ORG'),

    -- TODO wouldn't (ownerOid, targetOid, relationId) perform better for typical queries?
    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_parent_org_targetOidRelationId_idx
    ON m_ref_object_parent_org (targetOid, relationId);

-- region org-closure
/*
Trigger on m_ref_object_parent_org marks this view for refresh in one m_global_metadata row.
Closure contains also identity (org = org) entries because:
* It's easier to do optimized matrix-multiplication based refresh with them later.
* It actually makes some query easier and requires AND instead of OR conditions.
* While the table shows that o => o (=> means "is parent of"), this is not the semantics
of isParent/ChildOf searches and they never return parameter OID as a result.
*/
CREATE MATERIALIZED VIEW m_org_closure AS
WITH RECURSIVE org_h (
    ancestor_oid, -- ref.targetoid
    descendant_oid --ref.ownerOid
    -- paths -- number of different paths, not used for materialized view version
    -- depth -- possible later, but cycle detected must be added to the recursive term
) AS (
    -- non-recursive term:
    -- Gather all organization oids from parent-org refs and initialize identity lines (o => o).
    -- We don't want the orgs not in org hierarchy, that would require org triggers too.
    SELECT o.oid, o.oid FROM m_org o
        WHERE EXISTS(
            SELECT 1 FROM m_ref_object_parent_org r
                WHERE r.targetOid = o.oid OR r.ownerOid = o.oid)
    UNION
    -- recursive (iterative) term:
    -- Generate their parents (anc => desc, that is target => owner), => means "is parent of".
    SELECT par.targetoid, chi.descendant_oid -- leaving original child there generates closure
        FROM m_ref_object_parent_org as par, org_h as chi
        WHERE par.ownerOid = chi.ancestor_oid
)
SELECT * FROM org_h;

-- unique index is like PK if it was table
CREATE UNIQUE INDEX m_org_closure_asc_desc_idx
    ON m_org_closure (ancestor_oid, descendant_oid);
CREATE INDEX m_org_closure_desc_asc_idx
    ON m_org_closure (descendant_oid, ancestor_oid);

-- The trigger for m_ref_object_parent_org that flags the view for refresh.
CREATE OR REPLACE FUNCTION mark_org_closure_for_refresh()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'TRUNCATE' OR OLD.ownerType = 'ORG' OR NEW.ownerType = 'ORG' THEN
        INSERT INTO m_global_metadata VALUES ('orgClosureRefreshNeeded', 'true')
            ON CONFLICT (name) DO UPDATE SET value = 'true';
    END IF;

    -- after trigger returns null
    RETURN NULL;
END $$;

CREATE TRIGGER m_ref_object_parent_mark_refresh_tr
    AFTER INSERT OR UPDATE OR DELETE ON m_ref_object_parent_org
    FOR EACH ROW EXECUTE FUNCTION mark_org_closure_for_refresh();
CREATE TRIGGER m_ref_object_parent_mark_refresh_trunc_tr
    AFTER TRUNCATE ON m_ref_object_parent_org
    FOR EACH STATEMENT EXECUTE FUNCTION mark_org_closure_for_refresh();

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

-- This procedure for conditional refresh when needed is called from the application code.
-- The refresh can be forced, e.g. after many changes with triggers off (or just to be sure).
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
-- endregion

-- region OTHER object tables
-- Represents ResourceType, see https://docs.evolveum.com/midpoint/reference/resources/resource-configuration/
CREATE TABLE m_resource (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('RESOURCE') STORED
        CHECK (objectType = 'RESOURCE'),
    businessAdministrativeState ResourceAdministrativeStateType,
    -- administrativeOperationalState/administrativeAvailabilityStatus
    administrativeOperationalStateAdministrativeAvailabilityStatus AdministrativeAvailabilityStatusType,
    -- operationalState/lastAvailabilityStatus
    operationalStateLastAvailabilityStatus AvailabilityStatusType,
    connectorRefTargetOid UUID,
    connectorRefTargetType ObjectType,
    connectorRefRelationId INTEGER REFERENCES m_uri(id),
    template BOOLEAN,
    abstract BOOLEAN,
    superRefTargetOid UUID,
    superRefTargetType ObjectType,
    superRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_resource_oid_insert_tr BEFORE INSERT ON m_resource
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_resource_update_tr BEFORE UPDATE ON m_resource
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_resource_oid_delete_tr AFTER DELETE ON m_resource
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_resource_nameOrig_idx ON m_resource (nameOrig);
CREATE UNIQUE INDEX m_resource_nameNorm_key ON m_resource (nameNorm);
CREATE INDEX m_resource_subtypes_idx ON m_resource USING gin(subtypes);
CREATE INDEX m_resource_fullTextInfo_idx ON m_resource USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_resource_createTimestamp_idx ON m_resource (createTimestamp);
CREATE INDEX m_resource_modifyTimestamp_idx ON m_resource (modifyTimestamp);

-- stores ResourceType/business/approverRef
CREATE TABLE m_ref_resource_business_configuration_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS
        ('RESOURCE_BUSINESS_CONFIGURATION_APPROVER') STORED,

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_resource_biz_config_approver_targetOidRelationId_idx
    ON m_ref_resource_business_configuration_approver (targetOid, relationId);

-- Represents ShadowType, see https://docs.evolveum.com/midpoint/reference/resources/shadow/
-- and also https://docs.evolveum.com/midpoint/reference/schema/focus-and-projections/
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
    correlationSituation CorrelationSituationType,
    disableReasonId INTEGER REFERENCES m_uri(id),
    enableTimestamp TIMESTAMPTZ,
    disableTimestamp TIMESTAMPTZ,
    lastLoginTimestamp TIMESTAMPTZ

) PARTITION BY LIST (resourceRefTargetOid);

CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_shadow_update_tr BEFORE UPDATE ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();


CREATE TABLE m_shadow_default PARTITION OF m_shadow DEFAULT;
ALTER TABLE m_shadow_default ADD PRIMARY KEY (oid);

CREATE INDEX m_shadow_default_nameOrig_idx ON m_shadow_default (nameOrig);
CREATE INDEX m_shadow_default_nameNorm_idx ON m_shadow_default (nameNorm); -- may not be unique for shadows!
CREATE UNIQUE INDEX m_shadow_default_primIdVal_objCls_resRefOid_key
    ON m_shadow_default (primaryIdentifierValue, objectClassId, resourceRefTargetOid);

CREATE INDEX m_shadow_default_subtypes_idx ON m_shadow_default USING gin(subtypes);
CREATE INDEX m_shadow_default_policySituation_idx ON m_shadow_default USING gin(policysituations gin__int_ops);
CREATE INDEX m_shadow_default_ext_idx ON m_shadow_default USING gin(ext);
CREATE INDEX m_shadow_default_attributes_idx ON m_shadow_default USING gin(attributes);
CREATE INDEX m_shadow_default_fullTextInfo_idx ON m_shadow_default USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_shadow_default_resourceRefTargetOid_idx ON m_shadow_default (resourceRefTargetOid);
CREATE INDEX m_shadow_default_createTimestamp_idx ON m_shadow_default (createTimestamp);
CREATE INDEX m_shadow_default_modifyTimestamp_idx ON m_shadow_default (modifyTimestamp);
CREATE INDEX m_shadow_default_correlationStartTimestamp_idx ON m_shadow_default (correlationStartTimestamp);
CREATE INDEX m_shadow_default_correlationEndTimestamp_idx ON m_shadow_default (correlationEndTimestamp);
CREATE INDEX m_shadow_default_correlationCaseOpenTimestamp_idx ON m_shadow_default (correlationCaseOpenTimestamp);
CREATE INDEX m_shadow_default_correlationCaseCloseTimestamp_idx ON m_shadow_default (correlationCaseCloseTimestamp);



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
    EXECUTE format('ALTER TABLE %I DISABLE TRIGGER m_shadow_oid_delete_tr;', sourceTable);
      IF new.objectClassId IS NULL THEN
        EXECUTE format('DELETE FROM %I
            where resourceRefTargetOid = ''%s''', sourceTable, new.resourceOid);
      ELSE
        EXECUTE format('DELETE FROM %I
            where resourceRefTargetOid = ''%s'' AND objectClassId = %s', sourceTable, new.resourceOid, new.objectClassId);
      END IF;
      /* Reenable triggers in original table */
      EXECUTE format('ALTER TABLE %I ENABLE TRIGGER m_shadow_oid_delete_tr;', sourceTable);

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

CREATE TRIGGER "m_shadow_partition_def_bi" BEFORE INSERT ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_create_partition();;
CREATE TRIGGER "m_shadow_partition_def_bu" BEFORE UPDATE ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_update_partition();;
CREATE TRIGGER "m_shadow_partition_def_bd" BEFORE DELETE ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_delete_partition();;




/*
TODO: reconsider, especially boolean things like dead (perhaps WHERE in other indexes?)
CREATE INDEX iShadowDead ON m_shadow (dead);
CREATE INDEX iShadowKind ON m_shadow (kind);
CREATE INDEX iShadowIntent ON m_shadow (intent);
CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);
CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);
CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);
CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);
*/

-- We can now create m_object_view which will join m_shadow and m_object into single view
-- Necessary for .. in queries and searches by ObjectType

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


-- Represents shadowType/referenceAttributes/[name] ObjectReferenceTypes
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


-- Represents NodeType, see https://docs.evolveum.com/midpoint/reference/deployment/clustering-ha/managing-cluster-nodes/
CREATE TABLE m_node (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('NODE') STORED
        CHECK (objectType = 'NODE'),
    nodeIdentifier TEXT,
    operationalState NodeOperationalStateType
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_node_oid_insert_tr BEFORE INSERT ON m_node
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_node_update_tr BEFORE UPDATE ON m_node
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_node_oid_delete_tr AFTER DELETE ON m_node
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_node_nameOrig_idx ON m_node (nameOrig);
CREATE UNIQUE INDEX m_node_nameNorm_key ON m_node (nameNorm);
-- not interested in other indexes for this one, this table will be small

-- Represents SystemConfigurationType, see https://docs.evolveum.com/midpoint/reference/concepts/system-configuration-object/
CREATE TABLE m_system_configuration (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SYSTEM_CONFIGURATION') STORED
        CHECK (objectType = 'SYSTEM_CONFIGURATION')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_system_configuration_oid_insert_tr BEFORE INSERT ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_system_configuration_update_tr BEFORE UPDATE ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_system_configuration_oid_delete_tr AFTER DELETE ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE UNIQUE INDEX m_system_configuration_nameNorm_key ON m_system_configuration (nameNorm);
-- no need for the name index, m_system_configuration table is very small

-- Represents SecurityPolicyType, see https://docs.evolveum.com/midpoint/reference/security/security-policy/
CREATE TABLE m_security_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SECURITY_POLICY') STORED
        CHECK (objectType = 'SECURITY_POLICY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_security_policy_oid_insert_tr BEFORE INSERT ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_security_policy_update_tr BEFORE UPDATE ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_security_policy_oid_delete_tr AFTER DELETE ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_security_policy_nameOrig_idx ON m_security_policy (nameOrig);
CREATE UNIQUE INDEX m_security_policy_nameNorm_key ON m_security_policy (nameNorm);
CREATE INDEX m_security_policy_subtypes_idx ON m_security_policy USING gin(subtypes);
CREATE INDEX m_security_policy_policySituation_idx
    ON m_security_policy USING gin(policysituations gin__int_ops);
CREATE INDEX m_security_policy_fullTextInfo_idx
    ON m_security_policy USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_security_policy_createTimestamp_idx ON m_security_policy (createTimestamp);
CREATE INDEX m_security_policy_modifyTimestamp_idx ON m_security_policy (modifyTimestamp);

-- Represents ObjectCollectionType, see https://docs.evolveum.com/midpoint/reference/admin-gui/collections-views/configuration/
CREATE TABLE m_object_collection (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_COLLECTION') STORED
        CHECK (objectType = 'OBJECT_COLLECTION')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_object_collection_oid_insert_tr BEFORE INSERT ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_object_collection_update_tr BEFORE UPDATE ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_object_collection_oid_delete_tr AFTER DELETE ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_object_collection_nameOrig_idx ON m_object_collection (nameOrig);
CREATE UNIQUE INDEX m_object_collection_nameNorm_key ON m_object_collection (nameNorm);
CREATE INDEX m_object_collection_subtypes_idx ON m_object_collection USING gin(subtypes);
CREATE INDEX m_object_collection_policySituation_idx
    ON m_object_collection USING gin(policysituations gin__int_ops);
CREATE INDEX m_object_collection_fullTextInfo_idx
    ON m_object_collection USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_object_collection_createTimestamp_idx ON m_object_collection (createTimestamp);
CREATE INDEX m_object_collection_modifyTimestamp_idx ON m_object_collection (modifyTimestamp);

-- Represents DashboardType, see https://docs.evolveum.com/midpoint/reference/admin-gui/dashboards/configuration/
CREATE TABLE m_dashboard (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('DASHBOARD') STORED
        CHECK (objectType = 'DASHBOARD')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_dashboard_oid_insert_tr BEFORE INSERT ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_dashboard_update_tr BEFORE UPDATE ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_dashboard_oid_delete_tr AFTER DELETE ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_dashboard_nameOrig_idx ON m_dashboard (nameOrig);
CREATE UNIQUE INDEX m_dashboard_nameNorm_key ON m_dashboard (nameNorm);
CREATE INDEX m_dashboard_subtypes_idx ON m_dashboard USING gin(subtypes);
CREATE INDEX m_dashboard_policySituation_idx
    ON m_dashboard USING gin(policysituations gin__int_ops);
CREATE INDEX m_dashboard_createTimestamp_idx ON m_dashboard (createTimestamp);
CREATE INDEX m_dashboard_modifyTimestamp_idx ON m_dashboard (modifyTimestamp);

-- Represents ValuePolicyType
CREATE TABLE m_value_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('VALUE_POLICY') STORED
        CHECK (objectType = 'VALUE_POLICY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_value_policy_oid_insert_tr BEFORE INSERT ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_value_policy_update_tr BEFORE UPDATE ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_value_policy_oid_delete_tr AFTER DELETE ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_value_policy_nameOrig_idx ON m_value_policy (nameOrig);
CREATE UNIQUE INDEX m_value_policy_nameNorm_key ON m_value_policy (nameNorm);
CREATE INDEX m_value_policy_subtypes_idx ON m_value_policy USING gin(subtypes);
CREATE INDEX m_value_policy_policySituation_idx
    ON m_value_policy USING gin(policysituations gin__int_ops);
CREATE INDEX m_value_policy_createTimestamp_idx ON m_value_policy (createTimestamp);
CREATE INDEX m_value_policy_modifyTimestamp_idx ON m_value_policy (modifyTimestamp);

-- Represents ReportType, see https://docs.evolveum.com/midpoint/reference/misc/reports/report-configuration/
CREATE TABLE m_report (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT') STORED
        CHECK (objectType = 'REPORT')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_report_oid_insert_tr BEFORE INSERT ON m_report
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_report_update_tr BEFORE UPDATE ON m_report
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_report_oid_delete_tr AFTER DELETE ON m_report
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_report_nameOrig_idx ON m_report (nameOrig);
CREATE UNIQUE INDEX m_report_nameNorm_key ON m_report (nameNorm);
CREATE INDEX m_report_subtypes_idx ON m_report USING gin(subtypes);
CREATE INDEX m_report_policySituation_idx ON m_report USING gin(policysituations gin__int_ops);
CREATE INDEX m_report_createTimestamp_idx ON m_report (createTimestamp);
CREATE INDEX m_report_modifyTimestamp_idx ON m_report (modifyTimestamp);

-- Represents ReportDataType, see also m_report above
CREATE TABLE m_report_data (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT_DATA') STORED
        CHECK (objectType = 'REPORT_DATA'),
    reportRefTargetOid UUID,
    reportRefTargetType ObjectType,
    reportRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_report_data_oid_insert_tr BEFORE INSERT ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_report_data_update_tr BEFORE UPDATE ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_report_data_oid_delete_tr AFTER DELETE ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_report_data_nameOrig_idx ON m_report_data (nameOrig);
CREATE INDEX m_report_data_nameNorm_idx ON m_report_data (nameNorm); -- not unique
CREATE INDEX m_report_data_subtypes_idx ON m_report_data USING gin(subtypes);
CREATE INDEX m_report_data_policySituation_idx
    ON m_report_data USING gin(policysituations gin__int_ops);
CREATE INDEX m_report_data_createTimestamp_idx ON m_report_data (createTimestamp);
CREATE INDEX m_report_data_modifyTimestamp_idx ON m_report_data (modifyTimestamp);


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

CREATE TABLE m_role_analysis_outlier (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_OUTLIER') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_OUTLIER'),
        targetObjectRefTargetOid UUID,
        targetObjectRefTargetType ObjectType,
        targetObjectRefRelationId INTEGER REFERENCES m_uri(id),
        overallConfidence double precision
)
    INHERITS (m_assignment_holder);

CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetOid_idx ON m_role_analysis_outlier (targetObjectRefTargetOid);
CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetType_idx ON m_role_analysis_outlier (targetObjectRefTargetType);
CREATE INDEX m_role_analysis_outlier_targetObjectRefRelationId_idx ON m_role_analysis_outlier (targetObjectRefRelationId);
CREATE INDEX m_role_analysis_outlier_overallConfidence_idx ON m_role_analysis_outlier (overallConfidence);

CREATE TRIGGER m_role_analysis_outlier_oid_insert_tr BEFORE INSERT ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_role_analysis_outlier_update_tr BEFORE UPDATE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_role_analysis_outlier_oid_delete_tr AFTER DELETE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();


    CREATE TABLE m_role_analysis_outlier_partition (
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        containerType ContainerType GENERATED ALWAYS AS ('OUTLIER_PARTITION') STORED
            CHECK (containerType = 'OUTLIER_PARTITION'),
        ---
        clusterRefOid UUID,
        clusterRefTargetType ObjectType,
        clusterRefRelationId INTEGER REFERENCES m_uri(id),
        overallConfidence double precision,
        PRIMARY KEY (ownerOid, cid)
    )
        INHERITS(m_container);

CREATE INDEX m_role_analysis_outlier_partition_clusterRefOid_idx ON m_role_analysis_outlier_partition (clusterRefOid);
CREATE INDEX  m_role_analysis_outlier_partition_overallConfidence_idx ON m_role_analysis_outlier_partition (overallConfidence);


-- Represents LookupTableType, see https://docs.evolveum.com/midpoint/reference/misc/lookup-tables/
CREATE TABLE m_lookup_table (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('LOOKUP_TABLE') STORED
        CHECK (objectType = 'LOOKUP_TABLE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_lookup_table_oid_insert_tr BEFORE INSERT ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_lookup_table_update_tr BEFORE UPDATE ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_lookup_table_oid_delete_tr AFTER DELETE ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_lookup_table_nameOrig_idx ON m_lookup_table (nameOrig);
CREATE UNIQUE INDEX m_lookup_table_nameNorm_key ON m_lookup_table (nameNorm);
CREATE INDEX m_lookup_table_subtypes_idx ON m_lookup_table USING gin(subtypes);
CREATE INDEX m_lookup_table_policySituation_idx
    ON m_lookup_table USING gin(policysituations gin__int_ops);
CREATE INDEX m_lookup_table_createTimestamp_idx ON m_lookup_table (createTimestamp);
CREATE INDEX m_lookup_table_modifyTimestamp_idx ON m_lookup_table (modifyTimestamp);

-- Represents LookupTableRowType, see also m_lookup_table above
CREATE TABLE m_lookup_table_row (
    ownerOid UUID NOT NULL REFERENCES m_lookup_table(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('LOOKUP_TABLE_ROW') STORED
        CHECK (containerType = 'LOOKUP_TABLE_ROW'),
    key TEXT,
    value TEXT,
    labelOrig TEXT,
    labelNorm TEXT,
    lastChangeTimestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE UNIQUE INDEX m_lookup_table_row_ownerOid_key_key ON m_lookup_table_row (ownerOid, key);

-- Represents ConnectorType, see https://docs.evolveum.com/connectors/connectors/
CREATE TABLE m_connector (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR') STORED
        CHECK (objectType = 'CONNECTOR'),
    connectorBundle TEXT, -- typically a package name
    connectorType TEXT NOT NULL, -- typically a class name
    connectorVersion TEXT NOT NULL,
    frameworkId INTEGER REFERENCES m_uri(id),
    connectorHostRefTargetOid UUID,
    connectorHostRefTargetType ObjectType,
    connectorHostRefRelationId INTEGER REFERENCES m_uri(id),
    displayNameOrig TEXT,
    displayNameNorm TEXT,
    targetSystemTypes INTEGER[],
    available BOOLEAN
)
    INHERITS (m_assignment_holder);





CREATE TRIGGER m_connector_oid_insert_tr BEFORE INSERT ON m_connector
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_connector_update_tr BEFORE UPDATE ON m_connector
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_connector_oid_delete_tr AFTER DELETE ON m_connector
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE UNIQUE INDEX m_connector_typeVersion_key
    ON m_connector (connectorType, connectorVersion)
    WHERE connectorHostRefTargetOid IS NULL;
CREATE UNIQUE INDEX m_connector_typeVersionHost_key
    ON m_connector (connectorType, connectorVersion, connectorHostRefTargetOid)
    WHERE connectorHostRefTargetOid IS NOT NULL;
CREATE INDEX m_connector_nameOrig_idx ON m_connector (nameOrig);
CREATE INDEX m_connector_nameNorm_idx ON m_connector (nameNorm);
CREATE INDEX m_connector_subtypes_idx ON m_connector USING gin(subtypes);
CREATE INDEX m_connector_policySituation_idx
    ON m_connector USING gin(policysituations gin__int_ops);
CREATE INDEX m_connector_createTimestamp_idx ON m_connector (createTimestamp);
CREATE INDEX m_connector_modifyTimestamp_idx ON m_connector (modifyTimestamp);

-- Represents ConnectorHostType, see https://docs.evolveum.com/connectors/connid/1.x/connector-server/
CREATE TABLE m_connector_host (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_HOST') STORED
        CHECK (objectType = 'CONNECTOR_HOST'),
    hostname TEXT,
    port TEXT
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_connector_host_oid_insert_tr BEFORE INSERT ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_connector_host_update_tr BEFORE UPDATE ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_connector_host_oid_delete_tr AFTER DELETE ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_connector_host_nameOrig_idx ON m_connector_host (nameOrig);
CREATE UNIQUE INDEX m_connector_host_nameNorm_key ON m_connector_host (nameNorm);
CREATE INDEX m_connector_host_subtypes_idx ON m_connector_host USING gin(subtypes);
CREATE INDEX m_connector_host_policySituation_idx
    ON m_connector_host USING gin(policysituations gin__int_ops);
CREATE INDEX m_connector_host_createTimestamp_idx ON m_connector_host (createTimestamp);
CREATE INDEX m_connector_host_modifyTimestamp_idx ON m_connector_host (modifyTimestamp);

-- Represents persistent TaskType, see https://docs.evolveum.com/midpoint/reference/tasks/task-manager/
CREATE TABLE m_task (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('TASK') STORED
        CHECK (objectType = 'TASK'),
    taskIdentifier TEXT,
    binding TaskBindingType,
    category TEXT, -- TODO revise, deprecated, probably can go away soon
    completionTimestamp TIMESTAMPTZ,
    executionState TaskExecutionStateType,
    -- Logically fullResult and resultStatus are related, managed by Task manager.
    fullResult BYTEA,
    resultStatus OperationResultStatusType,
    handlerUriId INTEGER REFERENCES m_uri(id),
    lastRunStartTimestamp TIMESTAMPTZ,
    lastRunFinishTimestamp TIMESTAMPTZ,
    node TEXT, -- nodeId only for information purposes
    objectRefTargetOid UUID,
    objectRefTargetType ObjectType,
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    ownerRefTargetOid UUID,
    ownerRefTargetType ObjectType,
    ownerRefRelationId INTEGER REFERENCES m_uri(id),
    parent TEXT, -- value of taskIdentifier
    recurrence TaskRecurrenceType,
    schedulingState TaskSchedulingStateType,
    autoScalingMode TaskAutoScalingModeType, -- autoScaling/mode
    threadStopAction ThreadStopActionType,
    waitingReason TaskWaitingReasonType,
    dependentTaskIdentifiers TEXT[] -- contains values of taskIdentifier
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_task_oid_insert_tr BEFORE INSERT ON m_task
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_task_update_tr BEFORE UPDATE ON m_task
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_task_oid_delete_tr AFTER DELETE ON m_task
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_task_nameOrig_idx ON m_task (nameOrig);
CREATE INDEX m_task_nameNorm_idx ON m_task (nameNorm); -- can have duplicates
CREATE INDEX m_task_parent_idx ON m_task (parent);
CREATE INDEX m_task_objectRefTargetOid_idx ON m_task(objectRefTargetOid);
CREATE UNIQUE INDEX m_task_taskIdentifier_key ON m_task (taskIdentifier);
CREATE INDEX m_task_dependentTaskIdentifiers_idx ON m_task USING gin(dependentTaskIdentifiers);
CREATE INDEX m_task_subtypes_idx ON m_task USING gin(subtypes);
CREATE INDEX m_task_policySituation_idx ON m_task USING gin(policysituations gin__int_ops);
CREATE INDEX m_task_ext_idx ON m_task USING gin(ext);
CREATE INDEX m_task_fullTextInfo_idx ON m_task USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_task_createTimestamp_idx ON m_task (createTimestamp);
CREATE INDEX m_task_modifyTimestamp_idx ON m_task (modifyTimestamp);

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
    executionMode ExecutionModeType,
    predefinedConfigurationToUse PredefinedConfigurationType,
    PRIMARY KEY (ownerOid, cid)
) INHERITS(m_container);

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
-- endregion

-- region cases
-- Represents CaseType, see https://docs.evolveum.com/midpoint/features/planned/case-management/
CREATE TABLE m_case (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CASE') STORED
        CHECK (objectType = 'CASE'),
    state TEXT,
    closeTimestamp TIMESTAMPTZ,
    objectRefTargetOid UUID,
    objectRefTargetType ObjectType,
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    parentRefTargetOid UUID,
    parentRefTargetType ObjectType,
    parentRefRelationId INTEGER REFERENCES m_uri(id),
    requestorRefTargetOid UUID,
    requestorRefTargetType ObjectType,
    requestorRefRelationId INTEGER REFERENCES m_uri(id),
    targetRefTargetOid UUID,
    targetRefTargetType ObjectType,
    targetRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_case_oid_insert_tr BEFORE INSERT ON m_case
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_case_update_tr BEFORE UPDATE ON m_case
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_case_oid_delete_tr AFTER DELETE ON m_case
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_case_nameOrig_idx ON m_case (nameOrig);
CREATE INDEX m_case_nameNorm_idx ON m_case (nameNorm);
CREATE INDEX m_case_subtypes_idx ON m_case USING gin(subtypes);
CREATE INDEX m_case_policySituation_idx ON m_case USING gin(policysituations gin__int_ops);
CREATE INDEX m_case_fullTextInfo_idx ON m_case USING gin(fullTextInfo gin_trgm_ops);

CREATE INDEX m_case_objectRefTargetOid_idx ON m_case(objectRefTargetOid);
CREATE INDEX m_case_targetRefTargetOid_idx ON m_case(targetRefTargetOid);
CREATE INDEX m_case_parentRefTargetOid_idx ON m_case(parentRefTargetOid);
CREATE INDEX m_case_requestorRefTargetOid_idx ON m_case(requestorRefTargetOid);
CREATE INDEX m_case_closeTimestamp_idx ON m_case(closeTimestamp);
CREATE INDEX m_case_createTimestamp_idx ON m_case (createTimestamp);
CREATE INDEX m_case_modifyTimestamp_idx ON m_case (modifyTimestamp);

CREATE TABLE m_case_wi (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('CASE_WORK_ITEM') STORED
        CHECK (containerType = 'CASE_WORK_ITEM'),
    closeTimestamp TIMESTAMPTZ,
    createTimestamp TIMESTAMPTZ,
    deadline TIMESTAMPTZ,
    originalAssigneeRefTargetOid UUID,
    originalAssigneeRefTargetType ObjectType,
    originalAssigneeRefRelationId INTEGER REFERENCES m_uri(id),
    outcome TEXT, -- stores workitem/output/outcome
    performerRefTargetOid UUID,
    performerRefTargetType ObjectType,
    performerRefRelationId INTEGER REFERENCES m_uri(id),
    stageNumber INTEGER,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_case_wi_createTimestamp_idx ON m_case_wi (createTimestamp);
CREATE INDEX m_case_wi_closeTimestamp_idx ON m_case_wi (closeTimestamp);
CREATE INDEX m_case_wi_deadline_idx ON m_case_wi (deadline);
CREATE INDEX m_case_wi_originalAssigneeRefTargetOid_idx ON m_case_wi (originalAssigneeRefTargetOid);
CREATE INDEX m_case_wi_performerRefTargetOid_idx ON m_case_wi (performerRefTargetOid);

-- stores workItem/assigneeRef
CREATE TABLE m_case_wi_assignee (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_ASSIGNEE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first three PK columns
ALTER TABLE m_case_wi_assignee ADD CONSTRAINT m_case_wi_assignee_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid)
        ON DELETE CASCADE;

CREATE INDEX m_case_wi_assignee_targetOidRelationId_idx
    ON m_case_wi_assignee (targetOid, relationId);

-- stores workItem/candidateRef
CREATE TABLE m_case_wi_candidate (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_CANDIDATE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first two PK columns
ALTER TABLE m_case_wi_candidate ADD CONSTRAINT m_case_wi_candidate_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid)
        ON DELETE CASCADE;

CREATE INDEX m_case_wi_candidate_targetOidRelationId_idx
    ON m_case_wi_candidate (targetOid, relationId);
-- endregion

-- region Access Certification object tables
-- Represents AccessCertificationDefinitionType, see https://docs.evolveum.com/midpoint/reference/roles-policies/certification/
CREATE TABLE m_access_cert_definition (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_DEFINITION') STORED
        CHECK (objectType = 'ACCESS_CERTIFICATION_DEFINITION'),
    handlerUriId INTEGER REFERENCES m_uri(id),
    lastCampaignStartedTimestamp TIMESTAMPTZ,
    lastCampaignClosedTimestamp TIMESTAMPTZ,
    ownerRefTargetOid UUID,
    ownerRefTargetType ObjectType,
    ownerRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_access_cert_definition_oid_insert_tr BEFORE INSERT ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_access_cert_definition_update_tr BEFORE UPDATE ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_access_cert_definition_oid_delete_tr AFTER DELETE ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_access_cert_definition_nameOrig_idx ON m_access_cert_definition (nameOrig);
CREATE UNIQUE INDEX m_access_cert_definition_nameNorm_key ON m_access_cert_definition (nameNorm);
CREATE INDEX m_access_cert_definition_subtypes_idx ON m_access_cert_definition USING gin(subtypes);
CREATE INDEX m_access_cert_definition_policySituation_idx
    ON m_access_cert_definition USING gin(policysituations gin__int_ops);
CREATE INDEX m_access_cert_definition_ext_idx ON m_access_cert_definition USING gin(ext);
CREATE INDEX m_access_cert_definition_fullTextInfo_idx
    ON m_access_cert_definition USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_access_cert_definition_createTimestamp_idx ON m_access_cert_definition (createTimestamp);
CREATE INDEX m_access_cert_definition_modifyTimestamp_idx ON m_access_cert_definition (modifyTimestamp);

CREATE TABLE m_access_cert_campaign (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CAMPAIGN') STORED
        CHECK (objectType = 'ACCESS_CERTIFICATION_CAMPAIGN'),
    definitionRefTargetOid UUID,
    definitionRefTargetType ObjectType,
    definitionRefRelationId INTEGER REFERENCES m_uri(id),
    endTimestamp TIMESTAMPTZ,
    handlerUriId INTEGER REFERENCES m_uri(id),
    campaignIteration INTEGER NOT NULL,
    ownerRefTargetOid UUID,
    ownerRefTargetType ObjectType,
    ownerRefRelationId INTEGER REFERENCES m_uri(id),
    stageNumber INTEGER,
    startTimestamp TIMESTAMPTZ,
    state AccessCertificationCampaignStateType
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_access_cert_campaign_oid_insert_tr BEFORE INSERT ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_access_cert_campaign_update_tr BEFORE UPDATE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_access_cert_campaign_oid_delete_tr AFTER DELETE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_access_cert_campaign_nameOrig_idx ON m_access_cert_campaign (nameOrig);
CREATE UNIQUE INDEX m_access_cert_campaign_nameNorm_key ON m_access_cert_campaign (nameNorm);
CREATE INDEX m_access_cert_campaign_subtypes_idx ON m_access_cert_campaign USING gin(subtypes);
CREATE INDEX m_access_cert_campaign_policySituation_idx
    ON m_access_cert_campaign USING gin(policysituations gin__int_ops);
CREATE INDEX m_access_cert_campaign_ext_idx ON m_access_cert_campaign USING gin(ext);
CREATE INDEX m_access_cert_campaign_fullTextInfo_idx
    ON m_access_cert_campaign USING gin(fullTextInfo gin_trgm_ops);
CREATE INDEX m_access_cert_campaign_createTimestamp_idx ON m_access_cert_campaign (createTimestamp);
CREATE INDEX m_access_cert_campaign_modifyTimestamp_idx ON m_access_cert_campaign (modifyTimestamp);

CREATE TABLE m_access_cert_case (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CASE') STORED
        CHECK (containerType = 'ACCESS_CERTIFICATION_CASE'),
    administrativeStatus ActivationStatusType,
    archiveTimestamp TIMESTAMPTZ,
    disableReason TEXT,
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus ActivationStatusType,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus TimeIntervalStatusType,
    currentStageOutcome TEXT,
    fullObject BYTEA,
    campaignIteration INTEGER NOT NULL,
    objectRefTargetOid UUID,
    objectRefTargetType ObjectType,
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    orgRefTargetOid UUID,
    orgRefTargetType ObjectType,
    orgRefRelationId INTEGER REFERENCES m_uri(id),
    outcome TEXT,
    remediedTimestamp TIMESTAMPTZ,
    currentStageDeadline TIMESTAMPTZ,
    currentStageCreateTimestamp TIMESTAMPTZ,
    stageNumber INTEGER,
    targetRefTargetOid UUID,
    targetRefTargetType ObjectType,
    targetRefRelationId INTEGER REFERENCES m_uri(id),
    tenantRefTargetOid UUID,
    tenantRefTargetType ObjectType,
    tenantRefRelationId INTEGER REFERENCES m_uri(id),

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_access_cert_case_objectRefTargetOid_idx ON m_access_cert_case (objectRefTargetOid);
CREATE INDEX m_access_cert_case_targetRefTargetOid_idx ON m_access_cert_case (targetRefTargetOid);
CREATE INDEX m_access_cert_case_tenantRefTargetOid_idx ON m_access_cert_case (tenantRefTargetOid);
CREATE INDEX m_access_cert_case_orgRefTargetOid_idx ON m_access_cert_case (orgRefTargetOid);

CREATE TABLE m_access_cert_wi (
    ownerOid UUID NOT NULL, -- PK+FK
    accessCertCaseCid INTEGER NOT NULL, -- PK+FK
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_WORK_ITEM') STORED
        CHECK (containerType = 'ACCESS_CERTIFICATION_WORK_ITEM'),
    closeTimestamp TIMESTAMPTZ,
    campaignIteration INTEGER NOT NULL,
    outcome TEXT, -- stores output/outcome
    outputChangeTimestamp TIMESTAMPTZ,
    performerRefTargetOid UUID,
    performerRefTargetType ObjectType,
    performerRefRelationId INTEGER REFERENCES m_uri(id),
    stageNumber INTEGER,

    PRIMARY KEY (ownerOid, accessCertCaseCid, cid)
)
    INHERITS(m_container);

-- indexed by first two PK columns
ALTER TABLE m_access_cert_wi
    ADD CONSTRAINT m_access_cert_wi_id_fk FOREIGN KEY (ownerOid, accessCertCaseCid)
        REFERENCES m_access_cert_case (ownerOid, cid)
        ON DELETE CASCADE;

-- stores case/workItem/assigneeRef
CREATE TABLE m_access_cert_wi_assignee (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    accessCertCaseCid INTEGER NOT NULL,
    accessCertWorkItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ACCESS_CERT_WI_ASSIGNEE') STORED,

    PRIMARY KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first two PK columns, TODO: isn't this one superfluous with the next one?
ALTER TABLE m_access_cert_wi_assignee ADD CONSTRAINT m_access_cert_wi_assignee_id_fk_case
    FOREIGN KEY (ownerOid, accessCertCaseCid) REFERENCES m_access_cert_case (ownerOid, cid)
        ON DELETE CASCADE;

-- indexed by first three PK columns
ALTER TABLE m_access_cert_wi_assignee ADD CONSTRAINT m_access_cert_wi_assignee_id_fk_wi
    FOREIGN KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid)
        REFERENCES m_access_cert_wi (ownerOid, accessCertCaseCid, cid)
        ON DELETE CASCADE;

CREATE INDEX m_access_cert_wi_assignee_targetOidRelationId_idx
    ON m_access_cert_wi_assignee (targetOid, relationId);

-- stores case/workItem/candidateRef
CREATE TABLE m_access_cert_wi_candidate (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    accessCertCaseCid INTEGER NOT NULL,
    accessCertWorkItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ACCESS_CERT_WI_CANDIDATE') STORED,

    PRIMARY KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first two PK columns, TODO: isn't this one superfluous with the next one?
ALTER TABLE m_access_cert_wi_candidate ADD CONSTRAINT m_access_cert_wi_candidate_id_fk_case
    FOREIGN KEY (ownerOid, accessCertCaseCid) REFERENCES m_access_cert_case (ownerOid, cid)
        ON DELETE CASCADE;

-- indexed by first three PK columns
ALTER TABLE m_access_cert_wi_candidate ADD CONSTRAINT m_access_cert_wi_candidate_id_fk_wi
    FOREIGN KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid)
        REFERENCES m_access_cert_wi (ownerOid, accessCertCaseCid, cid)
        ON DELETE CASCADE;

CREATE INDEX m_access_cert_wi_candidate_targetOidRelationId_idx
    ON m_access_cert_wi_candidate (targetOid, relationId);
-- endregion

-- region ObjectTemplateType
CREATE TABLE m_object_template (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_TEMPLATE') STORED
        CHECK (objectType = 'OBJECT_TEMPLATE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_object_template_oid_insert_tr BEFORE INSERT ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_object_template_update_tr BEFORE UPDATE ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_object_template_oid_delete_tr AFTER DELETE ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_object_template_nameOrig_idx ON m_object_template (nameOrig);
CREATE UNIQUE INDEX m_object_template_nameNorm_key ON m_object_template (nameNorm);
CREATE INDEX m_object_template_subtypes_idx ON m_object_template USING gin(subtypes);
CREATE INDEX m_object_template_policySituation_idx
    ON m_object_template USING gin(policysituations gin__int_ops);
CREATE INDEX m_object_template_createTimestamp_idx ON m_object_template (createTimestamp);
CREATE INDEX m_object_template_modifyTimestamp_idx ON m_object_template (modifyTimestamp);

-- stores ObjectTemplateType/includeRef
CREATE TABLE m_ref_include (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('INCLUDE') STORED
        CHECK (referenceType = 'INCLUDE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_include_targetOidRelationId_idx
    ON m_ref_include (targetOid, relationId);
-- endregion

-- region FunctionLibrary/Sequence/Form tables
-- Represents FunctionLibraryType, see https://docs.evolveum.com/midpoint/reference/expressions/function-libraries/
CREATE TABLE m_function_library (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FUNCTION_LIBRARY') STORED
        CHECK (objectType = 'FUNCTION_LIBRARY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_function_library_oid_insert_tr BEFORE INSERT ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_function_library_update_tr BEFORE UPDATE ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_function_library_oid_delete_tr AFTER DELETE ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_function_library_nameOrig_idx ON m_function_library (nameOrig);
CREATE UNIQUE INDEX m_function_library_nameNorm_key ON m_function_library (nameNorm);
CREATE INDEX m_function_library_subtypes_idx ON m_function_library USING gin(subtypes);
CREATE INDEX m_function_library_policySituation_idx
    ON m_function_library USING gin(policysituations gin__int_ops);

-- Represents SequenceType, see https://docs.evolveum.com/midpoint/reference/expressions/sequences/
CREATE TABLE m_sequence (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SEQUENCE') STORED
        CHECK (objectType = 'SEQUENCE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_sequence_oid_insert_tr BEFORE INSERT ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_sequence_update_tr BEFORE UPDATE ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_sequence_oid_delete_tr AFTER DELETE ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_sequence_nameOrig_idx ON m_sequence (nameOrig);
CREATE UNIQUE INDEX m_sequence_nameNorm_key ON m_sequence (nameNorm);
CREATE INDEX m_sequence_subtypes_idx ON m_sequence USING gin(subtypes);
CREATE INDEX m_sequence_policySituation_idx ON m_sequence USING gin(policysituations gin__int_ops);
CREATE INDEX m_sequence_createTimestamp_idx ON m_sequence (createTimestamp);
CREATE INDEX m_sequence_modifyTimestamp_idx ON m_sequence (modifyTimestamp);

-- Represents FormType, see https://docs.evolveum.com/midpoint/reference/admin-gui/custom-forms/
CREATE TABLE m_form (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FORM') STORED
        CHECK (objectType = 'FORM')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_form_oid_insert_tr BEFORE INSERT ON m_form
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
CREATE TRIGGER m_form_update_tr BEFORE UPDATE ON m_form
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
CREATE TRIGGER m_form_oid_delete_tr AFTER DELETE ON m_form
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

CREATE INDEX m_form_nameOrig_idx ON m_form (nameOrig);
CREATE UNIQUE INDEX m_form_nameNorm_key ON m_form (nameNorm);
CREATE INDEX m_form_subtypes_idx ON m_form USING gin(subtypes);
CREATE INDEX m_form_policySituation_idx ON m_form USING gin(policysituations gin__int_ops);
CREATE INDEX m_form_createTimestamp_idx ON m_form (createTimestamp);
CREATE INDEX m_form_modifyTimestamp_idx ON m_form (modifyTimestamp);
-- endregion

-- region Notification and message transport
-- Represents MessageTemplateType
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
-- endregion

-- region Assignment/Inducement table
-- Represents AssignmentType, see https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/
-- and also https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/assignment-vs-inducement/
CREATE TABLE m_assignment (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    cid BIGINT NOT NULL,
    -- this is different from other containers, this is not generated, app must provide it
    containerType ContainerType NOT NULL CHECK (containerType IN ('ASSIGNMENT', 'INDUCEMENT')),
    ownerType ObjectType NOT NULL,
    lifecycleState TEXT,
    orderValue INTEGER, -- item "order"
    orgRefTargetOid UUID,
    orgRefTargetType ObjectType,
    orgRefRelationId INTEGER REFERENCES m_uri(id),
    targetRefTargetOid UUID,
    targetRefTargetType ObjectType,
    targetRefRelationId INTEGER REFERENCES m_uri(id),
    tenantRefTargetOid UUID,
    tenantRefTargetType ObjectType,
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    policySituations INTEGER[], -- soft-references m_uri, add index per table
    subtypes TEXT[], -- only EQ filter
    ext JSONB,
    -- construction
    resourceRefTargetOid UUID,
    resourceRefTargetType ObjectType,
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    -- activation
    administrativeStatus ActivationStatusType,
    effectiveStatus ActivationStatusType,
    enableTimestamp TIMESTAMPTZ,
    disableTimestamp TIMESTAMPTZ,
    disableReason TEXT,
    validityStatus TimeIntervalStatusType,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    archiveTimestamp TIMESTAMPTZ,
    -- metadata
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
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, cid)
);

-- Assignment metadata

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

CREATE INDEX m_assignment_metadata_createTimestamp_idx ON m_assignment (createTimestamp);
CREATE INDEX m_assignment_metadata_modifyTimestamp_idx ON m_assignment (modifyTimestamp);


CREATE INDEX m_assignment_policySituation_idx
    ON m_assignment USING gin(policysituations gin__int_ops);
CREATE INDEX m_assignment_subtypes_idx ON m_assignment USING gin(subtypes);
CREATE INDEX m_assignment_ext_idx ON m_assignment USING gin(ext);
-- TODO was: CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);
-- administrativeStatus has 3 states (ENABLED/DISABLED/ARCHIVED), not sure it's worth indexing
-- but it can be used as a condition to index other (e.g. WHERE administrativeStatus='ENABLED')
-- TODO the same: CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);
CREATE INDEX m_assignment_validFrom_idx ON m_assignment (validFrom);
CREATE INDEX m_assignment_validTo_idx ON m_assignment (validTo);
CREATE INDEX m_assignment_targetRefTargetOid_idx ON m_assignment (targetRefTargetOid);
CREATE INDEX m_assignment_tenantRefTargetOid_idx ON m_assignment (tenantRefTargetOid);
CREATE INDEX m_assignment_orgRefTargetOid_idx ON m_assignment (orgRefTargetOid);
CREATE INDEX m_assignment_resourceRefTargetOid_idx ON m_assignment (resourceRefTargetOid);
CREATE INDEX m_assignment_createTimestamp_idx ON m_assignment (createTimestamp);
CREATE INDEX m_assignment_modifyTimestamp_idx ON m_assignment (modifyTimestamp);

-- stores assignment/effectiveMarkRef
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


ALTER TABLE "m_assignment_metadata"
ADD FOREIGN KEY ("owneroid", "assignmentcid") REFERENCES "m_assignment" ("owneroid", "cid") ON DELETE CASCADE;

-- stores assignment/metadata/createApproverRef
CREATE TABLE m_assignment_ref_create_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignmentCid INTEGER NOT NULL,
    metadataCid INTEGER,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_CREATE_APPROVER') STORED
        CHECK (referenceType = 'ASSIGNMENT_CREATE_APPROVER')
)
    INHERITS (m_reference);

-- indexed by first two PK columns
ALTER TABLE m_assignment_ref_create_approver ADD CONSTRAINT m_assignment_ref_create_approver_id_fk
    FOREIGN KEY (ownerOid, assignmentCid) REFERENCES m_assignment (ownerOid, cid)
        ON DELETE CASCADE;
-- table does not have primary key since metadataCid == null are original values in metadata containar
-- and metadataCid != null are value metadata references
ALTER TABLE "m_assignment_ref_create_approver" ADD CONSTRAINT "m_assignment_ref_create_approver_pkey"
  UNIQUE ("owneroid", "assignmentcid", "metadatacid", "referencetype", "relationid", "targetoid");

CREATE INDEX m_assignment_ref_create_approver_targetOidRelationId_idx
    ON m_assignment_ref_create_approver (targetOid, relationId);

-- stores assignment/metadata/modifyApproverRef
CREATE TABLE m_assignment_ref_modify_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignmentCid INTEGER NOT NULL,
    metadataCid INTEGER,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_MODIFY_APPROVER') STORED
        CHECK (referenceType = 'ASSIGNMENT_MODIFY_APPROVER')
)
    INHERITS (m_reference);

-- indexed by first three PK columns
ALTER TABLE m_assignment_ref_modify_approver ADD CONSTRAINT m_assignment_ref_modify_approver_id_fk
    FOREIGN KEY (ownerOid, assignmentCid) REFERENCES m_assignment (ownerOid, cid)
        ON DELETE CASCADE;

ALTER TABLE "m_assignment_ref_modify_approver" ADD CONSTRAINT "m_assignment_ref_modify_approver_pkey"
  UNIQUE ("owneroid", "assignmentcid", "metadatacid", "referencetype", "relationid", "targetoid");

CREATE INDEX m_assignment_ref_modify_approver_targetOidRelationId_idx
    ON m_assignment_ref_modify_approver (targetOid, relationId);
-- endregion

-- region Other object containers
-- stores ObjectType/trigger (TriggerType)
CREATE TABLE m_trigger (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('TRIGGER') STORED
        CHECK (containerType = 'TRIGGER'),
    handlerUriId INTEGER REFERENCES m_uri(id),
    timestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_trigger_timestamp_idx ON m_trigger (timestamp);

-- stores ObjectType/operationExecution (OperationExecutionType)
CREATE TABLE m_operation_execution (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('OPERATION_EXECUTION') STORED
        CHECK (containerType = 'OPERATION_EXECUTION'),
    status OperationResultStatusType,
    recordType OperationExecutionRecordTypeType,
    initiatorRefTargetOid UUID,
    initiatorRefTargetType ObjectType,
    initiatorRefRelationId INTEGER REFERENCES m_uri(id),
    taskRefTargetOid UUID,
    taskRefTargetType ObjectType,
    taskRefRelationId INTEGER REFERENCES m_uri(id),
    timestamp TIMESTAMPTZ,
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_operation_execution_initiatorRefTargetOid_idx
    ON m_operation_execution (initiatorRefTargetOid);
CREATE INDEX m_operation_execution_taskRefTargetOid_idx
    ON m_operation_execution (taskRefTargetOid);
CREATE INDEX m_operation_execution_timestamp_idx ON m_operation_execution (timestamp);
-- index for ownerOid is part of PK
-- TODO: index for status is questionable, don't we want WHERE status = ... to another index instead?
-- endregion


-- region Simulations Support

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

-- endregion

-- region Mark

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


-- endregion

-- region schema
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

-- endregion

-- region Extension support
-- Catalog table of known indexed extension items.
-- While itemName and valueType are both Q-names they are not cached via m_uri because this
-- table is small, itemName does not repeat (valueType does) and readability is also better.
-- This has similar function as m_uri - it translates something to IDs, no need to nest it.
CREATE TABLE m_ext_item (
    id SERIAL NOT NULL PRIMARY KEY,
    itemName TEXT NOT NULL,
    valueType TEXT NOT NULL,
    holderType ExtItemHolderType NOT NULL,
    cardinality ExtItemCardinality NOT NULL
    -- information about storage mechanism (JSON common/separate, column, table separate/common, etc.)
    -- storageType JSONB NOT NULL default '{"type": "EXT_JSON"}', -- currently only JSONB is used
);

-- This works fine for itemName+holderType search used in raw processing
CREATE UNIQUE INDEX m_ext_item_key ON m_ext_item (itemName, holderType, valueType, cardinality);
-- endregion

-- INDEXING:
-- More indexes is possible, but for low-variability columns like lifecycleState or administrative/effectiveStatus
-- better use them in WHERE as needed when slow query appear: https://www.postgresql.org/docs/current/indexes-partial.html
-- Also see: https://docs.evolveum.com/midpoint/reference/repository/native-postgresql/db-maintenance/

-- region Schema versioning and upgrading
/*
Procedure applying a DB schema/data change for main repository tables (audit has separate procedure).
Use sequential change numbers to identify the changes.
This protects re-execution of the same change on the same database instance.
Use dollar-quoted string constant for a change, examples are lower, docs here:
https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-DOLLAR-QUOTING
The transaction is committed if the change is executed.
The change number is NOT semantic and uses different key than original 'databaseSchemaVersion'.
Semantic schema versioning is still possible, but now only for information purposes.

Example of an DB upgrade script (stuff between $$ can be multiline, here compressed for brevity):
CALL apply_change(1, $$ create table x(a int); insert into x values (1); $$);
CALL apply_change(2, $$ alter table x add column b text; insert into x values (2, 'two'); $$);
-- not a good idea in general, but "true" forces the execution; it never updates change # to lower
CALL apply_change(1, $$ insert into x values (3, 'three'); $$, true);
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
            -- even with force we never want to set lower-or-equal change number, hence the IF above
            UPDATE m_global_metadata SET value = changeNumber WHERE name = 'schemaChangeNumber';
        ELSE
            RAISE NOTICE 'Last change number left unchanged: #%', lastChange;
        END IF;
        COMMIT;
    ELSE
        RAISE NOTICE 'Change #% skipped - not newer than the last change #%!', changeNumber, lastChange;
    END IF;
END $$;
-- endregion



-- Initializing the last change number used in postgres-new-upgrade.sql.
-- This is important to avoid applying any change more than once.
-- Also update SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER
-- repo/repo-sqale/src/main/java/com/evolveum/midpoint/repo/sqale/SqaleUtils.java
call apply_change(51, $$ SELECT 1 $$, true);
