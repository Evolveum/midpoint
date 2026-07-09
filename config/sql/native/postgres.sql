--
-- Copyright (C) 2010-2023 Evolveum and contributors
--
-- Licensed under the EUPL-1.2 or later.
--

-- Developer documentation for SQL documentation annotations:
-- https://docs.evolveum.com/midpoint/devel/guides/sql-script-annotations/

-- @formatter:off because of terribly unreliable IDEA reformat for SQL

/*
@script-description:

Naming conventions:
M_ prefix is used for tables in main part of the repo, MA_ for audit tables (can be separate)
Constraints/indexes use table_column(s)_suffix convention, with PK for primary key,
FK foreign key, IDX for index, KEY for unique index.
TR is suffix for triggers.
Names are generally lowercase (despite prefix/suffixes above in uppercase ;-)).
Column names are Java style and match attribute names from M-classes (e.g. MObject).

Other notes:
`TEXT` is used instead of `VARCHAR`, see: https://dba.stackexchange.com/a/21496/157622[dba.stackexchange]
We prefer `CREATE UNIQUE INDEX` to `ALTER TABLE ... ADD CONSTRAINT`, unless the column
is marked as UNIQUE directly - then the index is implied, don't create it explicitly.

For Audit tables see 'postgres-audit.sql' right next to this file.
For Quartz tables see 'postgres-quartz.sql'.

noinspection SqlResolveForFile @ operator-class/"gin__int_ops"

public schema is not used as of now, everything is in the current user schema
https://www.postgresql.org/docs/15/ddl-schemas.html#DDL-SCHEMAS-PATTERNS[ddl-schemas-patterns]
see secure schema usage pattern

just in case CURRENT_USER schema was dropped (fastest way to remove all midpoint objects)
```
drop schema current_user cascade;
```
*/

-- @region: infrastructure
-- @regionTitle: Infrastructure
-- @regionDescription: Schema, extensions, enum types, shared identifiers, repository metadata, and support routines.
-- @description: Creates the schema used by the native PostgreSQL repository objects for the current database user.
CREATE SCHEMA IF NOT EXISTS AUTHORIZATION CURRENT_USER;

-- @description: Enables indexing support for integer array columns used by repository search tables.
CREATE EXTENSION IF NOT EXISTS intarray; -- support for indexing INTEGER[] columns
-- @description: Enables trigram indexes used for efficient text search.
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes
-- @description: Enables fuzzy string matching functions used by repository search features.
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; -- fuzzy string match (levenshtein, etc.)

-- region custom enum types
-- Some enums are from schema, some are only defined in repo-sqale.
-- All Java enum types must be registered in SqaleRepoContext constructor.

-- First purely repo-sqale enums (these have M prefix in Java, the rest of the name is the same):
-- @description: Identifies separately stored container table categories in the native repository.
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
-- @description: Identifies midPoint object kinds stored in native repository object tables.
CREATE TYPE ObjectType AS ENUM (
    'ABSTRACT_ROLE',
    'ACCESS_CERTIFICATION_CAMPAIGN',
    'ACCESS_CERTIFICATION_DEFINITION',
    'APPLICATION',
    'ARCHETYPE',
    'ASSIGNMENT_HOLDER',
    'CASE',
    'CONNECTOR',
    'CONNECTOR_DEVELOPMENT',
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

-- @description: Identifies reference table categories used for object and container references.
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

-- @description: Identifies whether an extension item belongs to object extension data or resource attributes.
CREATE TYPE ExtItemHolderType AS ENUM (
    'EXTENSION',
    'ATTRIBUTES');

-- @description: Identifies whether an indexed extension item stores a single value or an array of values.
CREATE TYPE ExtItemCardinality AS ENUM (
    'SCALAR',
    'ARRAY');

-- Schema based enums have the same name like their enum classes (I like the Type suffix here):
-- @description: Stores access certification campaign states used by certification campaign tables.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AccessCertificationCampaignStateType
CREATE TYPE AccessCertificationCampaignStateType AS ENUM (
    'CREATED', 'IN_REVIEW_STAGE', 'REVIEW_STAGE_DONE', 'IN_REMEDIATION', 'CLOSED');

-- @description: Stores activation administrative status values used by focus, assignment, and resource data.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ActivationStatusType
CREATE TYPE ActivationStatusType AS ENUM ('ENABLED', 'DISABLED', 'ARCHIVED');

-- @description: Stores administrative availability values used by resource search data.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AdministrativeAvailabilityStatusType
CREATE TYPE AdministrativeAvailabilityStatusType AS ENUM ('MAINTENANCE', 'OPERATIONAL');

-- @description: Stores availability status values used for operational resource and connector state.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AvailabilityStatusType
CREATE TYPE AvailabilityStatusType AS ENUM ('DOWN', 'UP', 'BROKEN');

-- @description: Stores correlation situation values used by shadow identity matching.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#CorrelationSituationType
CREATE TYPE CorrelationSituationType AS ENUM ('UNCERTAIN', 'EXISTING_OWNER', 'NO_OWNER', 'ERROR');

-- @description: Stores execution mode values used by tasks, simulations, and repository-side reporting.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ExecutionModeType
CREATE TYPE ExecutionModeType AS ENUM ('FULL', 'PREVIEW', 'SHADOW_MANAGEMENT_PREVIEW', 'DRY_RUN', 'NONE', 'BUCKET_ANALYSIS');

-- @description: Stores lockout status values used by focus activation data.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#LockoutStatusType
CREATE TYPE LockoutStatusType AS ENUM ('NORMAL', 'LOCKED');

-- @description: Stores node operational state values used by node repository objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#NodeOperationalStateType
CREATE TYPE NodeOperationalStateType AS ENUM ('UP', 'DOWN', 'STARTING');

-- @description: Stores operation execution record type values used by operation execution containers.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#OperationExecutionRecordTypeType
CREATE TYPE OperationExecutionRecordTypeType AS ENUM ('SIMPLE', 'COMPLEX');

-- NOTE: Keep in sync with the same enum in postgres-new-audit.sql!
-- @description: Stores operation result status values used by tasks, operation executions, and audit-related data.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#OperationResultStatusType
CREATE TYPE OperationResultStatusType AS ENUM ('SUCCESS', 'WARNING', 'PARTIAL_ERROR',
    'FATAL_ERROR', 'HANDLED_ERROR', 'NOT_APPLICABLE', 'IN_PROGRESS', 'UNKNOWN');

-- @description: Stores orientation values for report output and presentation metadata.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#OrientationType
CREATE TYPE OrientationType AS ENUM ('PORTRAIT', 'LANDSCAPE');

-- @description: Stores predefined configuration profile values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#PredefinedConfigurationType
CREATE TYPE PredefinedConfigurationType AS ENUM ( 'PRODUCTION', 'DEVELOPMENT' );

-- @description: Stores administrative state values used by resource objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ResourceAdministrativeStateType
CREATE TYPE ResourceAdministrativeStateType AS ENUM ('ENABLED', 'DISABLED');

-- @description: Stores shadow kind values used to classify resource object shadows.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ShadowKindType
CREATE TYPE ShadowKindType AS ENUM ('ACCOUNT', 'ENTITLEMENT', 'GENERIC', 'ASSOCIATION', 'UNKNOWN');

-- @description: Stores synchronization situation values used by shadow lifecycle and synchronization processing.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SynchronizationSituationType
CREATE TYPE SynchronizationSituationType AS ENUM (
    'DELETED', 'DISPUTED', 'LINKED', 'UNLINKED', 'UNMATCHED');

-- @description: Stores task auto-scaling mode values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskAutoScalingModeType
CREATE TYPE TaskAutoScalingModeType AS ENUM ('DISABLED', 'DEFAULT');

-- @description: Stores task binding values describing whether task execution is tied to a specific node.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskBindingType
CREATE TYPE TaskBindingType AS ENUM ('LOOSE', 'TIGHT');

-- @description: Stores task execution state values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskExecutionStateType
CREATE TYPE TaskExecutionStateType AS ENUM ('RUNNING', 'RUNNABLE', 'WAITING', 'SUSPENDED', 'CLOSED');

-- @description: Stores task recurrence values for single-run and recurring tasks.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskRecurrenceType
CREATE TYPE TaskRecurrenceType AS ENUM ('SINGLE', 'RECURRING');

-- @description: Stores task scheduling state values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskSchedulingStateType
CREATE TYPE TaskSchedulingStateType AS ENUM ('READY', 'WAITING', 'SUSPENDED', 'CLOSED');

-- @description: Stores task waiting reason values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskWaitingReasonType
CREATE TYPE TaskWaitingReasonType AS ENUM ('OTHER_TASKS', 'OTHER');

-- @description: Stores task thread stop action values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ThreadStopActionType
CREATE TYPE ThreadStopActionType AS ENUM ('RESTART', 'RESCHEDULE', 'SUSPEND', 'CLOSE');

-- @description: Stores time interval status values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TimeIntervalStatusType
CREATE TYPE TimeIntervalStatusType AS ENUM ('BEFORE', 'IN', 'AFTER');
-- endregion

-- region OID-pool table
-- To support gen_random_uuid() pgcrypto extension must be enabled for the database (not for PG 13).
-- select * from pg_available_extensions order by name;
-- @description: Ensures UUID generation support by enabling pgcrypto when gen_random_uuid() is not already available.
-- @usedFor: OID generation support
DO $$
BEGIN
    PERFORM pg_get_functiondef('gen_random_uuid()'::regprocedure);
    RAISE NOTICE 'gen_random_uuid already exists, skipping create EXTENSION pgcrypto';
EXCEPTION WHEN undefined_function THEN
    CREATE EXTENSION pgcrypto;
END
$$;

-- "OID pool", provides generated OID, can be referenced by FKs.
-- @description: Provides generated object identifiers shared by all concrete object tables.
CREATE TABLE m_object_oid (
    -- @description: Generated repository object identifier referenced by concrete object rows.
    oid UUID PRIMARY KEY DEFAULT gen_random_uuid()
);
-- endregion

-- region Functions/triggers
-- BEFORE INSERT trigger - must be declared on all concrete m_object sub-tables.
-- @description: Assigns a new OID to inserted object rows or reserves an explicitly provided OID.
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
-- @description: Releases the shared OID row after a concrete object row is deleted.
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
-- @description: Prevents object OID changes and updates database-managed modification metadata.
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
-- @description: Stores small repository-wide metadata and flags used by database-side maintenance logic.
CREATE TABLE m_global_metadata (
    -- @description: Metadata entry name.
    name TEXT PRIMARY KEY,
    -- @description: Metadata entry value.
    value TEXT
);

/*
@description: Stores frequently used URI values, such as channels and relation QNames, as compact numeric identifiers.

Catalog of often used URIs, typically channels and relation Q-names.
Never update values of "uri" manually to change URI for some objects
(unless you really want to migrate old URI to a new one).
URI can be anything, for QNames the format is based on QNameUtil ("prefix-url#localPart").
 */
CREATE TABLE m_uri (
    -- @description: Numeric URI identifier referenced by repository tables.
    id SERIAL NOT NULL PRIMARY KEY,
    -- @description: URI text value.
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
-- @description: Abstract base table containing common fields inherited by all repository object tables.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ObjectType
CREATE TABLE m_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    -- @description: Object identifier inherited by concrete object tables.
    oid UUID NOT NULL,
    -- objectType will be overridden with GENERATED value in concrete table
    -- CHECK helps optimizer to avoid this table when different type is asked, mind that
    -- WHERE objectType = 'OBJECT' never returns anything (unlike select * from m_object).
    -- We don't want this check to be inherited as it would prevent any inserts of other types.

    -- PG16: ObjectType column will be added later, it needs to have different definition for PG < 16 and PG >= 16
    -- and it is not possible to achieve that definition with ALTER COLUMN statement
    -- objectType ObjectType NOT NULL CHECK (objectType = 'OBJECT') NO INHERIT,
    -- @description: Original object name as entered or displayed.
    nameOrig TEXT NOT NULL,
    -- @description: Normalized object name used for exact case-insensitive lookup and uniqueness.
    nameNorm TEXT NOT NULL,
    /*
     * @description: Serialized full object representation, however some items are stored separately.
     * See xref:/midpoint/reference/repository/native-postgresql/splitted-fullobject/[Splitted full object] for more information.
     */
    fullObject BYTEA,
    -- @description: OID of the tenant reference target object.
    tenantRefTargetOid UUID,
    -- @description: Object type of the tenant reference target.
    tenantRefTargetType ObjectType,
    -- @description: Relation URI identifier of the tenant reference.
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    lifecycleState TEXT, -- TODO what is this? how many distinct values?
    -- @description: Next container identifier value allocated within this object.
    cidSeq BIGINT NOT NULL DEFAULT 1, -- sequence for container id, next free cid
    -- @description: Object version used for optimistic locking.
    version INTEGER NOT NULL DEFAULT 1,
    -- complex DB columns, add indexes as needed per concrete table, e.g. see m_user
    -- TODO compare with [] in JSONB, check performance, indexing, etc. first
    -- @description: Policy situation URI identifiers attached to the object.
    policySituations INTEGER[], -- soft-references m_uri, only EQ filter
    -- @description: Object subtype values used for subtype filtering.
    subtypes TEXT[], -- only EQ filter
    -- @description: Text search helper content derived from selected object data.
    fullTextInfo TEXT,
    /*
     * @description: Indexed extension and attribute values stored as JSON data.
     *
     * Extension items are stored as JSON key:value pairs, where key is m_ext_item.id (as string)
     * and values are stored as follows (this is internal and has no effect on how query is written):
     * * string and boolean are stored as-is
     * * any numeric type integral/float/precise is stored as NUMERIC (JSONB can store that)
     * * enum as toString() or name() of the Java enum instance
     * * date-time as Instant.toString() ISO-8601 long date-timeZ (UTC), cut to 3 fraction digits
     * * poly-string is stored as sub-object {"o":"orig-value","n":"norm-value"}
     * * reference is stored as sub-object {"o":"oid","t":"targetType","r":"relationId"}
     * ** where targetType is ObjectType and relationId is from m_uri.id, just like for ref columns
     */
    ext JSONB,
    -- metadata
    -- @description: OID of the object that created this object.
    creatorRefTargetOid UUID,
    -- @description: Object type of the object that created this object.
    creatorRefTargetType ObjectType,
    -- @description: Relation URI identifier for the creator reference.
    creatorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that created the object.
    createChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the object was created.
    createTimestamp TIMESTAMPTZ,
    -- @description: OID of the object that last modified this object.
    modifierRefTargetOid UUID,
    -- @description: Object type of the object that last modified this object.
    modifierRefTargetType ObjectType,
    -- @description: Relation URI identifier for the modifier reference.
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that last modified the object.
    modifyChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the object was last modified.
    modifyTimestamp TIMESTAMPTZ,

    -- these are purely DB-managed metadata, not mapped to in midPoint
    -- @description: Database timestamp when this row was created.
    db_created TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    -- @description: Database timestamp when this row was last modified.
    db_modified TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, -- updated in update trigger

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
);




-- Important objectType column needs to be non-generated on PG < 16 and generated on PG>=16
--
-- Before Postgres 16: if parent column was generated, child columns must use same value
-- After 16: Parent must be generated, if children are generated, they may have different generation values

-- @description: Adds the `objectType` discriminator column to `m_object` using PostgreSQL-version-specific generated-column behavior.
-- @usedFor: PostgreSQL compatibility for inherited object type discriminator columns
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

/*
@description: Abstract base table for assignment-holding objects, excluding shadows.

Represents AssignmentHolderType (all objects except shadows)
extending m_object, but still abstract, hence the CHECK (false).

No indexes here, always add indexes and referential constraints on concrete sub-tables.
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AssignmentHolderType
CREATE TABLE m_assignment_holder (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'ASSIGNMENT_HOLDER') NO INHERIT,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_object);

/*
@description: Abstract base table for separately persisted container values.

Purely abstract table (no entries are allowed). Represents Containerable/PrismContainerValue.
Allows querying all separately persisted containers, but not necessary for the application.
 */
CREATE TABLE m_container (
    /*
     * @description: OID of the owning object row.
     *
     * Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
     * Owner does not have to be the direct parent of the container.
     */
    ownerOid UUID NOT NULL,
    -- use like this on the concrete table:
    -- ownerOid UUID NOT NULL REFERENCES m_object_oid(oid),

    /*
     * @description: Container identifier unique within the owning object.
     *
     * Container ID, unique in the scope of the whole object (owner).
     * While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
     */
    cid BIGINT NOT NULL,
    -- containerType will be overridden with GENERATED value in concrete table
    -- containerType will be added by ALTER because we need different definition between PG Versions
    -- containerType ContainerType NOT NULL,

    CHECK (FALSE) NO INHERIT
    -- add on concrete table (additional columns possible): PRIMARY KEY (ownerOid, cid)
);
-- Abstract reference table, for object but also other container references.
-- @description: Abstract base table for object and container references.
CREATE TABLE m_reference (
    -- @description: OID of the object that owns the reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Object type of the reference owner.
    ownerType ObjectType NOT NULL,
    -- referenceType will be overridden with GENERATED value in concrete table
    -- referenceType will be added by ALTER because we need different definition between PG Versions

    -- @description: OID of the referenced target object.
    targetOid UUID NOT NULL, -- soft-references m_object
    -- @description: Object type of the referenced target object.
    targetType ObjectType NOT NULL,
    -- @description: Relation URI identifier of the reference.
    relationId INTEGER NOT NULL REFERENCES m_uri(id),

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
    -- add PK (referenceType is the same per table): PRIMARY KEY (ownerOid, relationId, targetOid)
);

-- Important: referenceType, containerType column needs to be non-generated on PG < 16 and generated on PG>=16
--
-- Before Postgres 16: if parent column was generated, child columns must use same value
-- After 16: Parent must be generated, if children are generated, they may have different generation values

-- @description: Adds discriminator columns to `m_reference` and `m_container` using PostgreSQL-version-specific generated-column behavior.
-- @usedFor: PostgreSQL compatibility for inherited reference and container discriminator columns
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


-- @region: references
-- @regionTitle: References
-- @regionDescription: Tables storing object and container references such as archetype, projection, role membership, and approver references.
-- references related to ObjectType and AssignmentHolderType
-- stores AssignmentHolderType/archetypeRef
-- @description: Stores archetype references assigned to assignment-holder objects.
CREATE TABLE m_ref_archetype (
    -- @description: OID of the object that owns the archetype reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (referenceType = 'ARCHETYPE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of archetype references by target object and relation.
-- @usedFor: archetype reference target searches
CREATE INDEX m_ref_archetype_targetOidRelationId_idx
    ON m_ref_archetype (targetOid, relationId);

-- stores AssignmentHolderType/delegatedRef
-- @description: Stores delegated references for assignment-holder objects.
CREATE TABLE m_ref_delegated (
    -- @description: OID of the object that owns the delegated reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('DELEGATED') STORED
        CHECK (referenceType = 'DELEGATED'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of delegated references by target object and relation.
-- @usedFor: delegated reference target searches
CREATE INDEX m_ref_delegated_targetOidRelationId_idx
    ON m_ref_delegated (targetOid, relationId);

-- stores ObjectType/metadata/createApproverRef
-- @description: Stores create-approver references from object metadata.
CREATE TABLE m_ref_object_create_approver (
    -- @description: OID of the object that owns the create-approver reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_CREATE_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_CREATE_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of object create-approver references by target object and relation.
-- @usedFor: object create-approver reference target searches
CREATE INDEX m_ref_object_create_approver_targetOidRelationId_idx
    ON m_ref_object_create_approver (targetOid, relationId);


-- stores ObjectType/effectiveMarkRef
-- @description: Stores effective mark references attached to objects.
CREATE TABLE m_ref_object_effective_mark (
    -- @description: OID of the object that owns the effective mark reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_EFFECTIVE_MARK') STORED
        CHECK (referenceType = 'OBJECT_EFFECTIVE_MARK'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of object effective mark references by target object and relation.
-- @usedFor: effective mark reference target searches
CREATE INDEX m_ref_object_effective_mark_targetOidRelationId_idx
    ON m_ref_object_effective_mark (targetOid, relationId);


-- stores ObjectType/metadata/modifyApproverRef
-- @description: Stores modify-approver references from object metadata.
CREATE TABLE m_ref_object_modify_approver (
    -- @description: OID of the object that owns the modify-approver reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_MODIFY_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_MODIFY_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of object modify-approver references by target object and relation.
-- @usedFor: object modify-approver reference target searches
CREATE INDEX m_ref_object_modify_approver_targetOidRelationId_idx
    ON m_ref_object_modify_approver (targetOid, relationId);

-- stores AssignmentHolderType/roleMembershipRef
-- @description: Stores computed role membership references for assignment-holder objects.
CREATE TABLE m_ref_role_membership (
    -- @description: OID of the object that owns the role membership reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ROLE_MEMBERSHIP') STORED
        CHECK (referenceType = 'ROLE_MEMBERSHIP'),
    -- @description: Serialized reference value when full reference data is needed.
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of role membership references by target object and relation.
-- @usedFor: role membership target searches
CREATE INDEX m_ref_role_membership_targetOidRelationId_idx
    ON m_ref_role_membership (targetOid, relationId);
-- endregion

-- @region: focus-and-users
-- @regionTitle: Focus and users
-- @regionDescription: Focus base data, users, generic objects, focus identities, and focus-related references.
-- region FOCUS related tables
-- Represents FocusType (Users, Roles, ...), see https://docs.evolveum.com/midpoint/reference/schema/focus-and-projections/
-- extending m_object, but still abstract, hence the CHECK (false)
-- @description: Abstract base table for focus objects such as users, roles, services, and organizations.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#FocusType
CREATE TABLE m_focus (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'FOCUS') NO INHERIT,
    -- @description: Cost center value used by focus objects.
    costCenter TEXT,
    -- @description: Primary email address of the focus object.
    emailAddress TEXT,
    -- @description: Binary photo data for the focus object.
    photo BYTEA, -- will be TOAST-ed if necessary
    -- @description: Locale preference of the focus object.
    locale TEXT,
    -- @description: Original locality value.
    localityOrig TEXT,
    -- @description: Normalized locality value used for searches.
    localityNorm TEXT,
    -- @description: Preferred language of the focus object.
    preferredLanguage TEXT,
    -- @description: Telephone number of the focus object.
    telephoneNumber TEXT,
    -- @description: Timezone preference of the focus object.
    timezone TEXT,
    -- credential/password/metadata
    -- @description: Time when password credentials were created.
    passwordCreateTimestamp TIMESTAMPTZ,
    -- @description: Time when password credentials were last modified.
    passwordModifyTimestamp TIMESTAMPTZ,
    -- activation
    -- @description: Administrative activation status.
    administrativeStatus ActivationStatusType,
    -- @description: Effective activation status.
    effectiveStatus ActivationStatusType,
    -- @description: Time when the focus becomes enabled.
    enableTimestamp TIMESTAMPTZ,
    -- @description: Time when the focus becomes disabled.
    disableTimestamp TIMESTAMPTZ,
    -- @description: Reason for disabling the focus.
    disableReason TEXT,
    -- @description: Validity interval status.
    validityStatus TimeIntervalStatusType,
    -- @description: Start of the focus validity interval.
    validFrom TIMESTAMPTZ,
    -- @description: End of the focus validity interval.
    validTo TIMESTAMPTZ,
    -- @description: Time when validity state last changed.
    validityChangeTimestamp TIMESTAMPTZ,
    -- @description: Time when the focus was archived.
    archiveTimestamp TIMESTAMPTZ,
    -- @description: Lockout status for focus credentials.
    lockoutStatus LockoutStatusType,
    -- @description: Normalized focus data used by repository search and correlation features.
    normalizedData JSONB,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_assignment_holder);

-- for each concrete sub-table indexes must be added, validFrom, validTo, etc.

-- stores FocusType/personaRef
-- @description: Stores persona references for focus objects.
CREATE TABLE m_ref_persona (
    -- @description: OID of the focus object that owns the persona reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PERSONA') STORED
        CHECK (referenceType = 'PERSONA'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of persona references by target object and relation.
-- @usedFor: persona reference target searches
CREATE INDEX m_ref_persona_targetOidRelationId_idx
    ON m_ref_persona (targetOid, relationId);

-- stores FocusType/linkRef ("projection" is newer and better term)
-- @description: Stores projection references from focus objects to resource object shadows.
CREATE TABLE m_ref_projection (
    -- @description: OID of the focus object that owns the projection reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PROJECTION') STORED
        CHECK (referenceType = 'PROJECTION'),
    -- @description: Serialized reference value when full projection reference data is needed.
    fullObject BYTEA,
    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of projection references by target object and relation.
-- @usedFor: projection reference target searches
CREATE INDEX m_ref_projection_targetOidRelationId_idx
    ON m_ref_projection (targetOid, relationId);

-- @description: Stores focus identity values used by correlation and identity matching.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#FocusIdentityType
CREATE TABLE m_focus_identity (
    -- @description: OID of the focus object that owns the identity value.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('FOCUS_IDENTITY') STORED
        CHECK (containerType = 'FOCUS_IDENTITY'),
    -- @description: Serialized focus identity value.
    fullObject BYTEA,
    -- @description: OID of the source resource reference for this identity value.
    sourceResourceRefTargetOid UUID,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up lookup of focus identities by source resource.
-- @usedFor: focus identity source-resource searches
CREATE INDEX m_focus_identity_sourceResourceRefTargetOid_idx ON m_focus_identity (sourceResourceRefTargetOid);

-- Represents GenericObjectType, see https://docs.evolveum.com/midpoint/reference/schema/generic-objects/
-- @description: Stores generic objects that do not have a dedicated concrete repository table.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#GenericObjectType
CREATE TABLE m_generic_object (
    -- @description: Generic object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('GENERIC_OBJECT') STORED
        CHECK (objectType = 'GENERIC_OBJECT')
)
    INHERITS (m_focus);

-- @description: Maintains the object OID registry when rows are inserted into `m_generic_object`.
CREATE TRIGGER m_generic_object_oid_insert_tr BEFORE INSERT ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_generic_object`.
CREATE TRIGGER m_generic_object_update_tr BEFORE UPDATE ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_generic_object`.
CREATE TRIGGER m_generic_object_oid_delete_tr AFTER DELETE ON m_generic_object
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_generic_object_nameOrig_idx ON m_generic_object (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_generic_object_nameNorm_key ON m_generic_object (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_generic_object_subtypes_idx ON m_generic_object USING gin(subtypes);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_generic_object_validFrom_idx ON m_generic_object (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_generic_object_validTo_idx ON m_generic_object (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_generic_object_fullTextInfo_idx
    ON m_generic_object USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_generic_object_createTimestamp_idx ON m_generic_object (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_generic_object_modifyTimestamp_idx ON m_generic_object (modifyTimestamp);
-- endregion

-- region USER related tables
-- Represents UserType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/usertype/
-- @description: Stores user objects and user-specific searchable attributes.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#UserType
CREATE TABLE m_user (
    -- @description: User object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('USER') STORED
        CHECK (objectType = 'USER'),
    -- @description: Additional name in original form.
    additionalNameOrig TEXT,
    -- @description: Additional name in normalized form used for searches.
    additionalNameNorm TEXT,
    -- @description: Display name in original form.
    displayNameOrig TEXT,
    -- @description: Display name in normalized form used for searches.
    displayNameNorm TEXT,
    -- @description: Employee number assigned to the user.
    employeeNumber TEXT,
    -- @description: Family name in original form.
    familyNameOrig TEXT,
    -- @description: Family name in normalized form used for searches.
    familyNameNorm TEXT,
    -- @description: Full name in original form.
    fullNameOrig TEXT,
    -- @description: Full name in normalized form used for searches.
    fullNameNorm TEXT,
    -- @description: Given name in original form.
    givenNameOrig TEXT,
    -- @description: Given name in normalized form used for searches.
    givenNameNorm TEXT,
    -- @description: Honorific prefix in original form.
    honorificPrefixOrig TEXT,
    -- @description: Honorific prefix in normalized form used for searches.
    honorificPrefixNorm TEXT,
    -- @description: Honorific suffix in original form.
    honorificSuffixOrig TEXT,
    -- @description: Honorific suffix in normalized form used for searches.
    honorificSuffixNorm TEXT,
    -- @description: Nickname in original form.
    nickNameOrig TEXT,
    -- @description: Nickname in normalized form used for searches.
    nickNameNorm TEXT,
    -- @description: Personal number assigned to the user.
    personalNumber TEXT,
    -- @description: Preferred name in original form.
    preferredNameOrig TEXT,
    -- @description: Preferred name in normalized form used for searches.
    preferredNameNorm TEXT,
    -- @description: Title in original form.
    titleOrig TEXT,
    -- @description: Title in normalized form used for searches.
    titleNorm TEXT,
    -- @description: Organization values stored as searchable poly-string JSON data.
    organizations JSONB, -- array of {o,n} objects (poly-strings)
    -- @description: Organizational unit values stored as searchable poly-string JSON data.
    organizationUnits JSONB -- array of {o,n} objects (poly-strings)
)
    INHERITS (m_focus);

-- @description: Maintains the object OID registry when rows are inserted into `m_user`.
CREATE TRIGGER m_user_oid_insert_tr BEFORE INSERT ON m_user
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_user`.
CREATE TRIGGER m_user_update_tr BEFORE UPDATE ON m_user
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_user`.
CREATE TRIGGER m_user_oid_delete_tr AFTER DELETE ON m_user
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_user_nameOrig_idx ON m_user (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_user_nameNorm_key ON m_user (nameNorm);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_user_policySituation_idx ON m_user USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_user_ext_idx ON m_user USING gin(ext);
-- @description: Speeds up lookup by display name.
-- @usedFor: display name searches
CREATE INDEX m_user_displayNameOrig_idx ON m_user (displayNameOrig);
-- @description: Speeds up lookup by original full name.
-- @usedFor: full name searches
CREATE INDEX m_user_fullNameOrig_idx ON m_user (fullNameOrig);
-- @description: Speeds up lookup by original family name.
-- @usedFor: family name searches
CREATE INDEX m_user_familyNameOrig_idx ON m_user (familyNameOrig);
-- @description: Speeds up lookup by original given name.
-- @usedFor: given name searches
CREATE INDEX m_user_givenNameOrig_idx ON m_user (givenNameOrig);
-- @description: Speeds up lookup by employee number.
-- @usedFor: employee number searches
CREATE INDEX m_user_employeeNumber_idx ON m_user (employeeNumber);
-- @description: Speeds up lookup by preferred name.
-- @usedFor: preferred name searches
CREATE INDEX m_user_preferredNameOrig_idx ON m_user (preferredNameOrig);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_user_subtypes_idx ON m_user USING gin(subtypes);
-- @description: Speeds up filtering by organization values.
-- @usedFor: organization filters
CREATE INDEX m_user_organizations_idx ON m_user USING gin(organizations);
-- @description: Speeds up filtering by organizational unit values.
-- @usedFor: organizational unit filters
CREATE INDEX m_user_organizationUnits_idx ON m_user USING gin(organizationUnits);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_user_validFrom_idx ON m_user (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_user_validTo_idx ON m_user (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_user_fullTextInfo_idx ON m_user USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_user_createTimestamp_idx ON m_user (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_user_modifyTimestamp_idx ON m_user (modifyTimestamp);
-- endregion

-- @region: roles-and-services
-- @regionTitle: Roles and services
-- @regionDescription: Abstract roles, roles, services, archetypes, policies, applications, and related object tables.
-- region ROLE related tables
-- Represents AbstractRoleType, see https://docs.evolveum.com/midpoint/architecture/concepts/abstract-role/
-- @description: Abstract base table for role-like focus objects that can be assigned or requested.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AbstractRoleType
CREATE TABLE m_abstract_role (
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL CHECK (objectType = 'ABSTRACT_ROLE') NO INHERIT,
    -- @description: Whether auto-assignment is enabled for this abstract role.
    autoAssignEnabled BOOLEAN,
    -- @description: Display name in original form.
    displayNameOrig TEXT,
    -- @description: Display name in normalized form used for searches.
    displayNameNorm TEXT,
    -- @description: Business or application identifier of the abstract role.
    identifier TEXT,
    -- @description: Whether this abstract role can be requested.
    requestable BOOLEAN,
    -- @description: Risk level associated with the abstract role.
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
-- @description: Stores role objects used to model access, business roles, and technical roles.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#RoleType
CREATE TABLE m_role (
    -- @description: Role object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE') STORED
        CHECK (objectType = 'ROLE')
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_role`.
CREATE TRIGGER m_role_oid_insert_tr BEFORE INSERT ON m_role
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_role`.
CREATE TRIGGER m_role_update_tr BEFORE UPDATE ON m_role
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_role`.
CREATE TRIGGER m_role_oid_delete_tr AFTER DELETE ON m_role
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_role_nameOrig_idx ON m_role (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_role_nameNorm_key ON m_role (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_role_subtypes_idx ON m_role USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_role_identifier_idx ON m_role (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_role_validFrom_idx ON m_role (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_role_validTo_idx ON m_role (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_role_fullTextInfo_idx ON m_role USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_role_createTimestamp_idx ON m_role (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_role_modifyTimestamp_idx ON m_role (modifyTimestamp);


-- Represents PolicyType
-- @description: Stores policy objects that define reusable policy behavior.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#PolicyType
CREATE TABLE m_policy (
    -- @description: Policy object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('POLICY') STORED
        CHECK (objectType = 'POLICY')
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_policy`.
CREATE TRIGGER m_policy_oid_insert_tr BEFORE INSERT ON m_policy
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_policy`.
CREATE TRIGGER m_policy_update_tr BEFORE UPDATE ON m_policy
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_policy`.
CREATE TRIGGER m_policy_oid_delete_tr AFTER DELETE ON m_policy
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_policy_nameOrig_idx ON m_policy (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_policy_nameNorm_key ON m_policy (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_policy_subtypes_idx ON m_policy USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_policy_identifier_idx ON m_policy (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_policy_validFrom_idx ON m_policy (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_policy_validTo_idx ON m_policy (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_policy_fullTextInfo_idx ON m_policy USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_policy_createTimestamp_idx ON m_policy (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_policy_modifyTimestamp_idx ON m_policy (modifyTimestamp);


-- Represents ApplicationType
-- @description: Stores application objects used to model applications and application access.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ApplicationType
CREATE TABLE m_application (
    -- @description: Application object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('APPLICATION') STORED
        CHECK (objectType = 'APPLICATION')
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_application`.
CREATE TRIGGER m_application_oid_insert_tr BEFORE INSERT ON m_application
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_application`.
CREATE TRIGGER m_application_update_tr BEFORE UPDATE ON m_application
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_application`.
CREATE TRIGGER m_application_oid_delete_tr AFTER DELETE ON m_application
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_application_nameOrig_idx ON m_application (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_application_nameNorm_key ON m_application (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_application_subtypes_idx ON m_application USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_application_identifier_idx ON m_application (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_application_validFrom_idx ON m_application (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_application_validTo_idx ON m_application (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_application_fullTextInfo_idx ON m_application USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_application_createTimestamp_idx ON m_application (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_application_modifyTimestamp_idx ON m_application (modifyTimestamp);



/*
@description: Stores service objects, including service accounts and application services.

Represents ServiceType, see https://docs.evolveum.com/midpoint/reference/deployment/service-account-management/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ServiceType
CREATE TABLE m_service (
    -- @description: Service object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SERVICE') STORED
        CHECK (objectType = 'SERVICE'),
    -- @description: Display ordering value for service presentation.
    displayOrder INTEGER
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_service`.
CREATE TRIGGER m_service_oid_insert_tr BEFORE INSERT ON m_service
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_service`.
CREATE TRIGGER m_service_update_tr BEFORE UPDATE ON m_service
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_service`.
CREATE TRIGGER m_service_oid_delete_tr AFTER DELETE ON m_service
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_service_nameOrig_idx ON m_service (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_service_nameNorm_key ON m_service (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_service_subtypes_idx ON m_service USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_service_identifier_idx ON m_service (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_service_validFrom_idx ON m_service (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_service_validTo_idx ON m_service (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_service_fullTextInfo_idx ON m_service USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_service_createTimestamp_idx ON m_service (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_service_modifyTimestamp_idx ON m_service (modifyTimestamp);

-- Represents ArchetypeType, see https://docs.evolveum.com/midpoint/reference/schema/archetypes/
-- @description: Stores archetype objects used to classify and constrain other objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ArchetypeType
CREATE TABLE m_archetype (
    -- @description: Archetype object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (objectType = 'ARCHETYPE')
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_archetype`.
CREATE TRIGGER m_archetype_oid_insert_tr BEFORE INSERT ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_archetype`.
CREATE TRIGGER m_archetype_update_tr BEFORE UPDATE ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_archetype`.
CREATE TRIGGER m_archetype_oid_delete_tr AFTER DELETE ON m_archetype
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_archetype_nameOrig_idx ON m_archetype (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_archetype_nameNorm_key ON m_archetype (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_archetype_subtypes_idx ON m_archetype USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_archetype_identifier_idx ON m_archetype (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_archetype_validFrom_idx ON m_archetype (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_archetype_validTo_idx ON m_archetype (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_archetype_fullTextInfo_idx ON m_archetype USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_archetype_createTimestamp_idx ON m_archetype (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_archetype_modifyTimestamp_idx ON m_archetype (modifyTimestamp);
-- endregion

-- @region: organization
-- @regionTitle: Organization
-- @regionDescription: Organization objects, parent organization references, and organization hierarchy closure support.
-- region Organization hierarchy support
/*
@description: Stores organization objects and hierarchy-related organization attributes.

Represents OrgType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/orgtype/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#OrgType
CREATE TABLE m_org (
    -- @description: Organization object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ORG') STORED
        CHECK (objectType = 'ORG'),
    -- @description: Display ordering value used when presenting organizations.
    displayOrder INTEGER,
    -- @description: Marks whether this organization represents a tenant.
    tenant BOOLEAN
)
    INHERITS (m_abstract_role);

-- @description: Maintains the object OID registry when rows are inserted into `m_org`.
CREATE TRIGGER m_org_oid_insert_tr BEFORE INSERT ON m_org
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_org`.
CREATE TRIGGER m_org_update_tr BEFORE UPDATE ON m_org
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_org`.
CREATE TRIGGER m_org_oid_delete_tr AFTER DELETE ON m_org
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_org_nameOrig_idx ON m_org (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_org_nameNorm_key ON m_org (nameNorm);
-- @description: Speeds up ordering or filtering organizations by display order.
-- @usedFor: organization display ordering
CREATE INDEX m_org_displayOrder_idx ON m_org (displayOrder);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_org_subtypes_idx ON m_org USING gin(subtypes);
-- @description: Speeds up lookup by abstract role identifier.
-- @usedFor: identifier searches
CREATE INDEX m_org_identifier_idx ON m_org (identifier);
-- @description: Speeds up filtering by activation validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_org_validFrom_idx ON m_org (validFrom);
-- @description: Speeds up filtering by activation validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_org_validTo_idx ON m_org (validTo);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_org_fullTextInfo_idx ON m_org USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_org_createTimestamp_idx ON m_org (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_org_modifyTimestamp_idx ON m_org (modifyTimestamp);

-- stores ObjectType/parentOrgRef
-- @description: Stores parent organization references that define the organization hierarchy.
CREATE TABLE m_ref_object_parent_org (
    -- @description: OID of the object that owns the parent organization reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_PARENT_ORG') STORED
        CHECK (referenceType = 'OBJECT_PARENT_ORG'),

    -- TODO wouldn't (ownerOid, targetOid, relationId) perform better for typical queries?
    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of parent organization references by target organization and relation.
-- @usedFor: organization hierarchy parent and child searches
CREATE INDEX m_ref_object_parent_org_targetOidRelationId_idx
    ON m_ref_object_parent_org (targetOid, relationId);

-- region org-closure
/*
@description: Materialized transitive closure of organization parent-child relationships.

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
-- @description: Enforces unique ancestor-descendant organization closure rows and speeds up descendant lookup by ancestor.
-- @usedFor: organization subtree searches
CREATE UNIQUE INDEX m_org_closure_asc_desc_idx
    ON m_org_closure (ancestor_oid, descendant_oid);
-- @description: Speeds up ancestor lookup by descendant organization.
-- @usedFor: organization ancestor searches
CREATE INDEX m_org_closure_desc_asc_idx
    ON m_org_closure (descendant_oid, ancestor_oid);

-- The trigger for m_ref_object_parent_org that flags the view for refresh.
-- @description: Marks organization closure data for refresh when parent organization references change.
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

-- @description: Marks organization closure data for refresh after parent organization reference row changes.
CREATE TRIGGER m_ref_object_parent_mark_refresh_tr
    AFTER INSERT OR UPDATE OR DELETE ON m_ref_object_parent_org
    FOR EACH ROW EXECUTE FUNCTION mark_org_closure_for_refresh();
-- @description: Marks organization closure data for refresh after parent organization references are truncated.
CREATE TRIGGER m_ref_object_parent_mark_refresh_trunc_tr
    AFTER TRUNCATE ON m_ref_object_parent_org
    FOR EACH STATEMENT EXECUTE FUNCTION mark_org_closure_for_refresh();

-- The trigger that flags the view for refresh after m_org changes.
-- @description: Marks organization closure data for refresh when organization rows are inserted or deleted.
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
-- @description: Marks organization closure data for refresh after organization rows are inserted or deleted.
CREATE TRIGGER m_org_mark_refresh_tr
    AFTER INSERT OR DELETE ON m_org
    FOR EACH ROW EXECUTE FUNCTION mark_org_closure_for_refresh_org();
-- @description: Marks organization closure data for refresh after organization rows are truncated.
CREATE TRIGGER m_org_mark_refresh_trunc_tr
    AFTER TRUNCATE ON m_org
    FOR EACH STATEMENT EXECUTE FUNCTION mark_org_closure_for_refresh_org();

-- This procedure for conditional refresh when needed is called from the application code.
-- The refresh can be forced, e.g. after many changes with triggers off (or just to be sure).
-- @description: Refreshes the organization closure materialized view when a refresh is needed or explicitly forced.
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

-- @region: resources-and-shadows
-- @regionTitle: Resources and shadows
-- @regionDescription: Resources, shadows, shadow partitions, connectors, and resource object reference data.
-- region OTHER object tables
/*
@description: Stores resource objects and resource-specific searchable state.

Represents ResourceType, see https://docs.evolveum.com/midpoint/reference/resources/resource-configuration/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ResourceType
CREATE TABLE m_resource (
    -- @description: Resource object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('RESOURCE') STORED
        CHECK (objectType = 'RESOURCE'),
    -- @description: Business administrative state of the resource.
    businessAdministrativeState ResourceAdministrativeStateType,
    -- administrativeOperationalState/administrativeAvailabilityStatus
    -- @description: Administrative availability status from the resource operational state.
    administrativeOperationalStateAdministrativeAvailabilityStatus AdministrativeAvailabilityStatusType,
    -- operationalState/lastAvailabilityStatus
    -- @description: Last known resource availability status.
    operationalStateLastAvailabilityStatus AvailabilityStatusType,
    -- @description: OID of the connector referenced by the resource.
    connectorRefTargetOid UUID,
    -- @description: Object type of the connector referenced by the resource.
    connectorRefTargetType ObjectType,
    -- @description: Relation URI identifier of the connector reference.
    connectorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Marks whether the resource is a resource template.
    template BOOLEAN,
    -- @description: Marks whether the resource is abstract.
    abstract BOOLEAN,
    -- @description: OID of the resource super-reference target.
    superRefTargetOid UUID,
    -- @description: Object type of the resource super-reference target.
    superRefTargetType ObjectType,
    -- @description: Relation URI identifier of the resource super-reference.
    superRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_resource`.
CREATE TRIGGER m_resource_oid_insert_tr BEFORE INSERT ON m_resource
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_resource`.
CREATE TRIGGER m_resource_update_tr BEFORE UPDATE ON m_resource
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_resource`.
CREATE TRIGGER m_resource_oid_delete_tr AFTER DELETE ON m_resource
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_resource_nameOrig_idx ON m_resource (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_resource_nameNorm_key ON m_resource (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_resource_subtypes_idx ON m_resource USING gin(subtypes);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_resource_fullTextInfo_idx ON m_resource USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_resource_createTimestamp_idx ON m_resource (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_resource_modifyTimestamp_idx ON m_resource (modifyTimestamp);

-- stores ResourceType/business/approverRef
-- @description: Stores approver references for resource business configuration.
CREATE TABLE m_ref_resource_business_configuration_approver (
    -- @description: OID of the resource that owns the business configuration approver reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS
        ('RESOURCE_BUSINESS_CONFIGURATION_APPROVER') STORED,

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of resource business configuration approver references by target object and relation.
-- @usedFor: resource business approver reference target searches
CREATE INDEX m_ref_resource_biz_config_approver_targetOidRelationId_idx
    ON m_ref_resource_business_configuration_approver (targetOid, relationId);

/*
@description: Stores resource object shadows and their searchable synchronization state.

Represents ShadowType, see https://docs.evolveum.com/midpoint/reference/resources/shadow/
and also https://docs.evolveum.com/midpoint/reference/schema/focus-and-projections/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ShadowType
CREATE TABLE m_shadow (
    -- @description: Shadow object identifier.
    oid UUID NOT NULL REFERENCES m_object_oid(oid),
    objectType ObjectType
            GENERATED ALWAYS AS ('SHADOW') STORED
        CONSTRAINT m_shadow_objecttype_check
            CHECK (objectType = 'SHADOW'),
    -- @description: Original shadow name.
    nameOrig TEXT NOT NULL,
    -- @description: Normalized shadow name used for exact lookup.
    nameNorm TEXT NOT NULL,
    -- @description: Serialized full shadow object.
    fullObject BYTEA,
    -- @description: OID of the tenant reference target object.
    tenantRefTargetOid UUID,
    -- @description: Object type of the tenant reference target.
    tenantRefTargetType ObjectType,
    -- @description: Relation URI identifier of the tenant reference.
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    lifecycleState TEXT,
    -- @description: Next container identifier value allocated within this shadow.
    cidSeq BIGINT NOT NULL DEFAULT 1, -- sequence for container id, next free cid
    -- @description: Shadow version used for optimistic locking.
    version INTEGER NOT NULL DEFAULT 1,
    -- @description: Policy situation URI identifiers attached to the shadow.
    policySituations INTEGER[], -- soft-references m_uri, only EQ filter
    -- @description: Shadow subtype values used for subtype filtering.
    subtypes TEXT[], -- only EQ filter
    -- @description: Text search helper content derived from selected shadow data.
    fullTextInfo TEXT,

    -- @description: Indexed extension values stored as JSON data.
    ext JSONB,
    -- @description: OID of the object that created this shadow.
    creatorRefTargetOid UUID,
    -- @description: Object type of the object that created this shadow.
    creatorRefTargetType ObjectType,
    -- @description: Relation URI identifier for the creator reference.
    creatorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that created the shadow.
    createChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the shadow was created.
    createTimestamp TIMESTAMPTZ,
    -- @description: OID of the object that last modified this shadow.
    modifierRefTargetOid UUID,
    -- @description: Object type of the object that last modified this shadow.
    modifierRefTargetType ObjectType,
    -- @description: Relation URI identifier for the modifier reference.
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that last modified the shadow.
    modifyChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the shadow was last modified.
    modifyTimestamp TIMESTAMPTZ,

    /*
     * @description: Database timestamp when this row was created.
     *
     * Purely DB-managed metadata, not mapped to in midPoint. Updated in update trigger.
     */
    db_created TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    /*
     * @description: Database timestamp when this row was last modified.
     *
     * Purely DB-managed metadata, not mapped to in midPoint. Updated in update trigger.
     */
    db_modified TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,

    -- @description: URI identifier of the shadow object class.
    objectClassId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the resource that owns the shadow.
    resourceRefTargetOid UUID,
    -- @description: Object type of the resource that owns the shadow.
    resourceRefTargetType ObjectType,
    -- @description: Relation URI identifier of the resource reference.
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Shadow intent value.
    intent TEXT,
    -- @description: Shadow tag value.
    tag TEXT,
    -- @description: Shadow kind classification.
    kind ShadowKindType,
    -- @description: Marks whether the resource object is considered dead.
    dead BOOLEAN,
    -- @description: Marks whether the resource object currently exists.
    exist BOOLEAN,
    -- @description: Time of the last full synchronization for this shadow.
    fullSynchronizationTimestamp TIMESTAMPTZ,
    -- @description: Number of pending operations recorded for this shadow.
    pendingOperationCount INTEGER NOT NULL,
    -- @description: Primary identifier value of the resource object.
    primaryIdentifierValue TEXT,
    -- @description: Synchronization situation of the shadow.
    synchronizationSituation SynchronizationSituationType,
    -- @description: Time of the last synchronization situation update.
    synchronizationTimestamp TIMESTAMPTZ,
    -- @description: Indexed shadow attributes stored as JSON data.
    attributes JSONB,
    -- correlation
    -- @description: Time when correlation processing started.
    correlationStartTimestamp TIMESTAMPTZ,
    -- @description: Time when correlation processing ended.
    correlationEndTimestamp TIMESTAMPTZ,
    -- @description: Time when a correlation case was opened.
    correlationCaseOpenTimestamp TIMESTAMPTZ,
    -- @description: Time when a correlation case was closed.
    correlationCaseCloseTimestamp TIMESTAMPTZ,
    -- @description: Correlation situation of the shadow.
    correlationSituation CorrelationSituationType,
    -- @description: URI identifier of the disable reason.
    disableReasonId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the shadow becomes enabled.
    enableTimestamp TIMESTAMPTZ,
    -- @description: Time when the shadow becomes disabled.
    disableTimestamp TIMESTAMPTZ,
    -- @description: Last login timestamp observed for this shadow.
    lastLoginTimestamp TIMESTAMPTZ

) PARTITION BY LIST (resourceRefTargetOid);

-- @description: Maintains the object OID registry when rows are inserted into `m_shadow`.
CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_shadow`.
CREATE TRIGGER m_shadow_update_tr BEFORE UPDATE ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_shadow`.
CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();


-- @description: Default shadow partition used before resource-specific shadow partitions are created.
CREATE TABLE m_shadow_default PARTITION OF m_shadow DEFAULT;
ALTER TABLE m_shadow_default ADD PRIMARY KEY (oid);

-- @description: Speeds up lookup by original shadow name.
-- @usedFor: original shadow name searches
CREATE INDEX m_shadow_default_nameOrig_idx ON m_shadow_default (nameOrig);
-- @description: Speeds up lookup by normalized shadow name.
-- @usedFor: normalized shadow name searches
CREATE INDEX m_shadow_default_nameNorm_idx ON m_shadow_default (nameNorm); -- may not be unique for shadows!
-- @description: Enforces uniqueness of primary identifier value within object class and resource.
-- @usedFor: shadow primary identifier lookup and uniqueness checks
CREATE UNIQUE INDEX m_shadow_default_primIdVal_objCls_resRefOid_key
    ON m_shadow_default (primaryIdentifierValue, objectClassId, resourceRefTargetOid);

-- @description: Speeds up filtering by shadow subtype.
-- @usedFor: subtype filters
CREATE INDEX m_shadow_default_subtypes_idx ON m_shadow_default USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_shadow_default_policySituation_idx ON m_shadow_default USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_shadow_default_ext_idx ON m_shadow_default USING gin(ext);
-- @description: Speeds up filtering by indexed shadow attributes.
-- @usedFor: shadow attribute filters
CREATE INDEX m_shadow_default_attributes_idx ON m_shadow_default USING gin(attributes);
-- @description: Speeds up full-text-like shadow searches.
-- @usedFor: full-text-like shadow searches
CREATE INDEX m_shadow_default_fullTextInfo_idx ON m_shadow_default USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering shadows by resource OID.
-- @usedFor: resource shadow searches
CREATE INDEX m_shadow_default_resourceRefTargetOid_idx ON m_shadow_default (resourceRefTargetOid);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_shadow_default_createTimestamp_idx ON m_shadow_default (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_shadow_default_modifyTimestamp_idx ON m_shadow_default (modifyTimestamp);
-- @description: Speeds up filtering by correlation start time.
-- @usedFor: correlation time filters
CREATE INDEX m_shadow_default_correlationStartTimestamp_idx ON m_shadow_default (correlationStartTimestamp);
-- @description: Speeds up filtering by correlation end time.
-- @usedFor: correlation time filters
CREATE INDEX m_shadow_default_correlationEndTimestamp_idx ON m_shadow_default (correlationEndTimestamp);
-- @description: Speeds up filtering by correlation case open time.
-- @usedFor: correlation case time filters
CREATE INDEX m_shadow_default_correlationCaseOpenTimestamp_idx ON m_shadow_default (correlationCaseOpenTimestamp);
-- @description: Speeds up filtering by correlation case close time.
-- @usedFor: correlation case time filters
CREATE INDEX m_shadow_default_correlationCaseCloseTimestamp_idx ON m_shadow_default (correlationCaseCloseTimestamp);



-- @description: Creates or attaches resource-specific shadow partitions when partition definitions are inserted.
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

-- @description: Drops a shadow partition table when its partition definition is deleted.
CREATE OR REPLACE FUNCTION m_shadow_delete_partition() RETURNS trigger AS $BODY$
        BEGIN
            EXECUTE format('DROP TABLE IF EXISTS  %I;', OLD."table" );
            RETURN OLD;
        END

    $BODY$
LANGUAGE plpgsql;


-- @description: Attaches shadow partition tables when their partition definition is updated.
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
-- @description: Stores definitions of shadow partition tables managed by repository-side partition triggers.
CREATE TABLE m_shadow_partition_def (
    -- @description: Resource OID represented by the shadow partition.
    resourceOid uuid,
    -- @description: Object class identifier represented by the shadow partition, when partitioned by object class.
    objectClassId integer,
    -- @description: Physical table name for the shadow partition.
    "table" text NOT NULL,
    -- @description: Whether this definition represents a real data partition.
    partition boolean NOT NULL,
    -- @description: Whether this partition is attached to the parent partitioned table.
    attached boolean NOT NULL
) WITH (oids = false);

-- @description: Creates shadow partition tables from inserted partition definitions.
CREATE TRIGGER "m_shadow_partition_def_bi" BEFORE INSERT ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_create_partition();;
-- @description: Updates shadow partition attachment state from partition definition changes.
CREATE TRIGGER "m_shadow_partition_def_bu" BEFORE UPDATE ON m_shadow_partition_def FOR EACH ROW EXECUTE FUNCTION m_shadow_update_partition();;
-- @description: Drops shadow partition tables when partition definitions are deleted.
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

-- @description: Presents regular objects and shadows through a common object-shaped query surface.
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
-- @description: Stores reference-valued shadow attributes for searching and reverse lookup.
CREATE TABLE m_shadow_ref_attribute (
        -- @description: OID of the shadow that owns the reference attribute value.
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        -- @description: Object type of the reference attribute owner.
        ownerType ObjectType NOT NULL,

        -- @description: URI identifier of the reference attribute path.
        pathId INTEGER NOT NULL,
        -- @description: OID of the resource containing the shadow.
        resourceOid UUID,
        -- @description: URI identifier of the owner shadow object class.
        ownerObjectClassId INTEGER,
        -- @description: OID of the referenced target object.
        targetOid UUID NOT NULL, -- soft-references m_object
        -- @description: Object type of the referenced target object.
        targetType ObjectType NOT NULL,
        -- @description: Relation URI identifier of the reference attribute value.
        relationId INTEGER NOT NULL REFERENCES m_uri(id)
    );

-- @description: Speeds up lookup of reference attributes by owning shadow.
-- @usedFor: shadow reference attribute owner searches
CREATE INDEX m_shadow_ref_attribute_ownerOid_idx ON m_shadow_ref_attribute (ownerOid);


-- @region: system-objects
-- @regionTitle: System objects
-- @regionDescription: Node, system configuration, security policy, and other system-level repository objects.
-- Represents NodeType, see https://docs.evolveum.com/midpoint/reference/deployment/clustering-ha/managing-cluster-nodes/
-- @description: Stores midPoint cluster node objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#NodeType
CREATE TABLE m_node (
    -- @description: Node object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('NODE') STORED
        CHECK (objectType = 'NODE'),
    -- @description: Node identifier used by the cluster.
    nodeIdentifier TEXT,
    -- @description: Operational state of the node.
    operationalState NodeOperationalStateType
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_node`.
CREATE TRIGGER m_node_oid_insert_tr BEFORE INSERT ON m_node
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_node`.
CREATE TRIGGER m_node_update_tr BEFORE UPDATE ON m_node
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_node`.
CREATE TRIGGER m_node_oid_delete_tr AFTER DELETE ON m_node
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_node_nameOrig_idx ON m_node (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_node_nameNorm_key ON m_node (nameNorm);
-- not interested in other indexes for this one, this table will be small

-- Represents SystemConfigurationType, see https://docs.evolveum.com/midpoint/reference/concepts/system-configuration-object/
-- @description: Stores the system configuration object.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SystemConfigurationType
CREATE TABLE m_system_configuration (
    -- @description: System configuration object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SYSTEM_CONFIGURATION') STORED
        CHECK (objectType = 'SYSTEM_CONFIGURATION')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_system_configuration`.
CREATE TRIGGER m_system_configuration_oid_insert_tr BEFORE INSERT ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_system_configuration`.
CREATE TRIGGER m_system_configuration_update_tr BEFORE UPDATE ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_system_configuration`.
CREATE TRIGGER m_system_configuration_oid_delete_tr AFTER DELETE ON m_system_configuration
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_system_configuration_nameNorm_key ON m_system_configuration (nameNorm);
-- no need for the name index, m_system_configuration table is very small

-- Represents SecurityPolicyType, see https://docs.evolveum.com/midpoint/reference/security/security-policy/
-- @description: Stores security policy objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SecurityPolicyType
CREATE TABLE m_security_policy (
    -- @description: Security policy object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SECURITY_POLICY') STORED
        CHECK (objectType = 'SECURITY_POLICY')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_security_policy`.
CREATE TRIGGER m_security_policy_oid_insert_tr BEFORE INSERT ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_security_policy`.
CREATE TRIGGER m_security_policy_update_tr BEFORE UPDATE ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_security_policy`.
CREATE TRIGGER m_security_policy_oid_delete_tr AFTER DELETE ON m_security_policy
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_security_policy_nameOrig_idx ON m_security_policy (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_security_policy_nameNorm_key ON m_security_policy (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_security_policy_subtypes_idx ON m_security_policy USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_security_policy_policySituation_idx
    ON m_security_policy USING gin(policysituations gin__int_ops);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_security_policy_fullTextInfo_idx
    ON m_security_policy USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_security_policy_createTimestamp_idx ON m_security_policy (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_security_policy_modifyTimestamp_idx ON m_security_policy (modifyTimestamp);

-- @region: reports-and-dashboards
-- @regionTitle: Reports and dashboards
-- @regionDescription: Reports, report data, dashboards, object collections, forms, function libraries, and related UI/reporting objects.
-- Represents ObjectCollectionType, see https://docs.evolveum.com/midpoint/reference/admin-gui/collections-views/configuration/
-- @description: Stores object collection definitions used by views, dashboards, and searches.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ObjectCollectionType
CREATE TABLE m_object_collection (
    -- @description: Object collection identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_COLLECTION') STORED
        CHECK (objectType = 'OBJECT_COLLECTION')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_object_collection`.
CREATE TRIGGER m_object_collection_oid_insert_tr BEFORE INSERT ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_object_collection`.
CREATE TRIGGER m_object_collection_update_tr BEFORE UPDATE ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_object_collection`.
CREATE TRIGGER m_object_collection_oid_delete_tr AFTER DELETE ON m_object_collection
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_object_collection_nameOrig_idx ON m_object_collection (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_object_collection_nameNorm_key ON m_object_collection (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_object_collection_subtypes_idx ON m_object_collection USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_object_collection_policySituation_idx
    ON m_object_collection USING gin(policysituations gin__int_ops);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_object_collection_fullTextInfo_idx
    ON m_object_collection USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_object_collection_createTimestamp_idx ON m_object_collection (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_object_collection_modifyTimestamp_idx ON m_object_collection (modifyTimestamp);

-- Represents DashboardType, see https://docs.evolveum.com/midpoint/reference/admin-gui/dashboards/configuration/
-- @description: Stores dashboard objects used by the administrative user interface.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#DashboardType
CREATE TABLE m_dashboard (
    -- @description: Dashboard object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('DASHBOARD') STORED
        CHECK (objectType = 'DASHBOARD')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_dashboard`.
CREATE TRIGGER m_dashboard_oid_insert_tr BEFORE INSERT ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_dashboard`.
CREATE TRIGGER m_dashboard_update_tr BEFORE UPDATE ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_dashboard`.
CREATE TRIGGER m_dashboard_oid_delete_tr AFTER DELETE ON m_dashboard
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_dashboard_nameOrig_idx ON m_dashboard (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_dashboard_nameNorm_key ON m_dashboard (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_dashboard_subtypes_idx ON m_dashboard USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_dashboard_policySituation_idx
    ON m_dashboard USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_dashboard_createTimestamp_idx ON m_dashboard (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_dashboard_modifyTimestamp_idx ON m_dashboard (modifyTimestamp);

-- Represents ValuePolicyType
-- @description: Stores value policy objects, such as password and value generation policies.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ValuePolicyType
CREATE TABLE m_value_policy (
    -- @description: Value policy object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('VALUE_POLICY') STORED
        CHECK (objectType = 'VALUE_POLICY')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_value_policy`.
CREATE TRIGGER m_value_policy_oid_insert_tr BEFORE INSERT ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_value_policy`.
CREATE TRIGGER m_value_policy_update_tr BEFORE UPDATE ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_value_policy`.
CREATE TRIGGER m_value_policy_oid_delete_tr AFTER DELETE ON m_value_policy
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_value_policy_nameOrig_idx ON m_value_policy (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_value_policy_nameNorm_key ON m_value_policy (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_value_policy_subtypes_idx ON m_value_policy USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_value_policy_policySituation_idx
    ON m_value_policy USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_value_policy_createTimestamp_idx ON m_value_policy (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_value_policy_modifyTimestamp_idx ON m_value_policy (modifyTimestamp);

-- Represents ReportType, see https://docs.evolveum.com/midpoint/reference/misc/reports/report-configuration/
-- @description: Stores report definitions.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ReportType
CREATE TABLE m_report (
    -- @description: Report object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT') STORED
        CHECK (objectType = 'REPORT')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_report`.
CREATE TRIGGER m_report_oid_insert_tr BEFORE INSERT ON m_report
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_report`.
CREATE TRIGGER m_report_update_tr BEFORE UPDATE ON m_report
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_report`.
CREATE TRIGGER m_report_oid_delete_tr AFTER DELETE ON m_report
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_report_nameOrig_idx ON m_report (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_report_nameNorm_key ON m_report (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_report_subtypes_idx ON m_report USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_report_policySituation_idx ON m_report USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_report_createTimestamp_idx ON m_report (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_report_modifyTimestamp_idx ON m_report (modifyTimestamp);

-- Represents ReportDataType, see also m_report above
-- @description: Stores report output data objects and their relation to report definitions.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ReportDataType
CREATE TABLE m_report_data (
    -- @description: Report data object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT_DATA') STORED
        CHECK (objectType = 'REPORT_DATA'),
    -- @description: OID of the report definition that produced this report data.
    reportRefTargetOid UUID,
    -- @description: Object type of the referenced report definition.
    reportRefTargetType ObjectType,
    -- @description: Relation URI identifier of the report reference.
    reportRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_report_data`.
CREATE TRIGGER m_report_data_oid_insert_tr BEFORE INSERT ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_report_data`.
CREATE TRIGGER m_report_data_update_tr BEFORE UPDATE ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_report_data`.
CREATE TRIGGER m_report_data_oid_delete_tr AFTER DELETE ON m_report_data
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_report_data_nameOrig_idx ON m_report_data (nameOrig);
-- @description: Speeds up lookup by normalized object name.
-- @usedFor: normalized name lookup
CREATE INDEX m_report_data_nameNorm_idx ON m_report_data (nameNorm); -- not unique
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_report_data_subtypes_idx ON m_report_data USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_report_data_policySituation_idx
    ON m_report_data USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_report_data_createTimestamp_idx ON m_report_data (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_report_data_modifyTimestamp_idx ON m_report_data (modifyTimestamp);


-- @region: role-analysis
-- @regionTitle: Role analysis
-- @regionDescription: Role analysis sessions, clusters, outliers, detected patterns, and related containers.
-- @description: Stores role analysis cluster objects and their parent cluster references.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#RoleAnalysisClusterType
CREATE TABLE m_role_analysis_cluster (
    -- @description: Role analysis cluster object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_CLUSTER') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_CLUSTER'),
        -- @description: OID of the parent cluster reference target.
        parentRefTargetOid UUID,
        -- @description: Object type of the parent cluster reference target.
        parentRefTargetType ObjectType,
        -- @description: Relation URI identifier of the parent cluster reference.
        parentRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_role_analysis_cluster`.
CREATE TRIGGER m_role_analysis_cluster_oid_insert_tr BEFORE INSERT ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_role_analysis_cluster`.
CREATE TRIGGER m_role_analysis_cluster_update_tr BEFORE UPDATE ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_role_analysis_cluster`.
CREATE TRIGGER m_role_analysis_cluster_oid_delete_tr AFTER DELETE ON m_role_analysis_cluster
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup of role analysis clusters by parent reference target OID.
-- @usedFor: role analysis cluster parent searches
CREATE INDEX m_role_analysis_cluster_parentRefTargetOid_idx ON m_role_analysis_cluster (parentRefTargetOid);
-- @description: Speeds up filtering role analysis clusters by parent reference target type.
-- @usedFor: role analysis cluster parent searches
CREATE INDEX m_role_analysis_cluster_parentRefTargetType_idx ON m_role_analysis_cluster (parentRefTargetType);
-- @description: Speeds up filtering role analysis clusters by parent reference relation.
-- @usedFor: role analysis cluster parent searches
CREATE INDEX m_role_analysis_cluster_parentRefRelationId_idx ON m_role_analysis_cluster (parentRefRelationId);


-- @description: Stores detected role analysis patterns inside a role analysis cluster.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#DetectedPatternType
CREATE TABLE m_role_analysis_cluster_detected_pattern (
    -- @description: OID of the role analysis cluster that owns the detected pattern.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('CLUSTER_DETECTED_PATTERN') STORED
        CHECK (containerType = 'CLUSTER_DETECTED_PATTERN'),
    ---
    -- @description: Reduction count calculated for the detected pattern.
    reductionCount double precision,
    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up filtering detected patterns by reduction count.
-- @usedFor: role analysis detected pattern ranking and filters
CREATE INDEX m_role_analysis_cluster_detected_pattern_reductionCount_idx ON m_role_analysis_cluster_detected_pattern (reductionCount);

-- @description: Stores role analysis session objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#RoleAnalysisSessionType
CREATE TABLE m_role_analysis_session (
    -- @description: Role analysis session object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_SESSION') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_SESSION')
        )
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_role_analysis_session`.
CREATE TRIGGER m_role_analysis_session_oid_insert_tr BEFORE INSERT ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_role_analysis_session`.
CREATE TRIGGER m_role_analysis_session_update_tr BEFORE UPDATE ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_role_analysis_session`.
CREATE TRIGGER m_role_analysis_session_oid_delete_tr AFTER DELETE ON m_role_analysis_session
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Stores role analysis outlier objects and references to analyzed target objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#RoleAnalysisOutlierType
CREATE TABLE m_role_analysis_outlier (
    -- @description: Role analysis outlier object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE_ANALYSIS_OUTLIER') STORED
        CHECK (objectType = 'ROLE_ANALYSIS_OUTLIER'),
        -- @description: OID of the object identified as an outlier target.
        targetObjectRefTargetOid UUID,
        -- @description: Object type of the outlier target object.
        targetObjectRefTargetType ObjectType,
        -- @description: Relation URI identifier of the outlier target reference.
        targetObjectRefRelationId INTEGER REFERENCES m_uri(id),
        -- @description: Overall confidence score calculated for the outlier.
        overallConfidence double precision
)
    INHERITS (m_assignment_holder);

-- @description: Speeds up lookup of role analysis outliers by target object OID.
-- @usedFor: role analysis outlier target searches
CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetOid_idx ON m_role_analysis_outlier (targetObjectRefTargetOid);
-- @description: Speeds up filtering role analysis outliers by target object type.
-- @usedFor: role analysis outlier target searches
CREATE INDEX m_role_analysis_outlier_targetObjectRefTargetType_idx ON m_role_analysis_outlier (targetObjectRefTargetType);
-- @description: Speeds up filtering role analysis outliers by target reference relation.
-- @usedFor: role analysis outlier target searches
CREATE INDEX m_role_analysis_outlier_targetObjectRefRelationId_idx ON m_role_analysis_outlier (targetObjectRefRelationId);
-- @description: Speeds up filtering or ordering role analysis outliers by confidence score.
-- @usedFor: role analysis outlier confidence filters and ordering
CREATE INDEX m_role_analysis_outlier_overallConfidence_idx ON m_role_analysis_outlier (overallConfidence);

-- @description: Maintains the object OID registry when rows are inserted into `m_role_analysis_outlier`.
CREATE TRIGGER m_role_analysis_outlier_oid_insert_tr BEFORE INSERT ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_role_analysis_outlier`.
CREATE TRIGGER m_role_analysis_outlier_update_tr BEFORE UPDATE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_role_analysis_outlier`.
CREATE TRIGGER m_role_analysis_outlier_oid_delete_tr AFTER DELETE ON m_role_analysis_outlier
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

    -- @description: Stores partition data for role analysis outliers.
    -- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#RoleAnalysisOutlierPartitionType
    CREATE TABLE m_role_analysis_outlier_partition (
        -- @description: OID of the role analysis outlier that owns this partition row.
        ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
        containerType ContainerType GENERATED ALWAYS AS ('OUTLIER_PARTITION') STORED
            CHECK (containerType = 'OUTLIER_PARTITION'),
        ---
        -- @description: OID of the related role analysis cluster.
        clusterRefOid UUID,
        -- @description: Object type of the related role analysis cluster.
        clusterRefTargetType ObjectType,
        -- @description: Relation URI identifier of the cluster reference.
        clusterRefRelationId INTEGER REFERENCES m_uri(id),
        -- @description: Confidence score calculated for this outlier partition.
        overallConfidence double precision,
        PRIMARY KEY (ownerOid, cid)
    )
        INHERITS(m_container);

-- @description: Speeds up lookup of outlier partitions by cluster OID.
-- @usedFor: role analysis outlier partition cluster searches
CREATE INDEX m_role_analysis_outlier_partition_clusterRefOid_idx ON m_role_analysis_outlier_partition (clusterRefOid);
-- @description: Speeds up filtering or ordering outlier partitions by confidence score.
-- @usedFor: role analysis outlier confidence filters and ordering
CREATE INDEX  m_role_analysis_outlier_partition_overallConfidence_idx ON m_role_analysis_outlier_partition (overallConfidence);


-- @region: lookup-tables
-- @regionTitle: Lookup tables
-- @regionDescription: Lookup table objects and their key-value row data.
-- Represents LookupTableType, see https://docs.evolveum.com/midpoint/reference/misc/lookup-tables/
-- @description: Stores lookup table objects used for reusable key-value mappings.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#LookupTableType
CREATE TABLE m_lookup_table (
    -- @description: Lookup table object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('LOOKUP_TABLE') STORED
        CHECK (objectType = 'LOOKUP_TABLE')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_lookup_table`.
CREATE TRIGGER m_lookup_table_oid_insert_tr BEFORE INSERT ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_lookup_table`.
CREATE TRIGGER m_lookup_table_update_tr BEFORE UPDATE ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_lookup_table`.
CREATE TRIGGER m_lookup_table_oid_delete_tr AFTER DELETE ON m_lookup_table
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_lookup_table_nameOrig_idx ON m_lookup_table (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_lookup_table_nameNorm_key ON m_lookup_table (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_lookup_table_subtypes_idx ON m_lookup_table USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_lookup_table_policySituation_idx
    ON m_lookup_table USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_lookup_table_createTimestamp_idx ON m_lookup_table (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_lookup_table_modifyTimestamp_idx ON m_lookup_table (modifyTimestamp);

-- Represents LookupTableRowType, see also m_lookup_table above
/*
@description: Stores rows of lookup table key-value data.

Lookup table row currently doesn't store whole polystring data for `label` property,
only the original and normalized string values are stored.
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#LookupTableRowType
CREATE TABLE m_lookup_table_row (
    -- @description: OID of the lookup table that owns this row.
    ownerOid UUID NOT NULL REFERENCES m_lookup_table(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('LOOKUP_TABLE_ROW') STORED
        CHECK (containerType = 'LOOKUP_TABLE_ROW'),
    -- @description: Lookup table row key.
    key TEXT,
    -- @description: Lookup table row value.
    value TEXT,
    -- @description: Lookup table row label in original form.
    labelOrig TEXT,
    -- @description: Lookup table row label in normalized form.
    labelNorm TEXT,
    -- @description: Time when this lookup table row last changed.
    lastChangeTimestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Enforces unique lookup table row keys within one lookup table.
-- @usedFor: lookup table row key lookup and uniqueness checks
CREATE UNIQUE INDEX m_lookup_table_row_ownerOid_key_key ON m_lookup_table_row (ownerOid, key);

-- Represents ConnectorType, see https://docs.evolveum.com/connectors/connectors/
-- @description: Stores connector definitions discovered or configured in midPoint.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ConnectorType
CREATE TABLE m_connector (
    -- @description: Connector object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR') STORED
        CHECK (objectType = 'CONNECTOR'),
    -- @description: Connector bundle name, typically a package name.
    connectorBundle TEXT, -- typically a package name
    -- @description: Connector type, typically a connector class name.
    connectorType TEXT NOT NULL, -- typically a class name
    -- @description: Connector version.
    connectorVersion TEXT NOT NULL,
    -- @description: URI identifier of the connector framework.
    frameworkId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the connector host reference target.
    connectorHostRefTargetOid UUID,
    -- @description: Object type of the connector host reference target.
    connectorHostRefTargetType ObjectType,
    -- @description: Relation URI identifier of the connector host reference.
    connectorHostRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Connector display name in original form.
    displayNameOrig TEXT,
    -- @description: Connector display name in normalized form.
    displayNameNorm TEXT,
    -- @description: URI identifiers of target system types supported by the connector.
    targetSystemTypes INTEGER[],
    -- @description: Marks whether the connector is available.
    available BOOLEAN
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_connector`.
CREATE TRIGGER m_connector_oid_insert_tr BEFORE INSERT ON m_connector
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_connector`.
CREATE TRIGGER m_connector_update_tr BEFORE UPDATE ON m_connector
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_connector`.
CREATE TRIGGER m_connector_oid_delete_tr AFTER DELETE ON m_connector
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up connector lookup by type and version for local connectors.
-- @usedFor: connector resolution without connector host
CREATE INDEX m_connector_typeVersion_key
    ON m_connector (connectorType, connectorVersion)
    WHERE connectorHostRefTargetOid IS NULL;
-- @description: Speeds up connector lookup by type, version, and connector host.
-- @usedFor: connector resolution for remote connector hosts
CREATE INDEX m_connector_typeVersionHost_key
    ON m_connector (connectorType, connectorVersion, connectorHostRefTargetOid)
    WHERE connectorHostRefTargetOid IS NOT NULL;
-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_connector_nameOrig_idx ON m_connector (nameOrig);
-- @description: Speeds up lookup by normalized object name.
-- @usedFor: normalized name lookup
CREATE INDEX m_connector_nameNorm_idx ON m_connector (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_connector_subtypes_idx ON m_connector USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_connector_policySituation_idx
    ON m_connector USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_connector_createTimestamp_idx ON m_connector (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_connector_modifyTimestamp_idx ON m_connector (modifyTimestamp);

-- Represents Connector Development table
-- @description: Stores connector development objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ConnectorDevelopmentType
CREATE TABLE m_connector_development (
     -- @description: Connector development object identifier.
     oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
     objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_DEVELOPMENT') STORED
        CHECK (objectType = 'CONNECTOR_DEVELOPMENT')
)
    INHERITS (m_object);

-- @description: Maintains the object OID registry when rows are inserted into `m_connector_development`.
CREATE TRIGGER m_connector_development_oid_insert_tr BEFORE INSERT ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_connector_development`.
CREATE TRIGGER m_connector_development_update_tr BEFORE UPDATE ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_connector_development`.
CREATE TRIGGER m_connector_development_oid_delete_tr AFTER DELETE ON m_connector_development
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();



/*
@description: Stores remote connector host definitions.

Represents ConnectorHostType, see https://docs.evolveum.com/connectors/connid/1.x/connector-server/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ConnectorHostType
CREATE TABLE m_connector_host (
    -- @description: Connector host object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_HOST') STORED
        CHECK (objectType = 'CONNECTOR_HOST'),
    -- @description: Connector host hostname.
    hostname TEXT,
    -- @description: Connector host port.
    port TEXT
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_connector_host`.
CREATE TRIGGER m_connector_host_oid_insert_tr BEFORE INSERT ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_connector_host`.
CREATE TRIGGER m_connector_host_update_tr BEFORE UPDATE ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_connector_host`.
CREATE TRIGGER m_connector_host_oid_delete_tr AFTER DELETE ON m_connector_host
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_connector_host_nameOrig_idx ON m_connector_host (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_connector_host_nameNorm_key ON m_connector_host (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_connector_host_subtypes_idx ON m_connector_host USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_connector_host_policySituation_idx
    ON m_connector_host USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_connector_host_createTimestamp_idx ON m_connector_host (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_connector_host_modifyTimestamp_idx ON m_connector_host (modifyTimestamp);

-- @region: tasks
-- @regionTitle: Tasks
-- @regionDescription: Task objects, task containers, affected objects, triggers, and operation execution data.
/*
@description: Stores persistent task objects managed by the task manager.

Represents persistent TaskType, see https://docs.evolveum.com/midpoint/reference/tasks/task-manager/
 */
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TaskType
CREATE TABLE m_task (
    -- @description: Task object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('TASK') STORED
        CHECK (objectType = 'TASK'),
    -- @description: Stable task identifier used by task manager logic.
    taskIdentifier TEXT,
    -- @description: Task binding mode controlling node affinity.
    binding TaskBindingType,
    category TEXT, -- TODO revise, deprecated, probably can go away soon
    -- @description: Time when the task completed.
    completionTimestamp TIMESTAMPTZ,
    -- @description: Current task execution state.
    executionState TaskExecutionStateType,
    -- Logically fullResult and resultStatus are related, managed by Task manager.
    -- @description: Serialized full operation result for the task.
    fullResult BYTEA,
    -- @description: Summary operation result status for the task.
    resultStatus OperationResultStatusType,
    -- @description: URI identifier of the task handler.
    handlerUriId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the last task run started.
    lastRunStartTimestamp TIMESTAMPTZ,
    -- @description: Time when the last task run finished.
    lastRunFinishTimestamp TIMESTAMPTZ,
    node TEXT, -- nodeId only for information purposes
    -- @description: OID of the task object reference target.
    objectRefTargetOid UUID,
    -- @description: Object type of the task object reference target.
    objectRefTargetType ObjectType,
    -- @description: Relation URI identifier of the task object reference.
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the task owner reference target.
    ownerRefTargetOid UUID,
    -- @description: Object type of the task owner reference target.
    ownerRefTargetType ObjectType,
    -- @description: Relation URI identifier of the task owner reference.
    ownerRefRelationId INTEGER REFERENCES m_uri(id),
    parent TEXT, -- value of taskIdentifier
    -- @description: Task recurrence mode.
    recurrence TaskRecurrenceType,
    -- @description: Task scheduling state.
    schedulingState TaskSchedulingStateType,
    autoScalingMode TaskAutoScalingModeType, -- autoScaling/mode
    -- @description: Action taken when a task thread is stopped.
    threadStopAction ThreadStopActionType,
    -- @description: Reason why the task is waiting.
    waitingReason TaskWaitingReasonType,
    -- @description: Identifiers of tasks this task depends on.
    dependentTaskIdentifiers TEXT[] -- contains values of taskIdentifier
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_task`.
CREATE TRIGGER m_task_oid_insert_tr BEFORE INSERT ON m_task
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_task`.
CREATE TRIGGER m_task_update_tr BEFORE UPDATE ON m_task
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_task`.
CREATE TRIGGER m_task_oid_delete_tr AFTER DELETE ON m_task
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_task_nameOrig_idx ON m_task (nameOrig);
-- @description: Speeds up lookup by normalized object name.
-- @usedFor: normalized name lookup
CREATE INDEX m_task_nameNorm_idx ON m_task (nameNorm); -- can have duplicates
-- @description: Speeds up lookup of task children by parent task identifier.
-- @usedFor: task hierarchy searches
CREATE INDEX m_task_parent_idx ON m_task (parent);
-- @description: Speeds up lookup of tasks by referenced object.
-- @usedFor: task object reference searches
CREATE INDEX m_task_objectRefTargetOid_idx ON m_task(objectRefTargetOid);
-- @description: Enforces unique task identifiers and speeds up task lookup by identifier.
-- @usedFor: task identifier lookup and uniqueness checks
CREATE UNIQUE INDEX m_task_taskIdentifier_key ON m_task (taskIdentifier);
-- @description: Speeds up lookup of tasks by dependent task identifiers.
-- @usedFor: task dependency searches
CREATE INDEX m_task_dependentTaskIdentifiers_idx ON m_task USING gin(dependentTaskIdentifiers);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_task_subtypes_idx ON m_task USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_task_policySituation_idx ON m_task USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_task_ext_idx ON m_task USING gin(ext);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_task_fullTextInfo_idx ON m_task USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_task_createTimestamp_idx ON m_task (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_task_modifyTimestamp_idx ON m_task (modifyTimestamp);

-- @description: Stores affected object information for task activity reporting and indexing.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ActivityAffectedObjectsType
CREATE TABLE m_task_affected_objects (
    -- @description: OID of the task that owns the affected object entry.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('AFFECTED_OBJECTS') STORED
     CHECK (containerType = 'AFFECTED_OBJECTS'),
    -- @description: URI identifier of the task activity.
    activityId INTEGER REFERENCES m_uri(id),
    -- @description: Object type of the affected object.
    type ObjectType,
    -- @description: OID of the affected object's archetype reference target.
    archetypeRefTargetOid UUID,
    -- @description: Object type of the affected object's archetype reference target.
    archetypeRefTargetType ObjectType,
    -- @description: Relation URI identifier of the affected object's archetype reference.
    archetypeRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the affected object class.
    objectClassId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the affected object's resource reference target.
    resourceRefTargetOid UUID,
    -- @description: Object type of the affected object's resource reference target.
    resourceRefTargetType ObjectType,
    -- @description: Relation URI identifier of the affected object's resource reference.
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Intent of the affected resource object.
    intent TEXT,
    -- @description: Kind of the affected shadow object.
    kind ShadowKindType,
    -- @description: Execution mode associated with the affected object.
    executionMode ExecutionModeType,
    -- @description: Predefined configuration used for the affected object.
    predefinedConfigurationToUse PredefinedConfigurationType,
    PRIMARY KEY (ownerOid, cid)
) INHERITS(m_container);

-- @description: Stores target references for task affected object entries.
CREATE TABLE m_ref_task_affected_object (
    -- @description: OID of the task that owns the affected object reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the affected object entry.
    affectedObjectCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('TASK_AFFECTED_OBJECT') STORED
        CHECK (referenceType = 'TASK_AFFECTED_OBJECT')
)
    INHERITS (m_reference);

ALTER TABLE m_ref_task_affected_object ADD CONSTRAINT m_ref_task_affected_object_id_fk
    FOREIGN KEY (ownerOid, affectedObjectCid) REFERENCES m_task_affected_objects (ownerOid, cid)
        ON DELETE CASCADE;

-- @description: Speeds up reverse lookup of task affected object references by target object and relation.
-- @usedFor: task affected object target searches
CREATE INDEX m_ref_task_affected_object_targetOidRelationId_idx
    ON m_ref_task_affected_object (targetOid, relationId);
-- endregion

-- @region: cases-and-certification
-- @regionTitle: Cases and certification
-- @regionDescription: Case management, access certification campaigns, certification cases, work items, and reviewer references.
-- region cases
-- Represents CaseType, see https://docs.evolveum.com/midpoint/features/planned/case-management/
-- @description: Stores case objects used for approvals, manual actions, and other case-management workflows.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#CaseType
CREATE TABLE m_case (
    -- @description: Case object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CASE') STORED
        CHECK (objectType = 'CASE'),
    -- @description: Case state.
    state TEXT,
    -- @description: Time when the case was closed.
    closeTimestamp TIMESTAMPTZ,
    -- @description: OID of the object reference target.
    objectRefTargetOid UUID,
    -- @description: Object type of the object reference target.
    objectRefTargetType ObjectType,
    -- @description: Relation URI identifier of the object reference.
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the parent case reference target.
    parentRefTargetOid UUID,
    -- @description: Object type of the parent case reference target.
    parentRefTargetType ObjectType,
    -- @description: Relation URI identifier of the parent case reference.
    parentRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the requestor reference target.
    requestorRefTargetOid UUID,
    -- @description: Object type of the requestor reference target.
    requestorRefTargetType ObjectType,
    -- @description: Relation URI identifier of the requestor reference.
    requestorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the case target reference target.
    targetRefTargetOid UUID,
    -- @description: Object type of the case target reference target.
    targetRefTargetType ObjectType,
    -- @description: Relation URI identifier of the case target reference.
    targetRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_case`.
CREATE TRIGGER m_case_oid_insert_tr BEFORE INSERT ON m_case
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_case`.
CREATE TRIGGER m_case_update_tr BEFORE UPDATE ON m_case
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_case`.
CREATE TRIGGER m_case_oid_delete_tr AFTER DELETE ON m_case
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_case_nameOrig_idx ON m_case (nameOrig);
-- @description: Speeds up lookup by normalized object name.
-- @usedFor: normalized name lookup
CREATE INDEX m_case_nameNorm_idx ON m_case (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_case_subtypes_idx ON m_case USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_case_policySituation_idx ON m_case USING gin(policysituations gin__int_ops);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_case_fullTextInfo_idx ON m_case USING gin(fullTextInfo gin_trgm_ops);

-- @description: Speeds up lookup of cases by object reference target.
-- @usedFor: case object reference searches
CREATE INDEX m_case_objectRefTargetOid_idx ON m_case(objectRefTargetOid);
-- @description: Speeds up lookup of cases by target reference target.
-- @usedFor: case target reference searches
CREATE INDEX m_case_targetRefTargetOid_idx ON m_case(targetRefTargetOid);
-- @description: Speeds up lookup of child cases by parent reference target.
-- @usedFor: case parent reference searches
CREATE INDEX m_case_parentRefTargetOid_idx ON m_case(parentRefTargetOid);
-- @description: Speeds up lookup of cases by requestor reference target.
-- @usedFor: case requestor reference searches
CREATE INDEX m_case_requestorRefTargetOid_idx ON m_case(requestorRefTargetOid);
-- @description: Speeds up filtering or ordering cases by close time.
-- @usedFor: case close time filters and ordering
CREATE INDEX m_case_closeTimestamp_idx ON m_case(closeTimestamp);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_case_createTimestamp_idx ON m_case (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_case_modifyTimestamp_idx ON m_case (modifyTimestamp);

-- @description: Stores case work items assigned to users or groups.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#CaseWorkItemType
CREATE TABLE m_case_wi (
    -- @description: OID of the case that owns the work item.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('CASE_WORK_ITEM') STORED
        CHECK (containerType = 'CASE_WORK_ITEM'),
    -- @description: Time when the work item was closed.
    closeTimestamp TIMESTAMPTZ,
    -- @description: Time when the work item was created.
    createTimestamp TIMESTAMPTZ,
    -- @description: Work item deadline.
    deadline TIMESTAMPTZ,
    -- @description: OID of the original assignee reference target.
    originalAssigneeRefTargetOid UUID,
    -- @description: Object type of the original assignee reference target.
    originalAssigneeRefTargetType ObjectType,
    -- @description: Relation URI identifier of the original assignee reference.
    originalAssigneeRefRelationId INTEGER REFERENCES m_uri(id),
    outcome TEXT, -- stores workitem/output/outcome
    -- @description: OID of the performer reference target.
    performerRefTargetOid UUID,
    -- @description: Object type of the performer reference target.
    performerRefTargetType ObjectType,
    -- @description: Relation URI identifier of the performer reference.
    performerRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Certification or approval stage number associated with the work item.
    stageNumber INTEGER,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up filtering or ordering case work items by creation time.
-- @usedFor: case work item creation time filters and ordering
CREATE INDEX m_case_wi_createTimestamp_idx ON m_case_wi (createTimestamp);
-- @description: Speeds up filtering or ordering case work items by close time.
-- @usedFor: case work item close time filters and ordering
CREATE INDEX m_case_wi_closeTimestamp_idx ON m_case_wi (closeTimestamp);
-- @description: Speeds up filtering or ordering case work items by deadline.
-- @usedFor: case work item deadline filters and ordering
CREATE INDEX m_case_wi_deadline_idx ON m_case_wi (deadline);
-- @description: Speeds up lookup by original assignee reference target.
-- @usedFor: case work item original assignee searches
CREATE INDEX m_case_wi_originalAssigneeRefTargetOid_idx ON m_case_wi (originalAssigneeRefTargetOid);
-- @description: Speeds up lookup by performer reference target.
-- @usedFor: case work item performer searches
CREATE INDEX m_case_wi_performerRefTargetOid_idx ON m_case_wi (performerRefTargetOid);

-- stores workItem/assigneeRef
-- @description: Stores assignee references for case work items.
CREATE TABLE m_case_wi_assignee (
    -- @description: OID of the case that owns the work item.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the work item.
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_ASSIGNEE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first three PK columns
ALTER TABLE m_case_wi_assignee ADD CONSTRAINT m_case_wi_assignee_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid)
        ON DELETE CASCADE;

-- @description: Speeds up reverse lookup of case work item assignees by target object and relation.
-- @usedFor: case work item assignee searches
CREATE INDEX m_case_wi_assignee_targetOidRelationId_idx
    ON m_case_wi_assignee (targetOid, relationId);

-- stores workItem/candidateRef
-- @description: Stores candidate references for case work items.
CREATE TABLE m_case_wi_candidate (
    -- @description: OID of the case that owns the work item.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the work item.
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_CANDIDATE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

-- indexed by first two PK columns
ALTER TABLE m_case_wi_candidate ADD CONSTRAINT m_case_wi_candidate_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid)
        ON DELETE CASCADE;

-- @description: Speeds up reverse lookup of case work item candidates by target object and relation.
-- @usedFor: case work item candidate searches
CREATE INDEX m_case_wi_candidate_targetOidRelationId_idx
    ON m_case_wi_candidate (targetOid, relationId);
-- endregion

-- region Access Certification object tables
-- Represents AccessCertificationDefinitionType, see https://docs.evolveum.com/midpoint/reference/roles-policies/certification/
-- @description: Stores access certification definition objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AccessCertificationDefinitionType
CREATE TABLE m_access_cert_definition (
    -- @description: Access certification definition object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_DEFINITION') STORED
        CHECK (objectType = 'ACCESS_CERTIFICATION_DEFINITION'),
    -- @description: URI identifier of the certification handler.
    handlerUriId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the last campaign from this definition started.
    lastCampaignStartedTimestamp TIMESTAMPTZ,
    -- @description: Time when the last campaign from this definition closed.
    lastCampaignClosedTimestamp TIMESTAMPTZ,
    -- @description: OID of the definition owner reference target.
    ownerRefTargetOid UUID,
    -- @description: Object type of the definition owner reference target.
    ownerRefTargetType ObjectType,
    -- @description: Relation URI identifier of the definition owner reference.
    ownerRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_access_cert_definition`.
CREATE TRIGGER m_access_cert_definition_oid_insert_tr BEFORE INSERT ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_access_cert_definition`.
CREATE TRIGGER m_access_cert_definition_update_tr BEFORE UPDATE ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_access_cert_definition`.
CREATE TRIGGER m_access_cert_definition_oid_delete_tr AFTER DELETE ON m_access_cert_definition
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_access_cert_definition_nameOrig_idx ON m_access_cert_definition (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_access_cert_definition_nameNorm_key ON m_access_cert_definition (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_access_cert_definition_subtypes_idx ON m_access_cert_definition USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_access_cert_definition_policySituation_idx
    ON m_access_cert_definition USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_access_cert_definition_ext_idx ON m_access_cert_definition USING gin(ext);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_access_cert_definition_fullTextInfo_idx
    ON m_access_cert_definition USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_access_cert_definition_createTimestamp_idx ON m_access_cert_definition (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_access_cert_definition_modifyTimestamp_idx ON m_access_cert_definition (modifyTimestamp);

-- @description: Stores access certification campaign objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AccessCertificationCampaignType
CREATE TABLE m_access_cert_campaign (
    -- @description: Access certification campaign object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CAMPAIGN') STORED
        CHECK (objectType = 'ACCESS_CERTIFICATION_CAMPAIGN'),
    -- @description: OID of the certification definition reference target.
    definitionRefTargetOid UUID,
    -- @description: Object type of the certification definition reference target.
    definitionRefTargetType ObjectType,
    -- @description: Relation URI identifier of the certification definition reference.
    definitionRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the certification campaign ended.
    endTimestamp TIMESTAMPTZ,
    -- @description: URI identifier of the certification campaign handler.
    handlerUriId INTEGER REFERENCES m_uri(id),
    -- @description: Campaign iteration number.
    campaignIteration INTEGER NOT NULL,
    -- @description: OID of the campaign owner reference target.
    ownerRefTargetOid UUID,
    -- @description: Object type of the campaign owner reference target.
    ownerRefTargetType ObjectType,
    -- @description: Relation URI identifier of the campaign owner reference.
    ownerRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Current campaign stage number.
    stageNumber INTEGER,
    -- @description: Time when the certification campaign started.
    startTimestamp TIMESTAMPTZ,
    -- @description: Current certification campaign state.
    state AccessCertificationCampaignStateType
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_access_cert_campaign`.
CREATE TRIGGER m_access_cert_campaign_oid_insert_tr BEFORE INSERT ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_access_cert_campaign`.
CREATE TRIGGER m_access_cert_campaign_update_tr BEFORE UPDATE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_access_cert_campaign`.
CREATE TRIGGER m_access_cert_campaign_oid_delete_tr AFTER DELETE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_access_cert_campaign_nameOrig_idx ON m_access_cert_campaign (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_access_cert_campaign_nameNorm_key ON m_access_cert_campaign (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_access_cert_campaign_subtypes_idx ON m_access_cert_campaign USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_access_cert_campaign_policySituation_idx
    ON m_access_cert_campaign USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_access_cert_campaign_ext_idx ON m_access_cert_campaign USING gin(ext);
-- @description: Speeds up full-text-like object searches.
-- @usedFor: full-text-like object searches
CREATE INDEX m_access_cert_campaign_fullTextInfo_idx
    ON m_access_cert_campaign USING gin(fullTextInfo gin_trgm_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_access_cert_campaign_createTimestamp_idx ON m_access_cert_campaign (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_access_cert_campaign_modifyTimestamp_idx ON m_access_cert_campaign (modifyTimestamp);

-- @description: Stores access certification cases inside certification campaigns.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AccessCertificationCaseType
CREATE TABLE m_access_cert_case (
    -- @description: OID of the access certification campaign that owns the case.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CASE') STORED
        CHECK (containerType = 'ACCESS_CERTIFICATION_CASE'),
    -- @description: Administrative activation status of the certified object.
    administrativeStatus ActivationStatusType,
    -- @description: Archive timestamp of the certified object.
    archiveTimestamp TIMESTAMPTZ,
    -- @description: Disable reason of the certified object.
    disableReason TEXT,
    -- @description: Disable timestamp of the certified object.
    disableTimestamp TIMESTAMPTZ,
    -- @description: Effective activation status of the certified object.
    effectiveStatus ActivationStatusType,
    -- @description: Enable timestamp of the certified object.
    enableTimestamp TIMESTAMPTZ,
    -- @description: Validity interval start of the certified object.
    validFrom TIMESTAMPTZ,
    -- @description: Validity interval end of the certified object.
    validTo TIMESTAMPTZ,
    -- @description: Time when validity state last changed.
    validityChangeTimestamp TIMESTAMPTZ,
    -- @description: Validity interval status.
    validityStatus TimeIntervalStatusType,
    -- @description: Outcome of the current certification stage.
    currentStageOutcome TEXT,
    -- @description: Serialized full certification case value.
    fullObject BYTEA,
    -- @description: Campaign iteration number for this case.
    campaignIteration INTEGER NOT NULL,
    -- @description: OID of the certified object reference target.
    objectRefTargetOid UUID,
    -- @description: Object type of the certified object reference target.
    objectRefTargetType ObjectType,
    -- @description: Relation URI identifier of the certified object reference.
    objectRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the organization reference target.
    orgRefTargetOid UUID,
    -- @description: Object type of the organization reference target.
    orgRefTargetType ObjectType,
    -- @description: Relation URI identifier of the organization reference.
    orgRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Overall certification case outcome.
    outcome TEXT,
    -- @description: Time when the certification case was remediated.
    remediedTimestamp TIMESTAMPTZ,
    -- @description: Deadline of the current certification stage.
    currentStageDeadline TIMESTAMPTZ,
    -- @description: Creation time of the current certification stage.
    currentStageCreateTimestamp TIMESTAMPTZ,
    -- @description: Current certification stage number.
    stageNumber INTEGER,
    -- @description: OID of the certification target reference target.
    targetRefTargetOid UUID,
    -- @description: Object type of the certification target reference target.
    targetRefTargetType ObjectType,
    -- @description: Relation URI identifier of the certification target reference.
    targetRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the tenant reference target.
    tenantRefTargetOid UUID,
    -- @description: Object type of the tenant reference target.
    tenantRefTargetType ObjectType,
    -- @description: Relation URI identifier of the tenant reference.
    tenantRefRelationId INTEGER REFERENCES m_uri(id),

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up lookup of certification cases by certified object.
-- @usedFor: certification case object searches
CREATE INDEX m_access_cert_case_objectRefTargetOid_idx ON m_access_cert_case (objectRefTargetOid);
-- @description: Speeds up lookup of certification cases by target object.
-- @usedFor: certification case target searches
CREATE INDEX m_access_cert_case_targetRefTargetOid_idx ON m_access_cert_case (targetRefTargetOid);
-- @description: Speeds up lookup of certification cases by tenant.
-- @usedFor: certification case tenant filters
CREATE INDEX m_access_cert_case_tenantRefTargetOid_idx ON m_access_cert_case (tenantRefTargetOid);
-- @description: Speeds up lookup of certification cases by organization.
-- @usedFor: certification case organization filters
CREATE INDEX m_access_cert_case_orgRefTargetOid_idx ON m_access_cert_case (orgRefTargetOid);

-- @description: Stores work items for access certification cases.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AccessCertificationWorkItemType
CREATE TABLE m_access_cert_wi (
    -- @description: OID of the certification campaign that owns the work item.
    ownerOid UUID NOT NULL, -- PK+FK
    -- @description: Container ID of the certification case that owns the work item.
    accessCertCaseCid INTEGER NOT NULL, -- PK+FK
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_WORK_ITEM') STORED
        CHECK (containerType = 'ACCESS_CERTIFICATION_WORK_ITEM'),
    -- @description: Time when the certification work item was closed.
    closeTimestamp TIMESTAMPTZ,
    -- @description: Campaign iteration number for this work item.
    campaignIteration INTEGER NOT NULL,
    outcome TEXT, -- stores output/outcome
    -- @description: Time when the work item output changed.
    outputChangeTimestamp TIMESTAMPTZ,
    -- @description: OID of the performer reference target.
    performerRefTargetOid UUID,
    -- @description: Object type of the performer reference target.
    performerRefTargetType ObjectType,
    -- @description: Relation URI identifier of the performer reference.
    performerRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Certification stage number for this work item.
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
-- @description: Stores assignee references for access certification work items.
CREATE TABLE m_access_cert_wi_assignee (
    -- @description: OID of the certification campaign that owns the work item.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the certification case.
    accessCertCaseCid INTEGER NOT NULL,
    -- @description: Container ID of the certification work item.
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

-- @description: Speeds up reverse lookup of certification work item assignees by target object and relation.
-- @usedFor: certification work item assignee searches
CREATE INDEX m_access_cert_wi_assignee_targetOidRelationId_idx
    ON m_access_cert_wi_assignee (targetOid, relationId);

-- stores case/workItem/candidateRef
-- @description: Stores candidate references for access certification work items.
CREATE TABLE m_access_cert_wi_candidate (
    -- @description: OID of the certification campaign that owns the work item.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the certification case.
    accessCertCaseCid INTEGER NOT NULL,
    -- @description: Container ID of the certification work item.
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

-- @description: Speeds up reverse lookup of certification work item candidates by target object and relation.
-- @usedFor: certification work item candidate searches
CREATE INDEX m_access_cert_wi_candidate_targetOidRelationId_idx
    ON m_access_cert_wi_candidate (targetOid, relationId);
-- endregion

-- @region: templates-and-messages
-- @regionTitle: Templates and messages
-- @regionDescription: Object templates, include references, sequences, forms, function libraries, and message templates.
-- region ObjectTemplateType
-- @description: Stores object template definitions.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ObjectTemplateType
CREATE TABLE m_object_template (
    -- @description: Object template identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_TEMPLATE') STORED
        CHECK (objectType = 'OBJECT_TEMPLATE')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_object_template`.
CREATE TRIGGER m_object_template_oid_insert_tr BEFORE INSERT ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_object_template`.
CREATE TRIGGER m_object_template_update_tr BEFORE UPDATE ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_object_template`.
CREATE TRIGGER m_object_template_oid_delete_tr AFTER DELETE ON m_object_template
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_object_template_nameOrig_idx ON m_object_template (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_object_template_nameNorm_key ON m_object_template (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_object_template_subtypes_idx ON m_object_template USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_object_template_policySituation_idx
    ON m_object_template USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_object_template_createTimestamp_idx ON m_object_template (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_object_template_modifyTimestamp_idx ON m_object_template (modifyTimestamp);

-- stores ObjectTemplateType/includeRef
-- @description: Stores include references between object templates.
CREATE TABLE m_ref_include (
    -- @description: OID of the object template that owns the include reference.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('INCLUDE') STORED
        CHECK (referenceType = 'INCLUDE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of include references by target object and relation.
-- @usedFor: object template include reference searches
CREATE INDEX m_ref_include_targetOidRelationId_idx
    ON m_ref_include (targetOid, relationId);
-- endregion

-- region FunctionLibrary/Sequence/Form tables
-- Represents FunctionLibraryType, see https://docs.evolveum.com/midpoint/reference/expressions/function-libraries/
-- @description: Stores function library objects used by expressions.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#FunctionLibraryType
CREATE TABLE m_function_library (
    -- @description: Function library object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FUNCTION_LIBRARY') STORED
        CHECK (objectType = 'FUNCTION_LIBRARY')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_function_library`.
CREATE TRIGGER m_function_library_oid_insert_tr BEFORE INSERT ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_function_library`.
CREATE TRIGGER m_function_library_update_tr BEFORE UPDATE ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_function_library`.
CREATE TRIGGER m_function_library_oid_delete_tr AFTER DELETE ON m_function_library
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_function_library_nameOrig_idx ON m_function_library (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_function_library_nameNorm_key ON m_function_library (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_function_library_subtypes_idx ON m_function_library USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_function_library_policySituation_idx
    ON m_function_library USING gin(policysituations gin__int_ops);

-- Represents SequenceType, see https://docs.evolveum.com/midpoint/reference/expressions/sequences/
-- @description: Stores sequence objects used for generated values.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SequenceType
CREATE TABLE m_sequence (
    -- @description: Sequence object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SEQUENCE') STORED
        CHECK (objectType = 'SEQUENCE')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_sequence`.
CREATE TRIGGER m_sequence_oid_insert_tr BEFORE INSERT ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_sequence`.
CREATE TRIGGER m_sequence_update_tr BEFORE UPDATE ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_sequence`.
CREATE TRIGGER m_sequence_oid_delete_tr AFTER DELETE ON m_sequence
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_sequence_nameOrig_idx ON m_sequence (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_sequence_nameNorm_key ON m_sequence (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_sequence_subtypes_idx ON m_sequence USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_sequence_policySituation_idx ON m_sequence USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_sequence_createTimestamp_idx ON m_sequence (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_sequence_modifyTimestamp_idx ON m_sequence (modifyTimestamp);

-- Represents FormType, see https://docs.evolveum.com/midpoint/reference/admin-gui/custom-forms/
-- @description: Stores custom form definitions.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#FormType
CREATE TABLE m_form (
    -- @description: Form object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FORM') STORED
        CHECK (objectType = 'FORM')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_form`.
CREATE TRIGGER m_form_oid_insert_tr BEFORE INSERT ON m_form
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_form`.
CREATE TRIGGER m_form_update_tr BEFORE UPDATE ON m_form
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_form`.
CREATE TRIGGER m_form_oid_delete_tr AFTER DELETE ON m_form
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_form_nameOrig_idx ON m_form (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_form_nameNorm_key ON m_form (nameNorm);
-- @description: Speeds up filtering by object subtype.
-- @usedFor: subtype filters
CREATE INDEX m_form_subtypes_idx ON m_form USING gin(subtypes);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_form_policySituation_idx ON m_form USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_form_createTimestamp_idx ON m_form (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_form_modifyTimestamp_idx ON m_form (modifyTimestamp);
-- endregion

-- region Notification and message transport
-- Represents MessageTemplateType
-- @description: Stores reusable message templates for notifications and message transport.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#MessageTemplateType
CREATE TABLE m_message_template (
    -- @description: Message template object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('MESSAGE_TEMPLATE') STORED
        CHECK (objectType = 'MESSAGE_TEMPLATE')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_message_template`.
CREATE TRIGGER m_message_template_oid_insert_tr BEFORE INSERT ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_message_template`.
CREATE TRIGGER m_message_template_update_tr BEFORE UPDATE ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_message_template`.
CREATE TRIGGER m_message_template_oid_delete_tr AFTER DELETE ON m_message_template
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Speeds up lookup by original object name.
-- @usedFor: original name searches
CREATE INDEX m_message_template_nameOrig_idx ON m_message_template (nameOrig);
-- @description: Enforces unique normalized object names and speeds up exact name lookup.
-- @usedFor: normalized name lookup and uniqueness checks
CREATE UNIQUE INDEX m_message_template_nameNorm_key ON m_message_template (nameNorm);
-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_message_template_policySituation_idx
    ON m_message_template USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_message_template_createTimestamp_idx ON m_message_template (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_message_template_modifyTimestamp_idx ON m_message_template (modifyTimestamp);
-- endregion

-- @region: assignments
-- @regionTitle: Assignments
-- @regionDescription: Assignment and inducement containers, assignment metadata, and assignment reference tables.
-- region Assignment/Inducement table
-- Represents AssignmentType, see https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/
-- and also https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/assignment-vs-inducement/
-- @description: Stores assignment and inducement containers for assignment-holding objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#AssignmentType
CREATE TABLE m_assignment (
    -- @description: OID of the object that owns the assignment or inducement.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    -- @description: Assignment or inducement container identifier unique within the owning object.
    cid BIGINT NOT NULL,
    -- this is different from other containers, this is not generated, app must provide it
    -- @description: Distinguishes assignment and inducement containers.
    containerType ContainerType NOT NULL CHECK (containerType IN ('ASSIGNMENT', 'INDUCEMENT')),
    -- @description: Object type of the assignment owner.
    ownerType ObjectType NOT NULL,
    -- @description: Assignment lifecycle state.
    lifecycleState TEXT,
    -- @description: Assignment order value.
    orderValue INTEGER, -- item "order"
    -- @description: OID of the organization reference target.
    orgRefTargetOid UUID,
    -- @description: Object type of the organization reference target.
    orgRefTargetType ObjectType,
    -- @description: Relation URI identifier of the organization reference.
    orgRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the assignment target reference target.
    targetRefTargetOid UUID,
    -- @description: Object type of the assignment target reference target.
    targetRefTargetType ObjectType,
    -- @description: Relation URI identifier of the assignment target reference.
    targetRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the tenant reference target.
    tenantRefTargetOid UUID,
    -- @description: Object type of the tenant reference target.
    tenantRefTargetType ObjectType,
    -- @description: Relation URI identifier of the tenant reference.
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Policy situation URI identifiers attached to the assignment.
    policySituations INTEGER[], -- soft-references m_uri, add index per table
    -- @description: Assignment subtype values used for subtype filtering.
    subtypes TEXT[], -- only EQ filter
    -- @description: Indexed extension values stored as JSON data.
    ext JSONB,
    -- construction
    -- @description: OID of the construction resource reference target.
    resourceRefTargetOid UUID,
    -- @description: Object type of the construction resource reference target.
    resourceRefTargetType ObjectType,
    -- @description: Relation URI identifier of the construction resource reference.
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    -- activation
    -- @description: Administrative activation status of the assignment.
    administrativeStatus ActivationStatusType,
    -- @description: Effective activation status of the assignment.
    effectiveStatus ActivationStatusType,
    -- @description: Time when the assignment becomes enabled.
    enableTimestamp TIMESTAMPTZ,
    -- @description: Time when the assignment becomes disabled.
    disableTimestamp TIMESTAMPTZ,
    -- @description: Reason why the assignment is disabled.
    disableReason TEXT,
    -- @description: Validity interval status of the assignment.
    validityStatus TimeIntervalStatusType,
    -- @description: Start of the assignment validity interval.
    validFrom TIMESTAMPTZ,
    -- @description: End of the assignment validity interval.
    validTo TIMESTAMPTZ,
    -- @description: Time when assignment validity state last changed.
    validityChangeTimestamp TIMESTAMPTZ,
    -- @description: Time when the assignment was archived.
    archiveTimestamp TIMESTAMPTZ,
    -- metadata
    -- @description: OID of the object that created this assignment.
    creatorRefTargetOid UUID,
    -- @description: Object type of the object that created this assignment.
    creatorRefTargetType ObjectType,
    -- @description: Relation URI identifier for the creator reference.
    creatorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that created the assignment.
    createChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the assignment was created.
    createTimestamp TIMESTAMPTZ,
    -- @description: OID of the object that last modified this assignment.
    modifierRefTargetOid UUID,
    -- @description: Object type of the object that last modified this assignment.
    modifierRefTargetType ObjectType,
    -- @description: Relation URI identifier for the modifier reference.
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that last modified the assignment.
    modifyChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the assignment was last modified.
    modifyTimestamp TIMESTAMPTZ,
    -- @description: Serialized full assignment value.
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, cid)
);

-- Assignment metadata

-- @description: Stores value metadata for assignments and inducements.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ValueMetadataType
CREATE TABLE m_assignment_metadata (
    -- @description: OID of the object that owns the assignment metadata.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Object type of the assignment metadata owner.
    ownerType ObjectType,
    -- @description: Container ID of the assignment that owns this metadata value.
    assignmentCid INTEGER NOT NULL,
    containerType ContainerType GENERATED ALWAYS AS ('ASSIGNMENT_METADATA') STORED
        CHECK (containerType = 'ASSIGNMENT_METADATA'),

    -- Storage metadata
    -- @description: OID of the object that created this metadata value.
    creatorRefTargetOid UUID,
    -- @description: Object type of the object that created this metadata value.
    creatorRefTargetType ObjectType,
    -- @description: Relation URI identifier for the creator reference.
    creatorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that created this metadata value.
    createChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when this metadata value was created.
    createTimestamp TIMESTAMPTZ,
    -- @description: OID of the object that last modified this metadata value.
    modifierRefTargetOid UUID,
    -- @description: Object type of the object that last modified this metadata value.
    modifierRefTargetType ObjectType,
    -- @description: Relation URI identifier for the modifier reference.
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: URI identifier of the channel that last modified this metadata value.
    modifyChannelId INTEGER REFERENCES m_uri(id),
    -- @description: Time when this metadata value was last modified.
    modifyTimestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, assignmentCid, cid)
) INHERITS(m_container);

-- @description: Speeds up filtering or ordering assignment metadata by creation time.
-- @usedFor: assignment metadata creation time filters and ordering
CREATE INDEX m_assignment_metadata_createTimestamp_idx ON m_assignment (createTimestamp);
-- @description: Speeds up filtering or ordering assignment metadata by modification time.
-- @usedFor: assignment metadata modification time filters and ordering
CREATE INDEX m_assignment_metadata_modifyTimestamp_idx ON m_assignment (modifyTimestamp);


-- @description: Speeds up filtering by policy situation.
-- @usedFor: policy situation filters
CREATE INDEX m_assignment_policySituation_idx
    ON m_assignment USING gin(policysituations gin__int_ops);
-- @description: Speeds up filtering by assignment subtype.
-- @usedFor: subtype filters
CREATE INDEX m_assignment_subtypes_idx ON m_assignment USING gin(subtypes);
-- @description: Speeds up filtering by indexed extension values.
-- @usedFor: extension item filters
CREATE INDEX m_assignment_ext_idx ON m_assignment USING gin(ext);
-- TODO was: CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);
-- administrativeStatus has 3 states (ENABLED/DISABLED/ARCHIVED), not sure it's worth indexing
-- but it can be used as a condition to index other (e.g. WHERE administrativeStatus='ENABLED')
-- TODO the same: CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);
-- @description: Speeds up filtering by assignment validity start time.
-- @usedFor: validity interval filters
CREATE INDEX m_assignment_validFrom_idx ON m_assignment (validFrom);
-- @description: Speeds up filtering by assignment validity end time.
-- @usedFor: validity interval filters
CREATE INDEX m_assignment_validTo_idx ON m_assignment (validTo);
-- @description: Speeds up lookup of assignments by target reference target.
-- @usedFor: assignment target reference searches
CREATE INDEX m_assignment_targetRefTargetOid_idx ON m_assignment (targetRefTargetOid);
-- @description: Speeds up lookup of assignments by tenant reference target.
-- @usedFor: assignment tenant reference searches
CREATE INDEX m_assignment_tenantRefTargetOid_idx ON m_assignment (tenantRefTargetOid);
-- @description: Speeds up lookup of assignments by organization reference target.
-- @usedFor: assignment organization reference searches
CREATE INDEX m_assignment_orgRefTargetOid_idx ON m_assignment (orgRefTargetOid);
-- @description: Speeds up lookup of assignments by construction resource reference target.
-- @usedFor: assignment construction resource searches
CREATE INDEX m_assignment_resourceRefTargetOid_idx ON m_assignment (resourceRefTargetOid);
-- @description: Speeds up filtering or ordering by creation time.
-- @usedFor: creation time filters and ordering
CREATE INDEX m_assignment_createTimestamp_idx ON m_assignment (createTimestamp);
-- @description: Speeds up filtering or ordering by modification time.
-- @usedFor: modification time filters and ordering
CREATE INDEX m_assignment_modifyTimestamp_idx ON m_assignment (modifyTimestamp);

-- stores assignment/effectiveMarkRef
-- @description: Stores effective mark references attached to assignments.
CREATE TABLE m_ref_assignment_effective_mark (
    -- @description: OID of the object that owns the assignment.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the assignment that owns the effective mark reference.
    assignmentCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_EFFECTIVE_MARK') STORED
        CHECK (referenceType = 'ASSIGNMENT_EFFECTIVE_MARK'),
    PRIMARY KEY (ownerOid, assignmentCid, relationId, targetOid)
)
    INHERITS (m_reference);

-- @description: Speeds up reverse lookup of assignment effective mark references by target object and relation.
-- @usedFor: assignment effective mark reference target searches
CREATE INDEX m_ref_assignment_effective_mark_targetOidRelationId_idx
    ON m_ref_assignment_effective_mark (targetOid, relationId);


ALTER TABLE "m_assignment_metadata"
ADD FOREIGN KEY ("owneroid", "assignmentcid") REFERENCES "m_assignment" ("owneroid", "cid") ON DELETE CASCADE;

-- stores assignment/metadata/createApproverRef
-- @description: Stores create-approver references from assignment metadata.
CREATE TABLE m_assignment_ref_create_approver (
    -- @description: OID of the object that owns the assignment.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the assignment that owns the reference.
    assignmentCid INTEGER NOT NULL,
    -- @description: Container ID of the assignment metadata value, when the reference belongs to value metadata.
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

-- @description: Speeds up reverse lookup of assignment create-approver references by target object and relation.
-- @usedFor: assignment create-approver reference target searches
CREATE INDEX m_assignment_ref_create_approver_targetOidRelationId_idx
    ON m_assignment_ref_create_approver (targetOid, relationId);

-- stores assignment/metadata/modifyApproverRef
-- @description: Stores modify-approver references from assignment metadata.
CREATE TABLE m_assignment_ref_modify_approver (
    -- @description: OID of the object that owns the assignment.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- @description: Container ID of the assignment that owns the reference.
    assignmentCid INTEGER NOT NULL,
    -- @description: Container ID of the assignment metadata value, when the reference belongs to value metadata.
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

-- @description: Speeds up reverse lookup of assignment modify-approver references by target object and relation.
-- @usedFor: assignment modify-approver reference target searches
CREATE INDEX m_assignment_ref_modify_approver_targetOidRelationId_idx
    ON m_assignment_ref_modify_approver (targetOid, relationId);
-- endregion

-- @region: tasks
-- @regionTitle: Tasks
-- @regionDescription: Task objects, task containers, affected objects, triggers, and operation execution data.
-- region Other object containers
-- stores ObjectType/trigger (TriggerType)
-- @description: Stores trigger containers attached to objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#TriggerType
CREATE TABLE m_trigger (
    -- @description: OID of the object that owns the trigger.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('TRIGGER') STORED
        CHECK (containerType = 'TRIGGER'),
    -- @description: URI identifier of the trigger handler.
    handlerUriId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the trigger should fire.
    timestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up lookup of triggers by firing time.
-- @usedFor: trigger scheduling searches
CREATE INDEX m_trigger_timestamp_idx ON m_trigger (timestamp);

-- stores ObjectType/operationExecution (OperationExecutionType)
-- @description: Stores operation execution records attached to objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#OperationExecutionType
CREATE TABLE m_operation_execution (
    -- @description: OID of the object that owns the operation execution record.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('OPERATION_EXECUTION') STORED
        CHECK (containerType = 'OPERATION_EXECUTION'),
    -- @description: Result status of the operation execution.
    status OperationResultStatusType,
    -- @description: Operation execution record type.
    recordType OperationExecutionRecordTypeType,
    -- @description: OID of the initiator reference target.
    initiatorRefTargetOid UUID,
    -- @description: Object type of the initiator reference target.
    initiatorRefTargetType ObjectType,
    -- @description: Relation URI identifier of the initiator reference.
    initiatorRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: OID of the related task reference target.
    taskRefTargetOid UUID,
    -- @description: Object type of the related task reference target.
    taskRefTargetType ObjectType,
    -- @description: Relation URI identifier of the task reference.
    taskRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Time of the operation execution.
    timestamp TIMESTAMPTZ,
    -- @description: Serialized full operation execution value.
    fullObject BYTEA,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

-- @description: Speeds up lookup of operation executions by initiator.
-- @usedFor: operation execution initiator searches
CREATE INDEX m_operation_execution_initiatorRefTargetOid_idx
    ON m_operation_execution (initiatorRefTargetOid);
-- @description: Speeds up lookup of operation executions by related task.
-- @usedFor: operation execution task searches
CREATE INDEX m_operation_execution_taskRefTargetOid_idx
    ON m_operation_execution (taskRefTargetOid);
-- @description: Speeds up filtering or ordering operation executions by time.
-- @usedFor: operation execution time filters and ordering
CREATE INDEX m_operation_execution_timestamp_idx ON m_operation_execution (timestamp);
-- index for ownerOid is part of PK
-- TODO: index for status is questionable, don't we want WHERE status = ... to another index instead?
-- endregion


-- @region: simulations
-- @regionTitle: Simulations
-- @regionDescription: Simulation result objects, processed simulation objects, and simulation event mark references.
-- region Simulations Support

-- @description: Stores simulation result objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SimulationResultType
CREATE TABLE m_simulation_result (
    -- @description: Simulation result object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SIMULATION_RESULT') STORED
        CHECK (objectType = 'SIMULATION_RESULT'),
    -- @description: Marks whether processed object data is stored in a dedicated partition.
    partitioned boolean,
    -- @description: OID of the root task reference target.
    rootTaskRefTargetOid UUID,
    -- @description: Object type of the root task reference target.
    rootTaskRefTargetType ObjectType,
    -- @description: Relation URI identifier of the root task reference.
    rootTaskRefRelationId INTEGER REFERENCES m_uri(id),
    -- @description: Time when the simulation started.
    startTimestamp TIMESTAMPTZ,
    -- @description: Time when the simulation ended.
    endTimestamp TIMESTAMPTZ
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_simulation_result`.
CREATE TRIGGER m_simulation_result_oid_insert_tr BEFORE INSERT ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_simulation_result`.
CREATE TRIGGER m_simulation_result_update_tr BEFORE UPDATE ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_simulation_result`.
CREATE TRIGGER m_simulation_result_oid_delete_tr AFTER DELETE ON m_simulation_result
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- @description: Stores processing state values for simulation processed objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ObjectProcessingStateType
CREATE TYPE ObjectProcessingStateType AS ENUM ('UNMODIFIED', 'ADDED', 'MODIFIED', 'DELETED');

-- @description: Stores objects processed by simulations and their before/after state.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#ProcessedObjectType
CREATE TABLE m_simulation_result_processed_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    -- Owner does not have to be the direct parent of the container.
    -- use like this on the concrete table:
    -- ownerOid UUID NOT NULL REFERENCES m_object_oid(oid),
    -- @description: OID of the simulation result that owns the processed object.
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    -- @description: Processed object container identifier unique within the simulation result.
    cid BIGINT NOT NULL,
    containerType ContainerType GENERATED ALWAYS AS ('SIMULATION_RESULT_PROCESSED_OBJECT') STORED
        CHECK (containerType = 'SIMULATION_RESULT_PROCESSED_OBJECT'),
    -- @description: OID of the processed object.
    oid UUID,
    -- @description: Object type of the processed object.
    objectType ObjectType,
    -- @description: Processed object name in original form.
    nameOrig TEXT,
    -- @description: Processed object name in normalized form.
    nameNorm TEXT,
    -- @description: Processing state of the object in the simulation.
    state ObjectProcessingStateType,
    -- @description: Metric identifiers associated with the processed object.
    metricIdentifiers TEXT[],
    -- @description: Serialized full processed object value.
    fullObject BYTEA,
    -- @description: Serialized object state before simulation processing.
    objectBefore BYTEA,
    -- @description: Serialized object state after simulation processing.
    objectAfter BYTEA,
    -- @description: Transaction identifier associated with the processed object.
    transactionId TEXT,
    -- @description: Identifier linking focus-related processed object records.
    focusRecordId BIGINT,

   PRIMARY KEY (ownerOid, cid)
) PARTITION BY LIST(ownerOid);

-- @description: Default partition for simulation processed objects.
CREATE TABLE m_simulation_result_processed_object_default PARTITION OF m_simulation_result_processed_object DEFAULT;

-- @description: Creates a processed-object partition for a simulation result when partitioning is enabled.
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

-- @description: Creates a processed-object partition after inserting a partitioned simulation result.
CREATE TRIGGER m_simulation_result_create_partition AFTER INSERT ON m_simulation_result
 FOR EACH ROW EXECUTE FUNCTION m_simulation_result_create_partition();

--- Trigger which deletes processed objects partition when whole simulation is deleted

-- @description: Drops the processed-object partition when a partitioned simulation result is deleted.
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

-- @description: Drops a processed-object partition before deleting a partitioned simulation result.
CREATE TRIGGER m_simulation_result_delete_partition BEFORE DELETE ON m_simulation_result
  FOR EACH ROW EXECUTE FUNCTION m_simulation_result_delete_partition();



-- @description: Stores event mark references for processed simulation objects.
CREATE TABLE m_processed_object_event_mark (
  -- @description: OID of the simulation result that owns the processed object.
  ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
  -- @description: Object type of the owner.
  ownerType ObjectType, -- GENERATED ALWAYS AS ('SIMULATION_RESULT') STORED,
  -- @description: Container ID of the processed object.
  processedObjectCid INTEGER NOT NULL,
  referenceType ReferenceType GENERATED ALWAYS AS ('PROCESSED_OBJECT_EVENT_MARK') STORED,
  -- @description: OID of the referenced event mark object.
  targetOid UUID NOT NULL, -- soft-references m_object
  -- @description: Object type of the referenced event mark object.
  targetType ObjectType NOT NULL,
  -- @description: Relation URI identifier of the event mark reference.
  relationId INTEGER NOT NULL REFERENCES m_uri(id)

) PARTITION BY LIST(ownerOid);

-- @description: Default partition for processed object event mark references.
CREATE TABLE m_processed_object_event_mark_default PARTITION OF m_processed_object_event_mark DEFAULT;

-- endregion

-- @region: marks
-- @regionTitle: Marks
-- @regionDescription: Mark objects used to classify and annotate repository objects and events.
-- region Mark

-- @description: Stores mark objects used to classify and annotate repository objects and events.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#MarkType
CREATE TABLE m_mark (
    -- @description: Mark object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('MARK') STORED
        CHECK (objectType = 'MARK')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_mark`.
CREATE TRIGGER m_mark_oid_insert_tr BEFORE INSERT ON m_mark
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_mark`.
CREATE TRIGGER m_mark_update_tr BEFORE UPDATE ON m_mark
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_mark`.
CREATE TRIGGER m_mark_oid_delete_tr AFTER DELETE ON m_mark
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();


-- endregion

-- @region: schema-objects
-- @regionTitle: Schema objects
-- @regionDescription: Schema repository objects.
-- region schema
-- @description: Stores schema objects.
-- @type: http://midpoint.evolveum.com/xml/ns/public/common/common-3#SchemaType
CREATE TABLE m_schema (
    -- @description: Schema object identifier.
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SCHEMA') STORED
       CHECK (objectType = 'SCHEMA')
)
    INHERITS (m_assignment_holder);

-- @description: Maintains the object OID registry when rows are inserted into `m_schema`.
CREATE TRIGGER m_schema_oid_insert_tr BEFORE INSERT ON m_schema
    FOR EACH ROW EXECUTE FUNCTION insert_object_oid();
-- @description: Maintains object update metadata before rows are updated in `m_schema`.
CREATE TRIGGER m_schema_update_tr BEFORE UPDATE ON m_schema
    FOR EACH ROW EXECUTE FUNCTION before_update_object();
-- @description: Removes the object OID registry entry when rows are deleted from `m_schema`.
CREATE TRIGGER m_schema_oid_delete_tr AFTER DELETE ON m_schema
    FOR EACH ROW EXECUTE FUNCTION delete_object_oid();

-- endregion

-- @region: extension-items
-- @regionTitle: Extension items
-- @regionDescription: Extension item catalog and indexed extension value storage support.
-- region Extension support
-- Catalog table of known indexed extension items.
-- While itemName and valueType are both Q-names they are not cached via m_uri because this
-- table is small, itemName does not repeat (valueType does) and readability is also better.
-- This has similar function as m_uri - it translates something to IDs, no need to nest it.
-- @description: Catalog of extension and attribute items indexed in JSON extension storage.
CREATE TABLE m_ext_item (
    -- @description: Numeric extension item identifier used as key in JSON extension storage.
    id SERIAL NOT NULL PRIMARY KEY,
    -- @description: Extension or attribute item QName.
    itemName TEXT NOT NULL,
    -- @description: Value type QName.
    valueType TEXT NOT NULL,
    -- @description: Holder category for the indexed item.
    holderType ExtItemHolderType NOT NULL,
    -- @description: Cardinality of indexed item values.
    cardinality ExtItemCardinality NOT NULL
    -- information about storage mechanism (JSON common/separate, column, table separate/common, etc.)
    -- storageType JSONB NOT NULL default '{"type": "EXT_JSON"}', -- currently only JSONB is used
);

-- This works fine for itemName+holderType search used in raw processing
-- @description: Enforces unique extension item definitions and speeds up lookup during raw extension processing.
-- @usedFor: extension item catalog lookup and uniqueness checks
CREATE UNIQUE INDEX m_ext_item_key ON m_ext_item (itemName, holderType, valueType, cardinality);
-- endregion

-- INDEXING:
-- More indexes is possible, but for low-variability columns like lifecycleState or administrative/effectiveStatus
-- better use them in WHERE as needed when slow query appear: https://www.postgresql.org/docs/current/indexes-partial.html
-- Also see: https://docs.evolveum.com/midpoint/reference/repository/native-postgresql/db-maintenance/

-- @region: schema-versioning
-- @regionTitle: Schema versioning
-- @regionDescription: Procedures and metadata used to track and apply native repository schema changes.
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
-- @description: Applies numbered main repository schema changes exactly once per database instance.
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
call apply_change(58, $$ SELECT 1 $$, true);
