-- Copyright (C) 2010-2021 Evolveum and contributors
--
-- This work is dual-licensed under the Apache License 2.0
-- and European Union Public License. See LICENSE file for details.
--
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

-- noinspection SqlResolveForFile @ operator-class/"gin__int_ops"

-- just in case PUBLIC schema was dropped (fastest way to remove all midpoint objects)
-- drop schema public cascade;
CREATE SCHEMA IF NOT EXISTS public;
CREATE EXTENSION IF NOT EXISTS intarray; -- support for indexing INTEGER[] columns
--CREATE EXTENSION IF NOT EXISTS pg_trgm; -- support for trigram indexes TODO for ext with LIKE and fulltext

-- region custom enum types
-- Some enums are from schema, some are only defined in repo-sqale.
-- All Java enum types must be registered in SqaleRepoContext constructor.

-- First purely repo-sqale enums (these have M prefix in Java, the rest of the name is the same):
CREATE TYPE ContainerType AS ENUM (
    'ACCESS_CERTIFICATION_CASE',
    'ACCESS_CERTIFICATION_WORK_ITEM',
    'ASSIGNMENT',
    'CASE_WORK_ITEM',
    'INDUCEMENT',
    'LOOKUP_TABLE_ROW',
    'OPERATION_EXECUTION',
    'TRIGGER');

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

CREATE TYPE ReferenceType AS ENUM (
    'ARCHETYPE',
    'ASSIGNMENT_CREATE_APPROVER',
    'ASSIGNMENT_MODIFY_APPROVER',
    'ACCESS_CERT_WI_ASSIGNEE',
    'ACCESS_CERT_WI_CANDIDATE',
    'CASE_WI_ASSIGNEE',
    'CASE_WI_CANDIDATE',
    'DELEGATED',
    'INCLUDE',
    'PROJECTION',
    'OBJECT_CREATE_APPROVER',
    'OBJECT_MODIFY_APPROVER',
    'OBJECT_PARENT_ORG',
    'PERSONA',
    'RESOURCE_BUSINESS_CONFIGURATION_APPROVER',
    'ROLE_MEMBERSHIP');

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

CREATE TYPE AvailabilityStatusType AS ENUM ('DOWN', 'UP', 'BROKEN');

CREATE TYPE LockoutStatusType AS ENUM ('NORMAL', 'LOCKED');

CREATE TYPE OperationExecutionRecordTypeType AS ENUM ('SIMPLE', 'COMPLEX');

CREATE TYPE OperationResultStatusType AS ENUM ('SUCCESS', 'WARNING', 'PARTIAL_ERROR',
    'FATAL_ERROR', 'HANDLED_ERROR', 'NOT_APPLICABLE', 'IN_PROGRESS', 'UNKNOWN');

CREATE TYPE OrientationType AS ENUM ('PORTRAIT', 'LANDSCAPE');

CREATE TYPE ResourceAdministrativeStateType AS ENUM ('ENABLED', 'DISABLED');

CREATE TYPE ShadowKindType AS ENUM ('ACCOUNT', 'ENTITLEMENT', 'GENERIC', 'UNKNOWN');

CREATE TYPE SynchronizationSituationType AS ENUM (
    'DELETED', 'DISPUTED', 'LINKED', 'UNLINKED', 'UNMATCHED');

CREATE TYPE TaskBindingType AS ENUM ('LOOSE', 'TIGHT');

CREATE TYPE TaskExecutionStateType AS ENUM ('RUNNING', 'RUNNABLE', 'WAITING', 'SUSPENDED', 'CLOSED');

CREATE TYPE TaskRecurrenceType AS ENUM ('SINGLE', 'RECURRING');

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
    objectType ObjectType NOT NULL CHECK (objectType = 'OBJECT') NO INHERIT,
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
    textInfo TEXT[], -- TODO not mapped yet, see RObjectTextInfo#createItemsSet, this may not be []
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
    containerType ContainerType NOT NULL,

    CHECK (FALSE) NO INHERIT
    -- add on concrete table (additional columns possible): PRIMARY KEY (ownerOid, cid)
);

-- Abstract reference table, for object but also other container references.
CREATE TABLE m_reference (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- referenceType will be overridden with GENERATED value in concrete table
    ownerType ObjectType NOT NULL,
    referenceType ReferenceType NOT NULL,
    targetOid UUID NOT NULL, -- soft-references m_object
    targetType ObjectType NOT NULL,
    relationId INTEGER NOT NULL REFERENCES m_uri(id),

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
    -- add PK (referenceType is the same per table): PRIMARY KEY (ownerOid, relationId, targetOid)
);
-- Add this index for each sub-table (reference type is not necessary, each sub-table has just one).
-- CREATE INDEX m_referenceTargetOidRelationId_idx ON m_reference (targetOid, relationId);

-- references related to ObjectType and AssignmentHolderType
-- stores AssignmentHolderType/archetypeRef
CREATE TABLE m_ref_archetype (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (referenceType = 'ARCHETYPE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_archetypeTargetOidRelationId_idx
    ON m_ref_archetype (targetOid, relationId);

-- stores AssignmentHolderType/delegatedRef
CREATE TABLE m_ref_delegated (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('DELEGATED') STORED
        CHECK (referenceType = 'DELEGATED'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_delegatedTargetOidRelationId_idx
    ON m_ref_delegated (targetOid, relationId);

-- stores ObjectType/metadata/createApproverRef
CREATE TABLE m_ref_object_create_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_CREATE_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_CREATE_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_create_approverTargetOidRelationId_idx
    ON m_ref_object_create_approver (targetOid, relationId);

-- stores ObjectType/metadata/modifyApproverRef
CREATE TABLE m_ref_object_modify_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_MODIFY_APPROVER') STORED
        CHECK (referenceType = 'OBJECT_MODIFY_APPROVER'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_modify_approverTargetOidRelationId_idx
    ON m_ref_object_modify_approver (targetOid, relationId);

-- stores AssignmentHolderType/roleMembershipRef
CREATE TABLE m_ref_role_membership (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ROLE_MEMBERSHIP') STORED
        CHECK (referenceType = 'ROLE_MEMBERSHIP'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_role_memberTargetOidRelationId_idx
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

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_assignment_holder);

-- stores FocusType/personaRef
CREATE TABLE m_ref_persona (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PERSONA') STORED
        CHECK (referenceType = 'PERSONA'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_personaTargetOidRelationId_idx
    ON m_ref_persona (targetOid, relationId);

-- stores FocusType/linkRef ("projection" is newer and better term)
CREATE TABLE m_ref_projection (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PROJECTION') STORED
        CHECK (referenceType = 'PROJECTION'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_projectionTargetOidRelationId_idx
    ON m_ref_projection (targetOid, relationId);

-- Represents GenericObjectType, see https://docs.evolveum.com/midpoint/reference/schema/generic-objects/
CREATE TABLE m_generic_object (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('GENERIC_OBJECT') STORED
        CHECK (objectType = 'GENERIC_OBJECT'),
    genericObjectTypeId INTEGER NOT NULL REFERENCES m_uri(id) -- GenericObjectType#objectType
)
    INHERITS (m_focus);

CREATE TRIGGER m_generic_object_oid_insert_tr BEFORE INSERT ON m_generic_object
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_generic_object_update_tr BEFORE UPDATE ON m_generic_object
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_generic_object_oid_delete_tr AFTER DELETE ON m_generic_object
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- TODO unique per genericObjectTypeId?
--  No indexes for GenericObjectType#objectType were in old repo, what queries are expected?
CREATE INDEX m_generic_object_nameOrig_idx ON m_generic_object (nameOrig);
ALTER TABLE m_generic_object ADD CONSTRAINT m_generic_object_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_generic_object_subtypes_idx ON m_generic_object USING gin(subtypes);
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
    titleOrig TEXT,
    titleNorm TEXT,
    organizations JSONB, -- array of {o,n} objects
    organizationUnits JSONB -- array of {o,n} objects
)
    INHERITS (m_focus);

CREATE TRIGGER m_user_oid_insert_tr BEFORE INSERT ON m_user
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_user_update_tr BEFORE UPDATE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_user_oid_delete_tr AFTER DELETE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_user_nameOrig_idx ON m_user (nameOrig);
ALTER TABLE m_user ADD CONSTRAINT m_user_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_user_policySituation_idx ON m_user USING gin(policysituations gin__int_ops);
CREATE INDEX m_user_ext_idx ON m_user USING gin(ext);
CREATE INDEX m_user_fullNameOrig_idx ON m_user (fullNameOrig);
CREATE INDEX m_user_familyNameOrig_idx ON m_user (familyNameOrig);
CREATE INDEX m_user_givenNameOrig_idx ON m_user (givenNameOrig);
CREATE INDEX m_user_employeeNumber_idx ON m_user (employeeNumber);
CREATE INDEX m_user_subtypes_idx ON m_user USING gin(subtypes);
CREATE INDEX m_user_organizations_idx ON m_user USING gin(organizations);
CREATE INDEX m_user_organizationUnits_idx ON m_user USING gin(organizationUnits);
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

/* TODO: add for sub-tables, role, org... all? how many services?
CREATE INDEX iAbstractRoleIdentifier ON m_abstract_role (identifier);
CREATE INDEX iRequestable ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled ON m_abstract_role(autoassign_enabled);
*/

-- Represents RoleType, see https://docs.evolveum.com/midpoint/architecture/archive/data-model/midpoint-common-schema/roletype/
CREATE TABLE m_role (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE') STORED
        CHECK (objectType = 'ROLE'),
    roleType TEXT
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_role_oid_insert_tr BEFORE INSERT ON m_role
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_role_update_tr BEFORE UPDATE ON m_role
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_role_oid_delete_tr AFTER DELETE ON m_role
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_role_nameOrig_idx ON m_role (nameOrig);
ALTER TABLE m_role ADD CONSTRAINT m_role_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_role_subtypes_idx ON m_role USING gin(subtypes);

-- Represents ServiceType, see https://wiki.evolveum.com/display/midPoint/Service+Account+Management
CREATE TABLE m_service (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SERVICE') STORED
        CHECK (objectType = 'SERVICE'),
    displayOrder INTEGER
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_service_oid_insert_tr BEFORE INSERT ON m_service
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_service_update_tr BEFORE UPDATE ON m_service
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_service_oid_delete_tr AFTER DELETE ON m_service
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_service_nameOrig_idx ON m_service (nameOrig);
ALTER TABLE m_service ADD CONSTRAINT m_service_nameNorm_key UNIQUE (nameNorm);

-- Represents ArchetypeType, see https://wiki.evolveum.com/display/midPoint/Archetypes
CREATE TABLE m_archetype (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ARCHETYPE') STORED
        CHECK (objectType = 'ARCHETYPE')
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_archetype_oid_insert_tr BEFORE INSERT ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_archetype_update_tr BEFORE UPDATE ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_archetype_oid_delete_tr AFTER DELETE ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_archetype_nameOrig_idx ON m_archetype (nameOrig);
ALTER TABLE m_archetype ADD CONSTRAINT m_archetype_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_archetype_subtypes_idx ON m_archetype USING gin(subtypes);
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
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_org_update_tr BEFORE UPDATE ON m_org
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_org_oid_delete_tr AFTER DELETE ON m_org
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_org_nameOrig_idx ON m_org (nameOrig);
ALTER TABLE m_org ADD CONSTRAINT m_org_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_org_displayOrder_idx ON m_org (displayOrder);
CREATE INDEX m_org_subtypes_idx ON m_org USING gin(subtypes);

-- stores ObjectType/parentOrgRef
CREATE TABLE m_ref_object_parent_org (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_PARENT_ORG') STORED
        CHECK (referenceType = 'OBJECT_PARENT_ORG'),

    -- TODO wouldn't (ownerOid, targetOid, relationId) perform better for typical queries?
    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

-- TODO is this enough? Is target+owner+relation needed too?
CREATE INDEX m_ref_object_parent_orgTargetOidRelationId_idx
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
    FOR EACH ROW EXECUTE PROCEDURE mark_org_closure_for_refresh();
CREATE TRIGGER m_ref_object_parent_mark_refresh_trunc_tr
    AFTER TRUNCATE ON m_ref_object_parent_org
    FOR EACH STATEMENT EXECUTE PROCEDURE mark_org_closure_for_refresh();

-- This procedure for conditional refresh when needed is called from the application code.
-- The refresh can be forced, e.g. after many changes with triggers off (or just to be sure).
CREATE OR REPLACE PROCEDURE m_refresh_org_closure(force boolean = false)
    LANGUAGE plpgsql
AS $$
DECLARE
    flag_val text;
BEGIN
    SELECT value INTO flag_val FROM m_global_metadata WHERE name = 'orgClosureRefreshNeeded';
    IF flag_val = 'true' OR force THEN
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
    END IF;
END; $$;
-- endregion
-- endregion

-- region OTHER object tables
-- Represents ResourceType, see https://wiki.evolveum.com/display/midPoint/Resource+Configuration
CREATE TABLE m_resource (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('RESOURCE') STORED
        CHECK (objectType = 'RESOURCE'),
    business_administrativeState ResourceAdministrativeStateType,
    operationalState_lastAvailabilityStatus AvailabilityStatusType,
    connectorRefTargetOid UUID,
    connectorRefTargetType ObjectType,
    connectorRefRelationId INTEGER REFERENCES m_uri(id)
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_resource_oid_insert_tr BEFORE INSERT ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_resource_update_tr BEFORE UPDATE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_resource_oid_delete_tr AFTER DELETE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_resource_nameOrig_idx ON m_resource (nameOrig);
ALTER TABLE m_resource ADD CONSTRAINT m_resource_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_resource_subtypes_idx ON m_resource USING gin(subtypes);

-- stores ResourceType/business/approverRef
CREATE TABLE m_ref_resource_business_configuration_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS
        ('RESOURCE_BUSINESS_CONFIGURATION_APPROVER') STORED,

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_resource_biz_config_approverTargetOidRelationId_idx
    ON m_ref_resource_business_configuration_approver (targetOid, relationId);

-- Represents ShadowType, see https://wiki.evolveum.com/display/midPoint/Shadow+Objects
-- and also https://docs.evolveum.com/midpoint/reference/schema/focus-and-projections/
CREATE TABLE m_shadow (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SHADOW') STORED
        CHECK (objectType = 'SHADOW'),
    objectClassId INTEGER REFERENCES m_uri(id),
    resourceRefTargetOid UUID,
    resourceRefTargetType ObjectType,
    resourceRefRelationId INTEGER REFERENCES m_uri(id),
    intent TEXT,
    tag TEXT,
    kind ShadowKindType,
    attemptNumber INTEGER, -- TODO how is this mapped?
    dead BOOLEAN,
    exist BOOLEAN,
    fullSynchronizationTimestamp TIMESTAMPTZ,
    pendingOperationCount INTEGER NOT NULL,
    primaryIdentifierValue TEXT,
--     status INTEGER, TODO how is this mapped? See RUtil.copyResultFromJAXB called from RTask and OperationResultMapper
    synchronizationSituation SynchronizationSituationType,
    synchronizationTimestamp TIMESTAMPTZ,
    attributes JSONB
)
    INHERITS (m_object);

CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_shadow_update_tr BEFORE UPDATE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_shadow_nameOrig_idx ON m_shadow (nameOrig);
CREATE INDEX m_shadow_nameNorm_idx ON m_shadow (nameNorm); -- may not be unique for shadows!
ALTER TABLE m_shadow ADD CONSTRAINT m_shadow_primIdVal_objCls_resRefOid_key
    UNIQUE (primaryIdentifierValue, objectClassId, resourceRefTargetOid);

CREATE INDEX m_shadow_subtypes_idx ON m_shadow USING gin(subtypes);
CREATE INDEX m_shadow_policySituation_idx ON m_shadow USING gin(policysituations gin__int_ops);
CREATE INDEX m_shadow_ext_idx ON m_shadow USING gin(ext);
CREATE INDEX m_shadow_attributes_idx ON m_shadow USING gin(attributes);
/*
TODO: reconsider, especially boolean things like dead (perhaps WHERE in other indexes?)
CREATE INDEX iShadowResourceRef ON m_shadow (resourceRefTargetOid);
CREATE INDEX iShadowDead ON m_shadow (dead);
CREATE INDEX iShadowKind ON m_shadow (kind);
CREATE INDEX iShadowIntent ON m_shadow (intent);
CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);
CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);
CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);
CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);
*/

-- Represents NodeType, see https://wiki.evolveum.com/display/midPoint/Managing+cluster+nodes
CREATE TABLE m_node (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('NODE') STORED
        CHECK (objectType = 'NODE'),
    nodeIdentifier TEXT
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_node_oid_insert_tr BEFORE INSERT ON m_node
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_node_update_tr BEFORE UPDATE ON m_node
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_node_oid_delete_tr AFTER DELETE ON m_node
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_node_nameOrig_idx ON m_node (nameOrig);
ALTER TABLE m_node ADD CONSTRAINT m_node_nameNorm_key UNIQUE (nameNorm);
-- not interested in other indexes for this one, this table will be small

-- Represents SystemConfigurationType, see https://wiki.evolveum.com/display/midPoint/System+Configuration+Object
CREATE TABLE m_system_configuration (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SYSTEM_CONFIGURATION') STORED
        CHECK (objectType = 'SYSTEM_CONFIGURATION')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_system_configuration_oid_insert_tr BEFORE INSERT ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_system_configuration_update_tr BEFORE UPDATE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_system_configuration_oid_delete_tr AFTER DELETE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

ALTER TABLE m_system_configuration
    ADD CONSTRAINT m_system_configuration_nameNorm_key UNIQUE (nameNorm);
-- no need for the name index, m_system_configuration table is very small

-- Represents SecurityPolicyType, see https://wiki.evolveum.com/display/midPoint/Security+Policy+Configuration
CREATE TABLE m_security_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SECURITY_POLICY') STORED
        CHECK (objectType = 'SECURITY_POLICY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_security_policy_oid_insert_tr BEFORE INSERT ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_security_policy_update_tr BEFORE UPDATE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_security_policy_oid_delete_tr AFTER DELETE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_security_policy_nameOrig_idx ON m_security_policy (nameOrig);
ALTER TABLE m_security_policy ADD CONSTRAINT m_security_policy_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_security_policy_subtypes_idx ON m_security_policy USING gin(subtypes);
CREATE INDEX m_security_policy_policySituation_idx
    ON m_security_policy USING gin(policysituations gin__int_ops);

-- Represents ObjectCollectionType, see https://wiki.evolveum.com/display/midPoint/Object+Collections+and+Views+Configuration
CREATE TABLE m_object_collection (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_COLLECTION') STORED
        CHECK (objectType = 'OBJECT_COLLECTION')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_object_collection_oid_insert_tr BEFORE INSERT ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_object_collection_update_tr BEFORE UPDATE ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_object_collection_oid_delete_tr AFTER DELETE ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_object_collection_nameOrig_idx ON m_object_collection (nameOrig);
ALTER TABLE m_object_collection ADD CONSTRAINT m_object_collection_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_object_collection_subtypes_idx ON m_object_collection USING gin(subtypes);
CREATE INDEX m_object_collection_policySituation_idx
    ON m_object_collection USING gin(policysituations gin__int_ops);

-- Represents DashboardType, see https://wiki.evolveum.com/display/midPoint/Dashboard+configuration
CREATE TABLE m_dashboard (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('DASHBOARD') STORED
        CHECK (objectType = 'DASHBOARD')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_dashboard_oid_insert_tr BEFORE INSERT ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_dashboard_update_tr BEFORE UPDATE ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_dashboard_oid_delete_tr AFTER DELETE ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_dashboard_nameOrig_idx ON m_dashboard (nameOrig);
ALTER TABLE m_dashboard ADD CONSTRAINT m_dashboard_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_dashboard_subtypes_idx ON m_dashboard USING gin(subtypes);
CREATE INDEX m_dashboard_policySituation_idx
    ON m_dashboard USING gin(policysituations gin__int_ops);

-- Represents ValuePolicyType
CREATE TABLE m_value_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('VALUE_POLICY') STORED
        CHECK (objectType = 'VALUE_POLICY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_value_policy_oid_insert_tr BEFORE INSERT ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_value_policy_update_tr BEFORE UPDATE ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_value_policy_oid_delete_tr AFTER DELETE ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_value_policy_nameOrig_idx ON m_value_policy (nameOrig);
ALTER TABLE m_value_policy ADD CONSTRAINT m_value_policy_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_value_policy_subtypes_idx ON m_value_policy USING gin(subtypes);
CREATE INDEX m_value_policy_policySituation_idx
    ON m_value_policy USING gin(policysituations gin__int_ops);

-- Represents ReportType, see https://wiki.evolveum.com/display/midPoint/Report+Configuration
CREATE TABLE m_report (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT') STORED
        CHECK (objectType = 'REPORT')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_report_oid_insert_tr BEFORE INSERT ON m_report
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_report_update_tr BEFORE UPDATE ON m_report
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_report_oid_delete_tr AFTER DELETE ON m_report
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_report_nameOrig_idx ON m_report (nameOrig);
ALTER TABLE m_report ADD CONSTRAINT m_report_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_report_subtypes_idx ON m_report USING gin(subtypes);
CREATE INDEX m_report_policySituation_idx ON m_report USING gin(policysituations gin__int_ops);

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
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_report_data_update_tr BEFORE UPDATE ON m_report_data
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_report_data_oid_delete_tr AFTER DELETE ON m_report_data
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_report_data_nameOrig_idx ON m_report_data (nameOrig);
ALTER TABLE m_report_data ADD CONSTRAINT m_report_data_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_report_data_subtypes_idx ON m_report_data USING gin(subtypes);
CREATE INDEX m_report_data_policySituation_idx
    ON m_report_data USING gin(policysituations gin__int_ops);

-- Represents LookupTableType, see https://wiki.evolveum.com/display/midPoint/Lookup+Tables
CREATE TABLE m_lookup_table (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('LOOKUP_TABLE') STORED
        CHECK (objectType = 'LOOKUP_TABLE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_lookup_table_oid_insert_tr BEFORE INSERT ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_lookup_table_update_tr BEFORE UPDATE ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_lookup_table_oid_delete_tr AFTER DELETE ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_lookup_table_nameOrig_idx ON m_lookup_table (nameOrig);
ALTER TABLE m_lookup_table ADD CONSTRAINT m_lookup_table_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_lookup_table_subtypes_idx ON m_lookup_table USING gin(subtypes);
CREATE INDEX m_lookup_table_policySituation_idx
    ON m_lookup_table USING gin(policysituations gin__int_ops);

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

ALTER TABLE m_lookup_table_row
    ADD CONSTRAINT m_lookup_table_row_ownerOid_key_key UNIQUE (ownerOid, key);

-- Represents ConnectorType, see https://wiki.evolveum.com/display/midPoint/Identity+Connectors
CREATE TABLE m_connector (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR') STORED
        CHECK (objectType = 'CONNECTOR'),
    connectorBundle TEXT, -- typically a package name
    connectorType TEXT, -- typically a class name
    connectorVersion TEXT,
    frameworkId INTEGER REFERENCES m_uri(id),
    connectorHostRefTargetOid UUID,
    connectorHostRefTargetType ObjectType,
    connectorHostRefRelationId INTEGER REFERENCES m_uri(id),
    targetSystemTypes INTEGER[]
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_connector_oid_insert_tr BEFORE INSERT ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_connector_update_tr BEFORE UPDATE ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_connector_oid_delete_tr AFTER DELETE ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_connector_nameOrig_idx ON m_connector (nameOrig);
ALTER TABLE m_connector ADD CONSTRAINT m_connector_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_connector_subtypes_idx ON m_connector USING gin(subtypes);
CREATE INDEX m_connector_policySituation_idx
    ON m_connector USING gin(policysituations gin__int_ops);

-- Represents ConnectorHostType, see https://wiki.evolveum.com/display/midPoint/Connector+Server
CREATE TABLE m_connector_host (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_HOST') STORED
        CHECK (objectType = 'CONNECTOR_HOST'),
    hostname TEXT,
    port TEXT
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_connector_host_oid_insert_tr BEFORE INSERT ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_connector_host_update_tr BEFORE UPDATE ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_connector_host_oid_delete_tr AFTER DELETE ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_connector_host_nameOrig_idx ON m_connector_host (nameOrig);
ALTER TABLE m_connector_host ADD CONSTRAINT m_connector_host_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_connector_host_subtypes_idx ON m_connector_host USING gin(subtypes);
CREATE INDEX m_connector_host_policySituation_idx
    ON m_connector_host USING gin(policysituations gin__int_ops);

-- Represents persistent TaskType, see https://wiki.evolveum.com/display/midPoint/Task+Manager
CREATE TABLE m_task (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('TASK') STORED
        CHECK (objectType = 'TASK'),
    taskIdentifier TEXT,
    binding TaskBindingType,
    category TEXT, -- TODO revise, deprecated, probably can go away soon
    completionTimestamp TIMESTAMPTZ,
    executionStatus TaskExecutionStateType,
    fullResult BYTEA,
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
    resultStatus OperationResultStatusType,
    threadStopAction ThreadStopActionType,
    waitingReason TaskWaitingReasonType,
    dependentTaskIdentifiers TEXT[] -- contains values of taskIdentifier
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_task_oid_insert_tr BEFORE INSERT ON m_task
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_task_update_tr BEFORE UPDATE ON m_task
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_task_oid_delete_tr AFTER DELETE ON m_task
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_task_nameOrig_idx ON m_task (nameOrig);
ALTER TABLE m_task ADD CONSTRAINT m_task_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_task_parent_idx ON m_task (parent);
CREATE INDEX m_task_objectRefTargetOid_idx ON m_task(objectRefTargetOid);
ALTER TABLE m_task ADD CONSTRAINT m_task_taskIdentifier_key UNIQUE (taskIdentifier);
CREATE INDEX m_task_dependentTaskIdentifiers_idx ON m_task USING gin(dependentTaskIdentifiers);
CREATE INDEX m_task_subtypes_idx ON m_task USING gin(subtypes);
CREATE INDEX m_task_policySituation_idx ON m_task USING gin(policysituations gin__int_ops);
-- endregion

-- region cases
-- Represents CaseType, see https://wiki.evolveum.com/display/midPoint/Case+Management
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
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_case_update_tr BEFORE UPDATE ON m_case
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_case_oid_delete_tr AFTER DELETE ON m_case
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_case_nameOrig_idx ON m_case (nameOrig);
ALTER TABLE m_case ADD CONSTRAINT m_case_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_case_subtypes_idx ON m_case USING gin(subtypes);
CREATE INDEX m_case_policySituation_idx ON m_case USING gin(policysituations gin__int_ops);

CREATE INDEX m_case_objectRefTargetOid_idx ON m_case(objectRefTargetOid);
CREATE INDEX m_case_targetRefTargetOid_idx ON m_case(targetRefTargetOid);
CREATE INDEX m_case_parentRefTargetOid_idx ON m_case(parentRefTargetOid);
CREATE INDEX m_case_requestorRefTargetOid_idx ON m_case(requestorRefTargetOid);
CREATE INDEX m_case_closeTimestamp_idx ON m_case(closeTimestamp);

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

-- TODO INDEXES, old repo had no indexes either

-- stores workItem/assigneeRef
CREATE TABLE m_case_wi_assignee (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_ASSIGNEE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_case_wi_assignee ADD CONSTRAINT m_case_wi_assignee_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid);

-- stores workItem/candidateRef
CREATE TABLE m_case_wi_candidate (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    workItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('CASE_WI_CANDIDATE') STORED,

    PRIMARY KEY (ownerOid, workItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_case_wi_candidate ADD CONSTRAINT m_case_wi_candidate_id_fk
    FOREIGN KEY (ownerOid, workItemCid) REFERENCES m_case_wi (ownerOid, cid);
-- endregion

-- region Access Certification object tables
-- Represents AccessCertificationDefinitionType, see https://wiki.evolveum.com/display/midPoint/Access+Certification
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
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_access_cert_definition_update_tr BEFORE UPDATE ON m_access_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_access_cert_definition_oid_delete_tr AFTER DELETE ON m_access_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_access_cert_definition_nameOrig_idx ON m_access_cert_definition (nameOrig);
ALTER TABLE m_access_cert_definition
    ADD CONSTRAINT m_access_cert_definition_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_access_cert_definition_subtypes_idx ON m_access_cert_definition USING gin(subtypes);
CREATE INDEX m_access_cert_definition_policySituation_idx
    ON m_access_cert_definition USING gin(policysituations gin__int_ops);
CREATE INDEX m_access_cert_definition_ext_idx ON m_access_cert_definition USING gin(ext);

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
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_access_cert_campaign_update_tr BEFORE UPDATE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_access_cert_campaign_oid_delete_tr AFTER DELETE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_access_cert_campaign_nameOrig_idx ON m_access_cert_campaign (nameOrig);
ALTER TABLE m_access_cert_campaign
    ADD CONSTRAINT m_access_cert_campaign_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_access_cert_campaign_subtypes_idx ON m_access_cert_campaign USING gin(subtypes);
CREATE INDEX m_access_cert_campaign_policySituation_idx
    ON m_access_cert_campaign USING gin(policysituations gin__int_ops);
CREATE INDEX m_access_cert_campaign_ext_idx ON m_access_cert_campaign USING gin(ext);

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

ALTER TABLE m_access_cert_wi_assignee ADD CONSTRAINT m_access_cert_wi_assignee_id_fk_case
    FOREIGN KEY (ownerOid, accessCertCaseCid) REFERENCES m_access_cert_case (ownerOid, cid);

ALTER TABLE m_access_cert_wi_assignee ADD CONSTRAINT m_access_cert_wi_assignee_id_fk_wi
    FOREIGN KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid)
        REFERENCES m_access_cert_wi (ownerOid, accessCertCaseCid, cid)
        ON DELETE CASCADE; -- TODO is the cascade needed?

-- stores case/workItem/candidateRef
CREATE TABLE m_access_cert_wi_candidate (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    accessCertCaseCid INTEGER NOT NULL,
    accessCertWorkItemCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ACCESS_CERT_WI_CANDIDATE') STORED,

    PRIMARY KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_access_cert_wi_candidate ADD CONSTRAINT m_access_cert_wi_candidate_id_fk_case
    FOREIGN KEY (ownerOid, accessCertCaseCid) REFERENCES m_access_cert_case (ownerOid, cid);

ALTER TABLE m_access_cert_wi_candidate ADD CONSTRAINT m_access_cert_wi_candidate_id_fk_wi
    FOREIGN KEY (ownerOid, accessCertCaseCid, accessCertWorkItemCid)
        REFERENCES m_access_cert_wi (ownerOid, accessCertCaseCid, cid)
        ON DELETE CASCADE; -- TODO is the cascade needed?

/*
CREATE INDEX iCertCampaignNameOrig ON m_access_cert_campaign (nameOrig);
ALTER TABLE m_access_cert_campaign ADD CONSTRAINT uc_access_cert_campaign_name UNIQUE (nameNorm);
CREATE INDEX iCaseObjectRefTargetOid ON m_access_cert_case (objectRefTargetOid);
CREATE INDEX iCaseTargetRefTargetOid ON m_access_cert_case (targetRefTargetOid);
CREATE INDEX iCaseTenantRefTargetOid ON m_access_cert_case (tenantRefTargetOid);
CREATE INDEX iCaseOrgRefTargetOid ON m_access_cert_case (orgRefTargetOid);
CREATE INDEX iCertDefinitionNameOrig ON m_access_cert_definition (nameOrig);
ALTER TABLE m_access_cert_definition ADD CONSTRAINT uc_access_cert_definition_name UNIQUE (nameNorm);
CREATE INDEX iCertWorkItemRefTargetOid ON m_access_cert_wi_reference (targetOid);
 */
-- endregion

-- region ObjectTemplateType
CREATE TABLE m_object_template (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_TEMPLATE') STORED
        CHECK (objectType = 'OBJECT_TEMPLATE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_object_template_oid_insert_tr BEFORE INSERT ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_object_template_update_tr BEFORE UPDATE ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_object_template_oid_delete_tr AFTER DELETE ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_object_template_nameOrig_idx ON m_object_template (nameOrig);
ALTER TABLE m_object_template ADD CONSTRAINT m_object_template_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_object_template_subtypes_idx ON m_object_template USING gin(subtypes);
CREATE INDEX m_object_template_policySituation_idx
    ON m_object_template USING gin(policysituations gin__int_ops);

-- stores ObjectTemplateType/includeRef
CREATE TABLE m_ref_include (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('INCLUDE') STORED
        CHECK (referenceType = 'INCLUDE'),

    PRIMARY KEY (ownerOid, relationId, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_includeTargetOidRelationId_idx
    ON m_ref_include (targetOid, relationId);
-- endregion

-- region FunctionLibrary/Sequence/Form tables
-- Represents FunctionLibraryType, see https://wiki.evolveum.com/display/midPoint/Function+Libraries
CREATE TABLE m_function_library (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FUNCTION_LIBRARY') STORED
        CHECK (objectType = 'FUNCTION_LIBRARY')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_function_library_oid_insert_tr BEFORE INSERT ON m_function_library
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_function_library_update_tr BEFORE UPDATE ON m_function_library
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_function_library_oid_delete_tr AFTER DELETE ON m_function_library
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_function_library_nameOrig_idx ON m_function_library (nameOrig);
ALTER TABLE m_function_library ADD CONSTRAINT m_function_library_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_function_library_subtypes_idx ON m_function_library USING gin(subtypes);
CREATE INDEX m_function_library_policySituation_idx
    ON m_function_library USING gin(policysituations gin__int_ops);

-- Represents SequenceType, see https://wiki.evolveum.com/display/midPoint/Sequences
CREATE TABLE m_sequence (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SEQUENCE') STORED
        CHECK (objectType = 'SEQUENCE')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_sequence_oid_insert_tr BEFORE INSERT ON m_sequence
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_sequence_update_tr BEFORE UPDATE ON m_sequence
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_sequence_oid_delete_tr AFTER DELETE ON m_sequence
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_sequence_nameOrig_idx ON m_sequence (nameOrig);
ALTER TABLE m_sequence ADD CONSTRAINT m_sequence_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_sequence_subtypes_idx ON m_sequence USING gin(subtypes);
CREATE INDEX m_sequence_policySituation_idx ON m_sequence USING gin(policysituations gin__int_ops);

-- Represents FormType, see https://wiki.evolveum.com/display/midPoint/Custom+forms
CREATE TABLE m_form (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('FORM') STORED
        CHECK (objectType = 'FORM')
)
    INHERITS (m_assignment_holder);

CREATE TRIGGER m_form_oid_insert_tr BEFORE INSERT ON m_form
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_form_update_tr BEFORE UPDATE ON m_form
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_form_oid_delete_tr AFTER DELETE ON m_form
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_form_nameOrig_idx ON m_form (nameOrig);
ALTER TABLE m_form ADD CONSTRAINT m_form_nameNorm_key UNIQUE (nameNorm);
CREATE INDEX m_form_subtypes_idx ON m_form USING gin(subtypes);
CREATE INDEX m_form_policySituation_idx ON m_form USING gin(policysituations gin__int_ops);
-- endregion

-- region Assignment/Inducement table
-- Represents AssignmentType, see https://wiki.evolveum.com/display/midPoint/Assignment
-- and also https://wiki.evolveum.com/display/midPoint/Assignment+vs+Inducement
-- TODO: partitioning, probably not by object type, it's not even... hash-something?
-- select assignmentowner, count(*) From m_assignment group by assignmentowner;
--1	45 (inducements)
--0	48756229
CREATE TABLE m_assignment (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- this is different from other containers, this is not generated, app must provide it
    containerType ContainerType NOT NULL CHECK (containerType IN ('ASSIGNMENT', 'INDUCEMENT')),
    ownerType ObjectType NOT NULL,
    lifecycleState TEXT,
    orderValue INTEGER,
    orgRefTargetOid UUID,
    orgRefTargetType ObjectType,
    orgRefRelationId INTEGER REFERENCES m_uri(id),
    targetRefTargetOid UUID,
    targetRefTargetType ObjectType,
    targetRefRelationId INTEGER REFERENCES m_uri(id),
    tenantRefTargetOid UUID,
    tenantRefTargetType ObjectType,
    tenantRefRelationId INTEGER REFERENCES m_uri(id),
    -- TODO what is this? see RAssignment.getExtension (both extId/Oid)
    extId INTEGER,
    extOid TEXT, -- is this UUID too?
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
    createChannelId INTEGER,
    createTimestamp TIMESTAMPTZ,
    modifierRefTargetOid UUID,
    modifierRefTargetType ObjectType,
    modifierRefRelationId INTEGER REFERENCES m_uri(id),
    modifyChannelId INTEGER,
    modifyTimestamp TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

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

-- stores assignment/metadata/createApproverRef
CREATE TABLE m_assignment_ref_create_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignmentCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_CREATE_APPROVER') STORED
        CHECK (referenceType = 'ASSIGNMENT_CREATE_APPROVER'),

    PRIMARY KEY (ownerOid, assignmentCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_assignment_ref_create_approver ADD CONSTRAINT m_assignment_ref_create_approver_id_fk
    FOREIGN KEY (ownerOid, assignmentCid) REFERENCES m_assignment (ownerOid, cid);

-- TODO index targetOid, relationId?

-- stores assignment/metadata/modifyApproverRef
CREATE TABLE m_assignment_ref_modify_approver (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignmentCid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_MODIFY_APPROVER') STORED
        CHECK (referenceType = 'ASSIGNMENT_MODIFY_APPROVER'),

    PRIMARY KEY (ownerOid, assignmentCid, referenceType, relationId, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_assignment_ref_modify_approver ADD CONSTRAINT m_assignment_ref_modify_approver_id_fk
    FOREIGN KEY (ownerOid, assignmentCid) REFERENCES m_assignment (ownerOid, cid);

-- TODO index targetOid, relationId?
-- endregion

-- region Other object containers
-- stores ObjectType/trigger (TriggerType)
CREATE TABLE m_trigger (
    ownerOid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('TRIGGER') STORED
        CHECK (containerType = 'TRIGGER'),
    handlerUriId INTEGER REFERENCES m_uri(id),
    timestampValue TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_trigger_timestampValue_idx ON m_trigger (timestampValue);

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
    timestampValue TIMESTAMPTZ,

    PRIMARY KEY (ownerOid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_operation_execution_initiatorRefTargetOid_idx
    ON m_operation_execution (initiatorRefTargetOid);
CREATE INDEX m_operation_execution_taskRefTargetOid_idx
    ON m_operation_execution (taskRefTargetOid);
CREATE INDEX m_operation_execution_timestampValue_idx ON m_operation_execution (timestampValue);
-- index for ownerOid is part of PK
-- TODO: index for status is questionable, don't we want WHERE status = ... to another index instead?
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
-- endregion

/*
-- TODO audit
CREATE TABLE m_audit_delta (
  checksum          VARCHAR(32) NOT NULL,
  record_id         BIGINT        NOT NULL,
  delta             BYTEA,
  deltaOid          UUID,
  deltaType         INTEGER,
  fullResult        BYTEA,
  objectNameNorm   TEXT,
  objectNameOrig   TEXT,
  resourceNameNorm TEXT,
  resourceNameOrig TEXT,
  resourceOid       UUID,
  status            INTEGER,
  PRIMARY KEY (record_id, checksum)
);
CREATE TABLE m_audit_event (
  id                BIGSERIAL NOT NULL,
  attorneyName      TEXT,
  attorneyOid       UUID,
  channel           TEXT,
  eventIdentifier   TEXT,
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    TEXT,
  initiatorName     TEXT,
  initiatorOid      UUID,
  initiatorType     INTEGER,
  message           VARCHAR(1024),
  nodeIdentifier    TEXT,
  outcome           INTEGER,
  parameter         TEXT,
  remoteHostAddress TEXT,
  requestIdentifier TEXT,
  result            TEXT,
  sessionIdentifier TEXT,
  targetName        TEXT,
  targetOid         UUID,
  targetOwnerName   TEXT,
  targetOwnerOid    UUID,
  targetOwnerType   INTEGER,
  targetType        INTEGER,
  taskIdentifier    TEXT,
  taskOID           TEXT,
  timestampValue    TIMESTAMPTZ,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_item (
  changedItemPath VARCHAR(900) NOT NULL,
  record_id       BIGINT         NOT NULL,
  PRIMARY KEY (record_id, changedItemPath)
);
CREATE TABLE m_audit_prop_value (
  id        BIGSERIAL NOT NULL,
  name      TEXT,
  record_id BIGINT,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_ref_value (
  id              BIGSERIAL NOT NULL,
  name            TEXT,
  oid             UUID,
  record_id       BIGINT,
  targetNameNorm TEXT,
  targetNameOrig TEXT,
  type            TEXT,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_resource (
  resourceOid       TEXT NOT NULL,
  record_id       BIGINT         NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);

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

CREATE INDEX iObjectCreateTimestamp
  ON m_object (createTimestamp);
CREATE INDEX iObjectLifecycleState
  ON m_object (lifecycleState);
CREATE INDEX iExtensionBoolean
  ON m_object_ext_boolean (booleanValue);
CREATE INDEX iExtensionDate
  ON m_object_ext_date (dateValue);
CREATE INDEX iExtensionLong
  ON m_object_ext_long (longValue);
CREATE INDEX iExtensionPolyString
  ON m_object_ext_poly (orig);
CREATE INDEX iExtensionReference
  ON m_object_ext_reference (targetoid);
CREATE INDEX iExtensionString
  ON m_object_ext_string (stringValue);
CREATE INDEX iFocusAdministrative
  ON m_focus (administrativeStatus);
CREATE INDEX iFocusEffective
  ON m_focus (effectiveStatus);
CREATE INDEX iLocality
  ON m_focus (localityOrig);
CREATE INDEX iFocusValidFrom
  ON m_focus (validFrom);
CREATE INDEX iFocusValidTo
  ON m_focus (validTo);

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
ALTER TABLE m_focus_photo
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (ownerOid) REFERENCES m_focus;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (ownerOid) REFERENCES m_object;
ALTER TABLE m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object;

-- Thanks to Patrick Lightbody for submitting this...
--
-- In your Quartz properties file, you'll need to set
-- org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.PostgreSQLDelegate

drop table if exists qrtz_fired_triggers;
DROP TABLE if exists QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE if exists QRTZ_SCHEDULER_STATE;
DROP TABLE if exists QRTZ_LOCKS;
drop table if exists qrtz_simple_triggers;
drop table if exists qrtz_cron_triggers;
drop table if exists qrtz_simprop_triggers;
DROP TABLE if exists QRTZ_BLOB_TRIGGERS;
drop table if exists qrtz_triggers;
drop table if exists qrtz_job_details;
drop table if exists qrtz_calendars;

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_NONCONCURRENT BOOL NOT NULL,
    IS_UPDATE_DATA BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    EXECUTION_GROUP VARCHAR(200) NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
    REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOL NULL,
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);


CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    EXECUTION_GROUP VARCHAR(200) NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);
*/

-- region Schema versioning and upgrading
/*
Procedure applying a DB schema/data change. Use sequential change numbers to identify the changes.
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
            -- even with force we never want to set lower change number, hence the IF above
            UPDATE m_global_metadata SET value = changeNumber WHERE name = 'schemaChangeNumber';
        END IF;
        COMMIT;
    ELSE
        RAISE NOTICE 'Change #% skipped, last change #% is newer!', changeNumber, lastChange;
    END IF;
END $$;
-- endregion
