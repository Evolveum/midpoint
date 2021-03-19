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

-- just in case PUBLIC schema was dropped (fastest way to remove all midpoint objects)
-- drop schema public cascade;
CREATE SCHEMA IF NOT EXISTS public;
CREATE EXTENSION IF NOT EXISTS intarray; -- support for indexing INTEGER[] columns

-- region custom enum types
-- Some enums are from schema, some are only defined in repo-sqale.
-- All Java enum types must be registered in SqaleRepoContext constructor.

-- First purely repo-sqale enums (these have M prefix in Java, the rest of the name is the same):
CREATE TYPE ContainerType AS ENUM (
    'ACCESS_CERTIFICATION_CASE',
    'ACCESS_CERTIFICATION_WORK_ITEM',
    'ASSIGNMENT',
    'INDUCEMENT',
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
    'DELEGATED',
    'INCLUDE',
    'PROJECTION',
    'OBJECT_CREATE_APPROVER',
    'OBJECT_MODIFY_APPROVER',
    'OBJECT_PARENT_ORG',
    'PERSONA',
    'RESOURCE_BUSINESS_CONFIGURATION_APPROVER',
    'ROLE_MEMBERSHIP');

-- Schema based enums have the same name like their enum classes (I like the Type suffix here):
CREATE TYPE ActivationStatusType AS ENUM ('ENABLED', 'DISABLED', 'ARCHIVED');

CREATE TYPE AvailabilityStatusType AS ENUM ('DOWN', 'UP', 'BROKEN');

CREATE TYPE TimeIntervalStatusType AS ENUM ('BEFORE', 'IN', 'AFTER');

CREATE TYPE OperationResultStatusType AS ENUM ('SUCCESS', 'WARNING', 'PARTIAL_ERROR',
    'FATAL_ERROR', 'HANDLED_ERROR', 'NOT_APPLICABLE', 'IN_PROGRESS', 'UNKNOWN');

CREATE TYPE ResourceAdministrativeStateType AS ENUM ('ENABLED', 'DISABLED');

CREATE TYPE TaskExecutionStatusType AS ENUM ('RUNNABLE', 'WAITING', 'SUSPENDED', 'CLOSED');

CREATE TYPE TaskWaitingReasonType AS ENUM ('OTHER_TASKS', 'OTHER');
-- endregion

-- region OID-pool table
-- To support gen_random_uuid() pgcrypto extension must be enabled for the database (not for PG 13).
-- select * from pg_available_extensions order by name;
DO $$
BEGIN
    perform pg_get_functiondef('gen_random_uuid()'::regprocedure);
    raise notice 'gen_random_uuid already exists, skipping create EXTENSION pgcrypto';
EXCEPTION WHEN undefined_function THEN
    create EXTENSION pgcrypto;
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
        insert into m_object_oid values (NEW.oid);
    ELSE
        insert into m_object_oid DEFAULT VALUES RETURNING oid INTO NEW.oid;
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

-- region Enumeration/code tables
-- Catalog of often used URIs, typically channels and relation Q-names.
-- Never update values of "uri" manually to change URI for some objects
-- (unless you really want to migrate old URI to a new one).
-- URI can be anything, for QNames the format is based on QNameUtil ("prefix-url#localPart").
CREATE TABLE m_uri (
    id SERIAL NOT NULL PRIMARY KEY,
    uri TEXT/*VARCHAR(255)*/ NOT NULL UNIQUE
);

INSERT INTO m_uri (id, uri)
    VALUES (0, 'http://midpoint.evolveum.com/xml/ns/public/common/org-3#default');
-- TODO pre-fill with various PrismConstants?
-- endregion

-- region for abstract tables m_object/container/reference
-- Purely abstract table (no entries are allowed). Represents ObjectType+ArchetypeHolderType.
-- See https://wiki.evolveum.com/display/midPoint/ObjectType
-- Following is recommended for each concrete table (see m_resource for example):
-- 1) override OID like this (PK+FK): oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
-- 2) define object type class (change value): objectType ObjectType GENERATED ALWAYS AS ('XY') STORED,
-- 3) add three triggers <table_name>_oid_{insert|update|delete}_tr
-- 4) add indexes for name_norm and name_orig columns (name_norm as unique)
-- 5) the rest varies on the concrete table, other indexes or constraints, etc.
-- 6) any required FK must be created on the concrete table, even for inherited columns

-- TODO EXPERIMENT: consider TEXT instead of VARCHAR, see: https://dba.stackexchange.com/a/21496/157622
--  Even VARCHAR without length can be used.
CREATE TABLE m_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    oid UUID NOT NULL,
    -- objectType will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL,
    name_orig TEXT/*VARCHAR(255)*/ NOT NULL,
    name_norm TEXT/*VARCHAR(255)*/ NOT NULL,
    fullObject BYTEA,
    tenantRef_targetOid UUID,
    tenantRef_targetType ObjectType,
    tenantRef_relation_id INTEGER, -- soft-references m_uri,
    lifecycleState TEXT/*VARCHAR(255)*/, -- TODO what is this? how many distinct values?
    cid_seq BIGINT NOT NULL DEFAULT 1, -- sequence for container id, next free cid
    version INTEGER NOT NULL DEFAULT 1,
    -- complex DB columns, add indexes as needed per concrete table, e.g. see m_user
    -- TODO compare with [] in JSONB, check performance, indexing, etc. first
    policySituations INTEGER[], -- soft-references m_uri, add index per table as/if needed
    subtypes TEXT[],
    textInfo TEXT[], -- TODO not mapped yet
    ext JSONB,
    -- metadata
    creatorRef_targetOid UUID,
    creatorRef_targetType ObjectType,
    creatorRef_relation_id INTEGER, -- soft-references m_uri,
    createChannel_id INTEGER, -- soft-references m_uri
    createTimestamp TIMESTAMPTZ,
    modifierRef_targetOid UUID,
    modifierRef_targetType ObjectType,
    modifierRef_relation_id INTEGER, -- soft-references m_uri
    modifyChannel_id INTEGER, -- soft-references m_uri,
    modifyTimestamp TIMESTAMPTZ,

    -- these are purely DB-managed metadata, not mapped to in midPoint
    db_created TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    db_modified TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, -- updated in update trigger

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
);
-- No indexes here, always add indexes and referential constraints on concrete sub-tables.

-- Purely abstract table (no entries are allowed). Represents Containerable/PrismContainerValue.
-- Allows querying all separately persisted containers, but not necessary for the application.
CREATE TABLE m_container (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    -- Owner does not have to be the direct parent of the container.
    owner_oid UUID NOT NULL,
    -- use like this on the concrete table:
    -- owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),

    -- Container ID, unique in the scope of the whole object (owner).
    -- While this provides it for sub-tables we will repeat this for clarity, it's part of PK.
    cid BIGINT NOT NULL,
    -- containerType will be overridden with GENERATED value in concrete table
    containerType ContainerType NOT NULL,

    CHECK (FALSE) NO INHERIT
    -- add on concrete table (additional columns possible): PRIMARY KEY (owner_oid, cid)
);

-- Abstract reference table, for object but also other container references.
CREATE TABLE m_reference (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    -- reference_type will be overridden with GENERATED value in concrete table
    referenceType ReferenceType NOT NULL,
    targetOid UUID NOT NULL, -- soft-references m_object
    targetType ObjectType NOT NULL,
    relation_id INTEGER NOT NULL, -- soft-references m_uri

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
    -- add PK (referenceType is the same per table): PRIMARY KEY (owner_oid, relation_id, targetOid)
);
-- Add this index for each sub-table (reference type is not necessary, each sub-table has just one).
-- CREATE INDEX m_reference_targetOid_relation_id_idx ON m_reference (targetOid, relation_id);

-- references related to ObjectType and AssignmentHolderType
-- stores AssignmentHolderType/archetypeRef
CREATE TABLE m_ref_archetype (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ARCHETYPE') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_archetype_targetOid_relation_id_idx
    ON m_ref_archetype (targetOid, relation_id);

-- stores AssignmentHolderType/delegatedRef
CREATE TABLE m_ref_delegated (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('DELEGATED') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_delegated_targetOid_relation_id_idx
    ON m_ref_delegated (targetOid, relation_id);

-- stores ObjectType/metadata/createApproverRef
CREATE TABLE m_ref_object_create_approver (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_CREATE_APPROVER') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_create_approver_targetOid_relation_id_idx
    ON m_ref_object_create_approver (targetOid, relation_id);

-- stores ObjectType/metadata/modifyApproverRef
CREATE TABLE m_ref_object_modify_approver (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_MODIFY_APPROVER') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_modify_approver_targetOid_relation_id_idx
    ON m_ref_object_modify_approver (targetOid, relation_id);

-- stores ObjectType/parentOrgRef
CREATE TABLE m_ref_object_parent_org (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('OBJECT_PARENT_ORG') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_object_parent_org_targetOid_relation_id_idx
    ON m_ref_object_parent_org (targetOid, relation_id);

-- stores AssignmentHolderType/roleMembershipRef
CREATE TABLE m_ref_role_membership (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('ROLE_MEMBERSHIP') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_role_member_targetOid_relation_id_idx
    ON m_ref_role_membership (targetOid, relation_id);
-- endregion

-- region FOCUS related tables
-- Represents FocusType (Users, Roles, ...), see https://wiki.evolveum.com/display/midPoint/Focus+and+Projections
-- extending m_object, but still abstract, hence DEFAULT for objectType and CHECK (false)
CREATE TABLE m_focus (
    -- will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL,
    costCenter TEXT/*VARCHAR(255)*/,
    emailAddress TEXT/*VARCHAR(255)*/,
    photo BYTEA, -- will be TOAST-ed if necessary
    locale TEXT/*VARCHAR(255)*/,
    locality_orig TEXT/*VARCHAR(255)*/,
    locality_norm TEXT/*VARCHAR(255)*/,
    preferredLanguage TEXT/*VARCHAR(255)*/,
    telephoneNumber TEXT/*VARCHAR(255)*/,
    timezone TEXT/*VARCHAR(255)*/,
    -- credential/password/metadata
    passwordCreateTimestamp TIMESTAMPTZ,
    passwordModifyTimestamp TIMESTAMPTZ,
    -- activation
    administrativeStatus ActivationStatusType,
    effectiveStatus ActivationStatusType,
    enableTimestamp TIMESTAMPTZ,
    disableTimestamp TIMESTAMPTZ,
    disableReason TEXT/*VARCHAR(255)*/,
    validityStatus TimeIntervalStatusType,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    archiveTimestamp TIMESTAMPTZ,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_object);

-- stores FocusType/personaRef
CREATE TABLE m_ref_persona (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PERSONA') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_persona_targetOid_relation_id_idx
    ON m_ref_persona (targetOid, relation_id);

-- stores FocusType/linkRef ("projection" is newer and better term)
CREATE TABLE m_ref_projection (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('PROJECTION') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_projection_targetOid_relation_id_idx
    ON m_ref_projection (targetOid, relation_id);
-- endregion

-- region USER related tables
-- Represents UserType, see https://wiki.evolveum.com/display/midPoint/UserType
CREATE TABLE m_user (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('USER') STORED,
    additionalName_orig TEXT/*VARCHAR(255)*/,
    additionalName_norm TEXT/*VARCHAR(255)*/,
    employeeNumber TEXT/*VARCHAR(255)*/,
    familyName_orig TEXT/*VARCHAR(255)*/,
    familyName_norm TEXT/*VARCHAR(255)*/,
    fullName_orig TEXT/*VARCHAR(255)*/,
    fullName_norm TEXT/*VARCHAR(255)*/,
    givenName_orig TEXT/*VARCHAR(255)*/,
    givenName_norm TEXT/*VARCHAR(255)*/,
    honorificPrefix_orig TEXT/*VARCHAR(255)*/,
    honorificPrefix_norm TEXT/*VARCHAR(255)*/,
    honorificSuffix_orig TEXT/*VARCHAR(255)*/,
    honorificSuffix_norm TEXT/*VARCHAR(255)*/,
    nickName_orig TEXT/*VARCHAR(255)*/,
    nickName_norm TEXT/*VARCHAR(255)*/,
    title_orig TEXT/*VARCHAR(255)*/,
    title_norm TEXT/*VARCHAR(255)*/
)
    INHERITS (m_focus);

CREATE TRIGGER m_user_oid_insert_tr BEFORE INSERT ON m_user
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_user_update_tr BEFORE UPDATE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_user_oid_delete_tr AFTER DELETE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_user_name_orig_idx ON m_user (name_orig);
ALTER TABLE m_user ADD CONSTRAINT m_user_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_user_policySituation_idx ON m_user USING GIN(policysituations gin__int_ops);
CREATE INDEX m_user_ext_idx ON m_user USING gin (ext);
CREATE INDEX m_user_fullName_orig_idx ON m_user (fullName_orig);
CREATE INDEX m_user_familyName_orig_idx ON m_user (familyName_orig);
CREATE INDEX m_user_givenName_orig_idx ON m_user (givenName_orig);
CREATE INDEX m_user_employeeNumber_idx ON m_user (employeeNumber);
-- endregion

-- region ROLE related tables
-- Represents AbstractRoleType, see https://wiki.evolveum.com/display/midPoint/Abstract+Role
CREATE TABLE m_abstract_role (
    -- will be overridden with GENERATED value in concrete table
    objectType ObjectType NOT NULL,
    autoAssignEnabled BOOLEAN,
    displayName_orig TEXT/*VARCHAR(255)*/,
    displayName_norm TEXT/*VARCHAR(255)*/,
    identifier TEXT/*VARCHAR(255)*/,
    requestable BOOLEAN,
    riskLevel TEXT/*VARCHAR(255)*/,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_focus);

-- Represents RoleType, see https://wiki.evolveum.com/display/midPoint/RoleType
CREATE TABLE m_role (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ROLE') STORED,
    roleType TEXT/*VARCHAR(255)*/
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_role_oid_insert_tr BEFORE INSERT ON m_role
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_role_update_tr BEFORE UPDATE ON m_role
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_role_oid_delete_tr AFTER DELETE ON m_role
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_role_name_orig_idx ON m_role (name_orig);
ALTER TABLE m_role ADD CONSTRAINT m_role_name_norm_key UNIQUE (name_norm);

-- Represents ServiceType, see https://wiki.evolveum.com/display/midPoint/Service+Account+Management
CREATE TABLE m_service (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SERVICE') STORED,
    displayOrder INTEGER
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_service_oid_insert_tr BEFORE INSERT ON m_service
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_service_update_tr BEFORE UPDATE ON m_service
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_service_oid_delete_tr AFTER DELETE ON m_service
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_service_name_orig_idx ON m_service (name_orig);
ALTER TABLE m_service ADD CONSTRAINT m_service_name_norm_key UNIQUE (name_norm);

-- Represents ArchetypeType, see https://wiki.evolveum.com/display/midPoint/Archetypes
CREATE TABLE m_archetype (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ARCHETYPE') STORED
)
    INHERITS (m_abstract_role);

CREATE TRIGGER m_archetype_oid_insert_tr BEFORE INSERT ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_archetype_update_tr BEFORE UPDATE ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_archetype_oid_delete_tr AFTER DELETE ON m_archetype
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_archetype_name_orig_idx ON m_archetype (name_orig);
ALTER TABLE m_archetype ADD CONSTRAINT m_archetype_name_norm_key UNIQUE (name_norm);
-- endregion

-- region Access Certification object tables
-- Represents AccessCertificationDefinitionType, see https://wiki.evolveum.com/display/midPoint/Access+Certification
CREATE TABLE m_access_cert_definition (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_DEFINITION') STORED,
    handlerUri_id INTEGER, -- soft-references m_uri
    lastCampaignStartedTimestamp TIMESTAMPTZ,
    lastCampaignClosedTimestamp TIMESTAMPTZ,
    ownerRef_targetOid UUID,
    ownerRef_targetType ObjectType,
    ownerRef_relation_id INTEGER -- soft-references m_uri
)
    INHERITS (m_object);

CREATE TRIGGER m_access_cert_definition_oid_insert_tr BEFORE INSERT ON m_access_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_access_cert_definition_update_tr BEFORE UPDATE ON m_access_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_access_cert_definition_oid_delete_tr AFTER DELETE ON m_access_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_access_cert_definition_name_orig_idx ON m_access_cert_definition (name_orig);
ALTER TABLE m_access_cert_definition
    ADD CONSTRAINT m_access_cert_definition_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_access_cert_definition_ext_idx ON m_access_cert_definition USING gin (ext);

-- TODO not mapped yet
CREATE TABLE m_access_cert_campaign (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CAMPAIGN') STORED,
    definitionRef_targetOid UUID,
    definitionRef_targetType ObjectType,
    definitionRef_relation_id INTEGER, -- soft-references m_uri
    endTimestamp TIMESTAMPTZ,
    handlerUri_id INTEGER, -- soft-references m_uri
    iteration INTEGER NOT NULL,
    ownerRef_targetOid UUID,
    ownerRef_targetType ObjectType,
    ownerRef_relation_id INTEGER, -- soft-references m_uri
    stageNumber INTEGER,
    startTimestamp TIMESTAMPTZ,
    state INTEGER
)
    INHERITS (m_object);

CREATE TRIGGER m_access_cert_campaign_oid_insert_tr BEFORE INSERT ON m_access_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_access_cert_campaign_update_tr BEFORE UPDATE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_access_cert_campaign_oid_delete_tr AFTER DELETE ON m_access_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_access_cert_campaign_name_orig_idx ON m_access_cert_campaign (name_orig);
ALTER TABLE m_access_cert_campaign ADD CONSTRAINT m_access_cert_campaign_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_access_cert_campaign_ext_idx ON m_access_cert_campaign USING gin (ext);

CREATE TABLE m_access_cert_case (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_CASE') STORED,
    administrativeStatus INTEGER,
    archiveTimestamp TIMESTAMPTZ,
    disableReason TEXT/*VARCHAR(255)*/,
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus INTEGER,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus INTEGER,
    currentStageOutcome TEXT/*VARCHAR(255)*/,
    fullObject BYTEA,
    iteration INTEGER NOT NULL,
    objectRef_targetOid UUID,
    objectRef_targetType ObjectType,
    objectRef_relation_id INTEGER, -- soft-references m_uri
    orgRef_targetOid UUID,
    orgRef_targetType ObjectType,
    orgRef_relation_id INTEGER, -- soft-references m_uri
    outcome TEXT/*VARCHAR(255)*/,
    remediedTimestamp TIMESTAMPTZ,
    reviewDeadline TIMESTAMPTZ,
    reviewRequestedTimestamp TIMESTAMPTZ,
    stageNumber INTEGER,
    targetRef_targetOid UUID,
    targetRef_targetType ObjectType,
    targetRef_relation_id INTEGER, -- soft-references m_uri
    tenantRef_targetOid UUID,
    tenantRef_targetType ObjectType,
    tenantRef_relation_id INTEGER, -- soft-references m_uri

    PRIMARY KEY (owner_oid, cid)
)
    INHERITS(m_container);

CREATE TABLE m_access_cert_wi (
    owner_oid UUID NOT NULL, -- PK+FK
    acc_cert_case_cid INTEGER NOT NULL, -- PK+FK
    containerType ContainerType GENERATED ALWAYS AS ('ACCESS_CERTIFICATION_WORK_ITEM') STORED,
    closeTimestamp TIMESTAMPTZ,
    iteration INTEGER NOT NULL,
    outcome TEXT/*VARCHAR(255)*/,
    outputChangeTimestamp TIMESTAMPTZ,
    performerRef_targetOid UUID,
    performerRef_targetType ObjectType,
    performerRef_relation_id INTEGER, -- soft-references m_uri
    stageNumber INTEGER,

    PRIMARY KEY (owner_oid, acc_cert_case_cid, cid)
)
    INHERITS(m_container);

ALTER TABLE m_access_cert_wi
    ADD CONSTRAINT m_access_cert_wi_id_fk FOREIGN KEY (owner_oid, acc_cert_case_cid)
        REFERENCES m_access_cert_case (owner_oid, cid)
            ON DELETE CASCADE;

CREATE TABLE m_access_cert_wi_reference (
    owner_oid UUID NOT NULL, -- PK+FK
    acc_cert_case_cid INTEGER NOT NULL, -- PK+FK
    acc_cert_wi_cid INTEGER NOT NULL, -- PK+FK
    targetOid UUID NOT NULL, -- more PK columns...
    targetType ObjectType,
    relation_id INTEGER NOT NULL, -- soft-references m_uri

    -- TODO is the order of last two components optimal for index/query?
    PRIMARY KEY (owner_oid, acc_cert_case_cid, acc_cert_wi_cid, relation_id, targetOid)
);

ALTER TABLE m_access_cert_wi_reference
    ADD CONSTRAINT m_access_cert_wi_reference_id_fk
        FOREIGN KEY (owner_oid, acc_cert_case_cid, acc_cert_wi_cid)
        REFERENCES m_access_cert_wi (owner_oid, acc_cert_case_cid, cid)
            ON DELETE CASCADE;
/*
CREATE INDEX iCertCampaignNameOrig ON m_access_cert_campaign (name_orig);
ALTER TABLE m_access_cert_campaign ADD CONSTRAINT uc_access_cert_campaign_name UNIQUE (name_norm);
CREATE INDEX iCaseObjectRefTargetOid ON m_access_cert_case (objectRef_targetOid);
CREATE INDEX iCaseTargetRefTargetOid ON m_access_cert_case (targetRef_targetOid);
CREATE INDEX iCaseTenantRefTargetOid ON m_access_cert_case (tenantRef_targetOid);
CREATE INDEX iCaseOrgRefTargetOid ON m_access_cert_case (orgRef_targetOid);
CREATE INDEX iCertDefinitionNameOrig ON m_access_cert_definition (name_orig);
ALTER TABLE m_access_cert_definition ADD CONSTRAINT uc_access_cert_definition_name UNIQUE (name_norm);
CREATE INDEX iCertWorkItemRefTargetOid ON m_access_cert_wi_reference (targetOid);
 */
-- endregion

-- region OTHER object tables
-- Represents ResourceType, see https://wiki.evolveum.com/display/midPoint/Resource+Configuration
CREATE TABLE m_resource (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('RESOURCE') STORED,
    business_administrativeState ResourceAdministrativeStateType,
    operationalState_lastAvailabilityStatus AvailabilityStatusType,
    connectorRef_targetOid UUID,
    connectorRef_targetType ObjectType,
    connectorRef_relation_id INTEGER -- soft-references m_uri
)
    INHERITS (m_object);

CREATE TRIGGER m_resource_oid_insert_tr BEFORE INSERT ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_resource_update_tr BEFORE UPDATE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_resource_oid_delete_tr AFTER DELETE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_resource_name_orig_idx ON m_resource (name_orig);
ALTER TABLE m_resource ADD CONSTRAINT m_resource_name_norm_key UNIQUE (name_norm);

-- stores ResourceType/business/approverRef
CREATE TABLE m_ref_resource_business_configuration_approver (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS
        ('RESOURCE_BUSINESS_CONFIGURATION_APPROVER') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_resource_biz_config_approver_targetOid_relation_id_idx
    ON m_ref_resource_business_configuration_approver (targetOid, relation_id);

-- TODO not mapped yet
CREATE TABLE m_shadow (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SHADOW') STORED,
    objectClass TEXT/*VARCHAR(157)*/ NOT NULL,
    resourceRef_targetOid UUID,
    resourceRef_targetType ObjectType,
    resourceRef_relation_id INTEGER, -- soft-references m_uri
    intent TEXT/*VARCHAR(255)*/,
    kind INTEGER,
    attemptNumber INTEGER,
    dead BOOLEAN,
    exist BOOLEAN,
    failedOperationType INTEGER,
    fullSynchronizationTimestamp TIMESTAMPTZ,
    pendingOperationCount INTEGER,
    primaryIdentifierValue TEXT/*VARCHAR(255)*/,
    status INTEGER,
    synchronizationSituation INTEGER,
    synchronizationTimestamp TIMESTAMPTZ
)
    INHERITS (m_object);

CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_shadow_update_tr BEFORE UPDATE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_shadow_name_orig_idx ON m_shadow (name_orig);
ALTER TABLE m_shadow ADD CONSTRAINT m_shadow_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_shadow_policySituation_idx ON m_shadow USING GIN(policysituations gin__int_ops);
CREATE INDEX m_shadow_ext_idx ON m_shadow USING gin (ext);
/*
TODO: reconsider, especially boolean things like dead (perhaps WHERE in other indexes?)
 Also consider partitioning by some of the attributes (class/kind/intent?)
CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);
CREATE INDEX iShadowDead ON m_shadow (dead);
CREATE INDEX iShadowKind ON m_shadow (kind);
CREATE INDEX iShadowIntent ON m_shadow (intent);
CREATE INDEX iShadowObjectClass ON m_shadow (objectClass);
CREATE INDEX iShadowFailedOperationType ON m_shadow (failedOperationType);
CREATE INDEX iShadowSyncSituation ON m_shadow (synchronizationSituation);
CREATE INDEX iShadowPendingOperationCount ON m_shadow (pendingOperationCount);
ALTER TABLE m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);
 */

-- Represents NodeType, see https://wiki.evolveum.com/display/midPoint/Managing+cluster+nodes
CREATE TABLE m_node (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('NODE') STORED,
    nodeIdentifier TEXT/*VARCHAR(255)*/
)
    INHERITS (m_object);

CREATE TRIGGER m_node_oid_insert_tr BEFORE INSERT ON m_node
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_node_update_tr BEFORE UPDATE ON m_node
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_node_oid_delete_tr AFTER DELETE ON m_node
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_node_name_orig_idx ON m_node (name_orig);
ALTER TABLE m_node ADD CONSTRAINT m_node_name_norm_key UNIQUE (name_norm);
-- not interested in ext index for this one, this table will be small

-- Represents SystemConfigurationType, see https://wiki.evolveum.com/display/midPoint/System+Configuration+Object
CREATE TABLE m_system_configuration (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SYSTEM_CONFIGURATION') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_system_configuration_oid_insert_tr BEFORE INSERT ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_system_configuration_update_tr BEFORE UPDATE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_system_configuration_oid_delete_tr AFTER DELETE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- no need for the name index, m_system_configuration table is very small

-- Represents SecurityPolicyType, see https://wiki.evolveum.com/display/midPoint/Security+Policy+Configuration
CREATE TABLE m_security_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('SECURITY_POLICY') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_security_policy_oid_insert_tr BEFORE INSERT ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_security_policy_update_tr BEFORE UPDATE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_security_policy_oid_delete_tr AFTER DELETE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- no need for the name index, m_security_policy table is very small

-- Represents ObjectCollectionType, see https://wiki.evolveum.com/display/midPoint/Object+Collections+and+Views+Configuration
CREATE TABLE m_object_collection (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_COLLECTION') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_object_collection_oid_insert_tr BEFORE INSERT ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_object_collection_update_tr BEFORE UPDATE ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_object_collection_oid_delete_tr AFTER DELETE ON m_object_collection
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_object_collection_name_orig_idx ON m_object_collection (name_orig);
ALTER TABLE m_object_collection ADD CONSTRAINT m_object_collection_name_norm_key UNIQUE (name_norm);

-- Represents DashboardType, see https://wiki.evolveum.com/display/midPoint/Dashboard+configuration
CREATE TABLE m_dashboard (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('DASHBOARD') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_dashboard_oid_insert_tr BEFORE INSERT ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_dashboard_update_tr BEFORE UPDATE ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_dashboard_oid_delete_tr AFTER DELETE ON m_dashboard
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_dashboard_name_orig_idx ON m_dashboard (name_orig);
ALTER TABLE m_dashboard ADD CONSTRAINT m_dashboard_name_norm_key UNIQUE (name_norm);

-- Represents ValuePolicyType
CREATE TABLE m_value_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('VALUE_POLICY') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_value_policy_oid_insert_tr BEFORE INSERT ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_value_policy_update_tr BEFORE UPDATE ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_value_policy_oid_delete_tr AFTER DELETE ON m_value_policy
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_value_policy_name_orig_idx ON m_value_policy (name_orig);
ALTER TABLE m_value_policy ADD CONSTRAINT m_value_policy_name_norm_key UNIQUE (name_norm);

-- Represents ReportType, see https://wiki.evolveum.com/display/midPoint/Report+Configuration
CREATE TABLE m_report (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT') STORED,
    export INTEGER,
    orientation INTEGER,
    parent BOOLEAN,
    useHibernateSession BOOLEAN
)
    INHERITS (m_object);

CREATE TRIGGER m_report_oid_insert_tr BEFORE INSERT ON m_report
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_report_update_tr BEFORE UPDATE ON m_report
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_report_oid_delete_tr AFTER DELETE ON m_report
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_report_name_orig_idx ON m_report (name_orig);
ALTER TABLE m_report ADD CONSTRAINT m_report_name_norm_key UNIQUE (name_norm);
-- TODO old repo had index on parent (boolean), does it make sense? if so, which value is sparse?

-- Represents ReportDataType, see also m_report above
CREATE TABLE m_report_data (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('REPORT_DATA') STORED,
    reportRef_targetOid UUID,
    reportRef_targetType ObjectType,
    reportRef_relation_id INTEGER -- soft-references m_uri
)
    INHERITS (m_object);

CREATE INDEX m_report_data_name_orig_idx ON m_report_data (name_orig);
ALTER TABLE m_report_data ADD CONSTRAINT m_report_data_name_norm_key UNIQUE (name_norm);

-- Represents LookupTableType, see https://wiki.evolveum.com/display/midPoint/Lookup+Tables
CREATE TABLE m_lookup_table (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('LOOKUP_TABLE') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_lookup_table_oid_insert_tr BEFORE INSERT ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_lookup_table_update_tr BEFORE UPDATE ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_lookup_table_oid_delete_tr AFTER DELETE ON m_lookup_table
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_lookup_table_name_orig_idx ON m_lookup_table (name_orig);
ALTER TABLE m_lookup_table ADD CONSTRAINT m_lookup_table_name_norm_key UNIQUE (name_norm);

-- Represents LookupTableRowType, see also m_lookup_table above
CREATE TABLE m_lookup_table_row (
    owner_oid UUID NOT NULL REFERENCES m_lookup_table(oid) ON DELETE CASCADE,
    row_id INTEGER NOT NULL,
    row_key TEXT/*VARCHAR(255)*/,
    label_orig TEXT/*VARCHAR(255)*/,
    label_norm TEXT/*VARCHAR(255)*/,
    row_value TEXT/*VARCHAR(255)*/,
    lastChangeTimestamp TIMESTAMPTZ,

    PRIMARY KEY (owner_oid, row_id)
);

ALTER TABLE m_lookup_table_row
    ADD CONSTRAINT m_lookup_table_row_owner_oid_row_key_key UNIQUE (owner_oid, row_key);

-- Represents ConnectorType, see https://wiki.evolveum.com/display/midPoint/Identity+Connectors
CREATE TABLE m_connector (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR') STORED,
    connectorBundle TEXT/*VARCHAR(255)*/,
    connectorType TEXT/*VARCHAR(255)*/,
    connectorVersion TEXT/*VARCHAR(255)*/,
    framework TEXT/*VARCHAR(255)*/,
    connectorHostRef_targetOid UUID,
    connectorHostRef_targetType ObjectType,
    connectorHostRef_relation_id INTEGER -- soft-references m_uri

)
    INHERITS (m_object);

CREATE TRIGGER m_connector_oid_insert_tr BEFORE INSERT ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_connector_update_tr BEFORE UPDATE ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_connector_oid_delete_tr AFTER DELETE ON m_connector
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_connector_name_orig_idx ON m_connector (name_orig);
ALTER TABLE m_connector ADD CONSTRAINT m_connector_name_norm_key UNIQUE (name_norm);

-- TODO array/json in m_connector table
-- CREATE TABLE m_connector_target_system (
--     connector_oid UUID NOT NULL,
--     targetSystemType TEXT/*VARCHAR(255)*/
-- );
-- ALTER TABLE m_connector_target_system
--     ADD CONSTRAINT fk_connector_target_system FOREIGN KEY (connector_oid) REFERENCES m_connector;

-- Represents ConnectorHostType, see https://wiki.evolveum.com/display/midPoint/Connector+Server
CREATE TABLE m_connector_host (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CONNECTOR_HOST') STORED,
    hostname TEXT/*VARCHAR(255)*/,
    port TEXT/*VARCHAR(32)*/
)
    INHERITS (m_object);

CREATE TRIGGER m_connector_host_oid_insert_tr BEFORE INSERT ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_connector_host_update_tr BEFORE UPDATE ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_connector_host_oid_delete_tr AFTER DELETE ON m_connector_host
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_connector_host_name_orig_idx ON m_connector_host (name_orig);
ALTER TABLE m_connector_host ADD CONSTRAINT m_connector_host_name_norm_key UNIQUE (name_norm);

-- Represents persistent TaskType, see https://wiki.evolveum.com/display/midPoint/Task+Manager
CREATE TABLE m_task (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('TASK') STORED,
    binding INTEGER,
    category TEXT/*VARCHAR(255)*/,
    completionTimestamp TIMESTAMPTZ,
    executionStatus TaskExecutionStatusType,
    fullResult BYTEA,
    handlerUri_id INTEGER, -- soft-references m_uri
    lastRunFinishTimestamp TIMESTAMPTZ,
    lastRunStartTimestamp TIMESTAMPTZ,
    node TEXT/*VARCHAR(255)*/, -- node_id only for information purposes
    objectRef_targetOid UUID,
    objectRef_targetType ObjectType,
    objectRef_relation_id INTEGER, -- soft-references m_uri
    ownerRef_targetOid UUID,
    ownerRef_targetType ObjectType,
    ownerRef_relation_id INTEGER, -- soft-references m_uri
    parent TEXT/*VARCHAR(255)*/, -- value of taskIdentifier
    recurrence INTEGER,
    resultStatus OperationResultStatusType,
    taskIdentifier TEXT/*VARCHAR(255)*/,
    threadStopAction INTEGER,
    waitingReason TaskWaitingReasonType
)
    INHERITS (m_object);

CREATE TRIGGER m_task_oid_insert_tr BEFORE INSERT ON m_task
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_task_update_tr BEFORE UPDATE ON m_task
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_task_oid_delete_tr AFTER DELETE ON m_task
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_task_name_orig_idx ON m_task (name_orig);
ALTER TABLE m_task ADD CONSTRAINT m_task_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_task_parent_idx ON m_task (parent);
CREATE INDEX m_task_objectRef_targetOid_idx ON m_task(objectRef_targetOid);
ALTER TABLE m_task ADD CONSTRAINT m_task_taskIdentifier_key UNIQUE (taskIdentifier);

/* TODO inline as array or json to m_task
CREATE TABLE m_task_dependent (
    task_oid UUID NOT NULL,
    dependent TEXT/*VARCHAR(255)*/
);
ALTER TABLE m_task_dependent
    ADD CONSTRAINT fk_task_dependent FOREIGN KEY (task_oid) REFERENCES m_task;
CREATE INDEX iTaskDependentOid ON M_TASK_DEPENDENT(TASK_OID);
*/

-- Represents CaseType, see https://wiki.evolveum.com/display/midPoint/Case+Management
CREATE TABLE m_case (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('CASE') STORED,
    state TEXT/*VARCHAR(255)*/,
    closeTimestamp TIMESTAMPTZ,
    objectRef_targetOid UUID,
    objectRef_targetType ObjectType,
    objectRef_relation_id INTEGER, -- soft-references m_uri
    parentRef_targetOid UUID,
    parentRef_targetType ObjectType,
    parentRef_relation_id INTEGER, -- soft-references m_uri
    requestorRef_targetOid UUID,
    requestorRef_targetType ObjectType,
    requestorRef_relation_id INTEGER, -- soft-references m_uri
    targetRef_targetOid UUID,
    targetRef_targetType ObjectType,
    targetRef_relation_id INTEGER -- soft-references m_uri
)
    INHERITS (m_object);

CREATE TRIGGER m_case_oid_insert_tr BEFORE INSERT ON m_case
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_case_update_tr BEFORE UPDATE ON m_case
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_case_oid_delete_tr AFTER DELETE ON m_case
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_case_name_orig_idx ON m_case (name_orig);
ALTER TABLE m_case ADD CONSTRAINT m_case_name_norm_key UNIQUE (name_norm);

/*
CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid);
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid);
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid);
CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);
ALTER TABLE m_case_wi
    ADD CONSTRAINT fk_case_wi_owner FOREIGN KEY (owner_oid) REFERENCES m_case;
ALTER TABLE m_case_wi_reference
    ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi;
*/
-- endregion

-- region ObjectTemplateType
CREATE TABLE m_object_template (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectType ObjectType GENERATED ALWAYS AS ('OBJECT_TEMPLATE') STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_object_template_oid_insert_tr BEFORE INSERT ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_object_template_update_tr BEFORE UPDATE ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_object_template_oid_delete_tr AFTER DELETE ON m_object_template
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_object_template_name_orig_idx ON m_object_template (name_orig);
ALTER TABLE m_object_template ADD CONSTRAINT m_object_template_name_norm_key UNIQUE (name_norm);

-- stores ObjectTemplateType/includeRef
CREATE TABLE m_ref_include (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    referenceType ReferenceType GENERATED ALWAYS AS ('INCLUDE') STORED,

    PRIMARY KEY (owner_oid, relation_id, targetOid)
)
    INHERITS (m_reference);

CREATE INDEX m_ref_include_targetOid_relation_id_idx
    ON m_ref_include (targetOid, relation_id);
-- endregion

-- region Assignment/Inducement tables
-- Represents AssignmentType, see https://wiki.evolveum.com/display/midPoint/Assignment
-- and also https://wiki.evolveum.com/display/midPoint/Assignment+vs+Inducement
-- TODO: if we never need mix of inducements and assignments then let's have two separate tables
-- consult with Rado/Katka/Palo
-- TODO: partitioning, not by object type, it's not even... hash-something?
-- select assignmentowner, count(*) From m_assignment group by assignmentowner;
--1	45 (inducements)
--0	48756229
-- abstract common structure for m_assignment and m_inducement
CREATE TABLE m_assignment_type (
    owner_oid UUID NOT NULL, -- see sub-tables for PK definition
    containerType ContainerType NOT NULL,
    -- new column may avoid join to object for some queries
    owner_type ObjectType NOT NULL,
    lifecycleState TEXT/*VARCHAR(255)*/,
    orderValue INTEGER,
    orgRef_targetOid UUID,
    orgRef_targetType ObjectType,
    orgRef_relation_id INTEGER, -- soft-references m_uri
    targetRef_targetOid UUID,
    targetRef_targetType ObjectType,
    targetRef_relation_id INTEGER, -- soft-references m_uri
    tenantRef_targetOid UUID,
    tenantRef_targetType ObjectType,
    tenantRef_relation_id INTEGER, -- soft-references m_uri
    -- TODO what is this? see RAssignment.getExtension (both extId/Oid)
    extId INTEGER,
    extOid TEXT/*VARCHAR(36)*/, -- is this UUID too?
    policySituations INTEGER[], -- soft-references m_uri, add index per table
    ext JSONB,
    -- construction
    resourceRef_targetOid UUID,
    resourceRef_targetType ObjectType,
    resourceRef_relation_id INTEGER, -- soft-references m_uri
    -- activation
    administrativeStatus ActivationStatusType,
    effectiveStatus ActivationStatusType,
    enableTimestamp TIMESTAMPTZ,
    disableTimestamp TIMESTAMPTZ,
    disableReason TEXT/*VARCHAR(255)*/,
    validityStatus TimeIntervalStatusType,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    archiveTimestamp TIMESTAMPTZ,
    -- metadata
    creatorRef_targetOid UUID,
    creatorRef_targetType ObjectType,
    creatorRef_relation_id INTEGER, -- soft-references m_uri
    createChannel_id INTEGER,
    createTimestamp TIMESTAMPTZ,
    modifierRef_targetOid UUID,
    modifierRef_targetType ObjectType,
    modifierRef_relation_id INTEGER, -- soft-references m_uri
    modifyChannel_id INTEGER,
    modifyTimestamp TIMESTAMPTZ,

    PRIMARY KEY (owner_oid, cid),
    -- no need to index owner_oid, it's part of the PK index

    CHECK (FALSE) NO INHERIT
)
    INHERITS(m_container);

CREATE TABLE m_assignment (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('ASSIGNMENT') STORED,

    PRIMARY KEY (owner_oid, cid)
    -- no need to index owner_oid, it's part of the PK index
)
    INHERITS(m_assignment_type);

CREATE INDEX m_assignment_policySituation_idx ON m_assignment USING GIN(policysituations gin__int_ops);
CREATE INDEX m_assignment_ext_idx ON m_assignment USING gin (ext);
-- TODO was: CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);
-- administrativeStatus has 3 states (ENABLED/DISABLED/ARCHIVED), not sure it's worth indexing
-- but it can be used as a condition to index other (e.g. WHERE administrativeStatus='ENABLED')
-- TODO the same: CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);
CREATE INDEX m_assignment_validFrom_idx ON m_assignment (validFrom);
CREATE INDEX m_assignment_validTo_idx ON m_assignment (validTo);
CREATE INDEX m_assignment_targetRef_targetOid_idx ON m_assignment (targetRef_targetOid);
CREATE INDEX m_assignment_tenantRef_targetOid_idx ON m_assignment (tenantRef_targetOid);
CREATE INDEX m_assignment_orgRef_targetOid_idx ON m_assignment (orgRef_targetOid);
CREATE INDEX m_assignment_resourceRef_targetOid_idx ON m_assignment (resourceRef_targetOid);

-- stores assignment/metadata/createApproverRef
CREATE TABLE m_assignment_ref_create_approver (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignment_cid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_CREATE_APPROVER') STORED,

    PRIMARY KEY (owner_oid, assignment_cid, referenceType, relation_id, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_assignment_ref_create_approver ADD CONSTRAINT m_assignment_ref_create_approver_id_fk
    FOREIGN KEY (owner_oid, assignment_cid) REFERENCES m_assignment (owner_oid, cid);

-- TODO index targetOid, relation_id?

-- stores assignment/metadata/modifyApproverRef
CREATE TABLE m_assignment_ref_modify_approver (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    assignment_cid INTEGER NOT NULL,
    referenceType ReferenceType GENERATED ALWAYS AS ('ASSIGNMENT_MODIFY_APPROVER') STORED,

    PRIMARY KEY (owner_oid, assignment_cid, referenceType, relation_id, targetOid)
)
    INHERITS (m_reference);

ALTER TABLE m_assignment_ref_modify_approver ADD CONSTRAINT m_assignment_ref_modify_approver_id_fk
    FOREIGN KEY (owner_oid, assignment_cid) REFERENCES m_assignment (owner_oid, cid);

-- TODO index targetOid, relation_id?

CREATE TABLE m_inducement (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('INDUCEMENT') STORED,

    PRIMARY KEY (owner_oid, cid)
    -- no need to index owner_oid, it's part of the PK index
)
    INHERITS(m_assignment_type);

CREATE INDEX m_inducement_ext_idx ON m_inducement USING gin (ext);
CREATE INDEX m_inducement_validFrom_idx ON m_inducement (validFrom);
CREATE INDEX m_inducement_validTo_idx ON m_inducement (validTo);
CREATE INDEX m_inducement_targetRef_targetOid_idx ON m_inducement (targetRef_targetOid);
CREATE INDEX m_inducement_tenantRef_targetOid_idx ON m_inducement (tenantRef_targetOid);
CREATE INDEX m_inducement_orgRef_targetOid_idx ON m_inducement (orgRef_targetOid);
CREATE INDEX m_inducement_resourceRef_targetOid_idx ON m_inducement (resourceRef_targetOid);

-- TODO other tables like for assignments needed? policy situations, refs?
-- endregion

-- region Other object containers
-- stores ObjectType/trigger
CREATE TABLE m_trigger (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid) ON DELETE CASCADE,
    containerType ContainerType GENERATED ALWAYS AS ('TRIGGER') STORED,
    handlerUri_id INTEGER, -- soft-references m_uri
    timestampValue TIMESTAMPTZ,

    PRIMARY KEY (owner_oid, cid)
)
    INHERITS(m_container);

CREATE INDEX m_trigger_timestampValue_idx ON m_trigger (timestampValue);
-- endregion

-- region Extension support
-- TODO: catalog unused at the moment
CREATE TABLE m_ext_item (
    id SERIAL NOT NULL,
    kind INTEGER, -- see RItemKind, is this necessary? does it contain cardinality?
    name TEXT/*VARCHAR(157)*/, -- no path for nested props needed?
    type TEXT/*VARCHAR(32)*/, -- data type TODO enum?
    storageType TEXT/*VARCHAR(32)*/ NOT NULL default 'EXT_JSON', -- type of storage (JSON, column, table separate/common, etc.)
    storageInfo TEXT/*VARCHAR(32)*/, -- optional storage detail, name of column or table if necessary

    PRIMARY KEY (id)
);
-- endregion

/*
-- EXPERIMENTAL EAV (first without catalog, so string keys are used)
CREATE TABLE m_object_ext_boolean (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    value BOOLEAN NOT NULL,
    PRIMARY KEY (owner_oid, ext_item_id, value)
);
CREATE TABLE m_object_ext_date (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    value TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_oid, ext_item_id, value)
);
CREATE TABLE m_object_ext_long (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    value INTEGER NOT NULL,
    PRIMARY KEY (owner_oid, ext_item_id, value)
);
CREATE TABLE m_object_ext_poly (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    orig TEXT/*VARCHAR(255)*/ NOT NULL,
    norm TEXT/*VARCHAR(255)*/,
    PRIMARY KEY (owner_oid, ext_item_id, orig)
);
CREATE TABLE m_object_ext_reference (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    target_oid UUID NOT NULL,
    relation_id INTEGER,
    targetType INTEGER,
    PRIMARY KEY (owner_oid, ext_item_id, target_oid)
);
CREATE TABLE m_object_ext_string (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    value TEXT/*VARCHAR(255)*/ NOT NULL,
    PRIMARY KEY (owner_oid, ext_item_id, value)
);


-- TODO what of assignment extensions? Can they change for various types of assignments?
--  Then what? Inheritance is impractical, legacy extension tables are unwieldy.

-- TODO other indexes, only PKs/FKs are defined at the moment

-- TODO hopefully replaced by JSON ext column and not needed
CREATE TABLE m_assignment_ext_boolean (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid UUID NOT NULL,
  booleanValue                 BOOLEAN     NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)
);
CREATE TABLE m_assignment_ext_date (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid UUID NOT NULL,
  dateValue                    TIMESTAMPTZ   NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)
);
CREATE TABLE m_assignment_ext_long (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid UUID NOT NULL,
  longValue                    BIGINT        NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)
);
CREATE TABLE m_assignment_ext_poly (
  item_id                      INTEGER         NOT NULL,
  anyContainer_owner_id        INTEGER         NOT NULL,
  anyContainer_owner_owner_oid UUID  NOT NULL,
  orig                         TEXT/*VARCHAR(255)*/ NOT NULL,
  norm                         TEXT/*VARCHAR(255)*/,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)
);
CREATE TABLE m_assignment_ext_reference (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid UUID NOT NULL,
  targetoid                    UUID NOT NULL,
  relation                     VARCHAR(157),
  targetType                   INTEGER,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)
);
CREATE TABLE m_assignment_ext_string (
  item_id                      INTEGER         NOT NULL,
  anyContainer_owner_id        INTEGER         NOT NULL,
  anyContainer_owner_owner_oid UUID  NOT NULL,
  stringValue                  TEXT/*VARCHAR(255)*/ NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)
);
CREATE TABLE m_assignment_extension (
  owner_id        INTEGER        NOT NULL,
  owner_owner_oid UUID NOT NULL,
  PRIMARY KEY (owner_owner_oid, owner_id)
);


-- TODO HERE
CREATE TABLE m_audit_delta (
  checksum          VARCHAR(32) NOT NULL,
  record_id         BIGINT        NOT NULL,
  delta             BYTEA,
  deltaOid          UUID,
  deltaType         INTEGER,
  fullResult        BYTEA,
  objectName_norm   TEXT/*VARCHAR(255)*/,
  objectName_orig   TEXT/*VARCHAR(255)*/,
  resourceName_norm TEXT/*VARCHAR(255)*/,
  resourceName_orig TEXT/*VARCHAR(255)*/,
  resourceOid       UUID,
  status            INTEGER,
  PRIMARY KEY (record_id, checksum)
);
CREATE TABLE m_audit_event (
  id                BIGSERIAL NOT NULL,
  attorneyName      TEXT/*VARCHAR(255)*/,
  attorneyOid       UUID,
  channel           TEXT/*VARCHAR(255)*/,
  eventIdentifier   TEXT/*VARCHAR(255)*/,
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    TEXT/*VARCHAR(255)*/,
  initiatorName     TEXT/*VARCHAR(255)*/,
  initiatorOid      UUID,
  initiatorType     INTEGER,
  message           VARCHAR(1024),
  nodeIdentifier    TEXT/*VARCHAR(255)*/,
  outcome           INTEGER,
  parameter         TEXT/*VARCHAR(255)*/,
  remoteHostAddress TEXT/*VARCHAR(255)*/,
  requestIdentifier TEXT/*VARCHAR(255)*/,
  result            TEXT/*VARCHAR(255)*/,
  sessionIdentifier TEXT/*VARCHAR(255)*/,
  targetName        TEXT/*VARCHAR(255)*/,
  targetOid         UUID,
  targetOwnerName   TEXT/*VARCHAR(255)*/,
  targetOwnerOid    UUID,
  targetOwnerType   INTEGER,
  targetType        INTEGER,
  taskIdentifier    TEXT/*VARCHAR(255)*/,
  taskOID           TEXT/*VARCHAR(255)*/,
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
  name      TEXT/*VARCHAR(255)*/,
  record_id BIGINT,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_ref_value (
  id              BIGSERIAL NOT NULL,
  name            TEXT/*VARCHAR(255)*/,
  oid             UUID,
  record_id       BIGINT,
  targetName_norm TEXT/*VARCHAR(255)*/,
  targetName_orig TEXT/*VARCHAR(255)*/,
  type            TEXT/*VARCHAR(255)*/,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_resource (
  resourceOid       TEXT/*VARCHAR(255)*/ NOT NULL,
  record_id       BIGINT         NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);
CREATE TABLE m_case_wi (
  id                                INTEGER        NOT NULL,
  owner_oid                         UUID NOT NULL,
  closeTimestamp                    TIMESTAMPTZ,
  createTimestamp                   TIMESTAMPTZ,
  deadline                          TIMESTAMPTZ,
  originalAssigneeRef_relation      VARCHAR(157),
  originalAssigneeRef_targetOid     UUID,
  originalAssigneeRef_targetType    INTEGER,
  outcome                           TEXT/*VARCHAR(255)*/,
  performerRef_relation             VARCHAR(157),
  performerRef_targetOid            UUID,
  performerRef_targetType           INTEGER,
  stageNumber                       INTEGER,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_case_wi_reference (
  owner_id        INTEGER         NOT NULL,
  owner_owner_oid UUID  NOT NULL,
  reference_type  INTEGER         NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       UUID  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, targetOid, relation)
);
CREATE TABLE m_ext_item (
  id       SERIAL NOT NULL,
  kind     INTEGER,
  itemName VARCHAR(157),
  itemType VARCHAR(157),
  PRIMARY KEY (id)
);
CREATE TABLE m_object_ext_boolean (
  item_id      INTEGER        NOT NULL,
  owner_oid    UUID NOT NULL,
  ownerType    INTEGER        NOT NULL,
  booleanValue BOOLEAN     NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, booleanValue)
);
CREATE TABLE m_object_ext_date (
  item_id   INTEGER        NOT NULL,
  owner_oid UUID NOT NULL,
  ownerType INTEGER        NOT NULL,
  dateValue TIMESTAMPTZ   NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, dateValue)
);
CREATE TABLE m_object_ext_long (
  item_id   INTEGER        NOT NULL,
  owner_oid UUID NOT NULL,
  ownerType INTEGER        NOT NULL,
  longValue BIGINT        NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, longValue)
);
CREATE TABLE m_object_ext_poly (
  item_id   INTEGER         NOT NULL,
  owner_oid UUID  NOT NULL,
  ownerType INTEGER         NOT NULL,
  orig      TEXT/*VARCHAR(255)*/ NOT NULL,
  norm      TEXT/*VARCHAR(255)*/,
  PRIMARY KEY (owner_oid, ownerType, item_id, orig)
);
CREATE TABLE m_object_ext_reference (
  item_id    INTEGER        NOT NULL,
  owner_oid  UUID NOT NULL,
  ownerType  INTEGER        NOT NULL,
  targetoid  UUID NOT NULL,
  relation   VARCHAR(157),
  targetType INTEGER,
  PRIMARY KEY (owner_oid, ownerType, item_id, targetoid)
);
CREATE TABLE m_object_ext_string (
  item_id     INTEGER         NOT NULL,
  owner_oid   UUID  NOT NULL,
  ownerType   INTEGER         NOT NULL,
  stringValue TEXT/*VARCHAR(255)*/ NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, stringValue)
);
CREATE TABLE m_operation_execution (
  id                        INTEGER        NOT NULL,
  owner_oid                 UUID NOT NULL,
  initiatorRef_relation     VARCHAR(157),
  initiatorRef_targetOid    UUID,
  initiatorRef_targetType   INTEGER,
  status                    INTEGER,
  taskRef_relation          VARCHAR(157),
  taskRef_targetOid         UUID,
  taskRef_targetType        INTEGER,
  timestampValue            TIMESTAMPTZ,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_org_closure (
  ancestor_oid   UUID NOT NULL,
  descendant_oid UUID NOT NULL,
  val            INTEGER,
  PRIMARY KEY (ancestor_oid, descendant_oid)
);
CREATE TABLE m_user_organization (
  user_oid UUID NOT NULL,
  norm     TEXT/*VARCHAR(255)*/,
  orig     TEXT/*VARCHAR(255)*/
);
CREATE TABLE m_user_organizational_unit (
  user_oid UUID NOT NULL,
  norm     TEXT/*VARCHAR(255)*/,
  orig     TEXT/*VARCHAR(255)*/
);
CREATE TABLE m_form (
  name_norm TEXT/*VARCHAR(255)*/,
  name_orig TEXT/*VARCHAR(255)*/,
  oid       UUID NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_function_library (
  name_norm TEXT/*VARCHAR(255)*/,
  name_orig TEXT/*VARCHAR(255)*/,
  oid       UUID NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_generic_object (
  name_norm  TEXT/*VARCHAR(255)*/,
  name_orig  TEXT/*VARCHAR(255)*/,
  objectType TEXT/*VARCHAR(255)*/,
  oid        UUID NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_org (
  displayOrder INTEGER,
  name_norm    TEXT/*VARCHAR(255)*/,
  name_orig    TEXT/*VARCHAR(255)*/,
  tenant       BOOLEAN,
  oid          UUID NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_sequence (
  name_norm TEXT/*VARCHAR(255)*/,
  name_orig TEXT/*VARCHAR(255)*/,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE INDEX iAExtensionBoolean
  ON m_assignment_ext_boolean (booleanValue);
CREATE INDEX iAExtensionDate
  ON m_assignment_ext_date (dateValue);
CREATE INDEX iAExtensionLong
  ON m_assignment_ext_long (longValue);
CREATE INDEX iAExtensionPolyString
  ON m_assignment_ext_poly (orig);
CREATE INDEX iAExtensionReference
  ON m_assignment_ext_reference (targetoid);
CREATE INDEX iAExtensionString
  ON m_assignment_ext_string (stringValue);
CREATE INDEX iAssignmentReferenceTargetOid
  ON m_assignment_reference (targetOid);
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
CREATE INDEX iCaseWorkItemRefTargetOid
  ON m_case_wi_reference (targetOid);

ALTER TABLE m_ext_item
  ADD CONSTRAINT iExtItemDefinition UNIQUE (itemName, itemType, kind);
CREATE INDEX iObjectNameOrig
  ON m_object (name_orig);
CREATE INDEX iObjectNameNorm
  ON m_object (name_norm);
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
CREATE INDEX iOpExecTaskOid
  ON m_operation_execution (taskRef_targetOid);
CREATE INDEX iOpExecInitiatorOid
  ON m_operation_execution (initiatorRef_targetOid);
CREATE INDEX iOpExecStatus
  ON m_operation_execution (status);
CREATE INDEX iOpExecOwnerOid
  ON m_operation_execution (owner_oid);
CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);
CREATE INDEX iAncestor
  ON m_org_closure (ancestor_oid);
CREATE INDEX iDescendant
  ON m_org_closure (descendant_oid);
CREATE INDEX iDescendantAncestor
  ON m_org_closure (descendant_oid, ancestor_oid);
CREATE INDEX iAbstractRoleIdentifier
  ON m_abstract_role (identifier);
CREATE INDEX iRequestable
  ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled ON m_abstract_role(autoassign_enabled);
CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);
CREATE INDEX iFocusAdministrative
  ON m_focus (administrativeStatus);
CREATE INDEX iFocusEffective
  ON m_focus (effectiveStatus);
CREATE INDEX iLocality
  ON m_focus (locality_orig);
CREATE INDEX iFocusValidFrom
  ON m_focus (validFrom);
CREATE INDEX iFocusValidTo
  ON m_focus (validTo);
CREATE INDEX iFormNameOrig
  ON m_form (name_orig);
ALTER TABLE m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);
CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig);
ALTER TABLE m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);
CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig);
ALTER TABLE m_generic_object
  ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm);
CREATE INDEX iNodeNameOrig
  ON m_node (name_orig);
ALTER TABLE m_node
  ADD CONSTRAINT uc_node_name UNIQUE (name_norm);
CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig);
ALTER TABLE m_object_template
  ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm);
CREATE INDEX iDisplayOrder
  ON m_org (displayOrder);
CREATE INDEX iOrgNameOrig
  ON m_org (name_orig);
ALTER TABLE m_org
  ADD CONSTRAINT uc_org_name UNIQUE (name_norm);
CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig);
ALTER TABLE m_sequence
  ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);
CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig);
ALTER TABLE m_system_configuration
  ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm);
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;

-- These are created manually
ALTER TABLE m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

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
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (owner_oid) REFERENCES m_focus;
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_object_ext_long FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_object_ext_string FOREIGN KEY (owner_oid) REFERENCES m_object;

-- These are created manually
ALTER TABLE m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_long
  ADD CONSTRAINT fk_o_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE m_object_ext_string
  ADD CONSTRAINT fk_o_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

ALTER TABLE m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor_oid) REFERENCES m_object;
ALTER TABLE m_org_closure
  ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant_oid) REFERENCES m_object;
ALTER TABLE m_org_org_type
  ADD CONSTRAINT fk_org_org_type FOREIGN KEY (org_oid) REFERENCES m_org;
ALTER TABLE m_user_employee_type
  ADD CONSTRAINT fk_user_employee_type FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE m_node
  ADD CONSTRAINT fk_node FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_object_template
  ADD CONSTRAINT fk_object_template FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_org
  ADD CONSTRAINT fk_org FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE m_sequence
  ADD CONSTRAINT fk_sequence FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE m_system_configuration
  ADD CONSTRAINT fk_system_configuration FOREIGN KEY (oid) REFERENCES m_object;

-- Indices for foreign keys; maintained manually
CREATE INDEX iUserEmployeeTypeOid ON M_USER_EMPLOYEE_TYPE(USER_OID);
CREATE INDEX iUserOrganizationOid ON M_USER_ORGANIZATION(USER_OID);
CREATE INDEX iUserOrganizationalUnitOid ON M_USER_ORGANIZATIONAL_UNIT(USER_OID);
CREATE INDEX iAssignmentExtBooleanItemId ON M_ASSIGNMENT_EXT_BOOLEAN(ITEM_ID);
CREATE INDEX iAssignmentExtDateItemId ON M_ASSIGNMENT_EXT_DATE(ITEM_ID);
CREATE INDEX iAssignmentExtLongItemId ON M_ASSIGNMENT_EXT_LONG(ITEM_ID);
CREATE INDEX iAssignmentExtPolyItemId ON M_ASSIGNMENT_EXT_POLY(ITEM_ID);
CREATE INDEX iAssignmentExtReferenceItemId ON M_ASSIGNMENT_EXT_REFERENCE(ITEM_ID);
CREATE INDEX iAssignmentExtStringItemId ON M_ASSIGNMENT_EXT_STRING(ITEM_ID);
CREATE INDEX iObjectExtBooleanItemId ON M_OBJECT_EXT_BOOLEAN(ITEM_ID);
CREATE INDEX iObjectExtDateItemId ON M_OBJECT_EXT_DATE(ITEM_ID);
CREATE INDEX iObjectExtLongItemId ON M_OBJECT_EXT_LONG(ITEM_ID);
CREATE INDEX iObjectExtPolyItemId ON M_OBJECT_EXT_POLY(ITEM_ID);
CREATE INDEX iObjectExtReferenceItemId ON M_OBJECT_EXT_REFERENCE(ITEM_ID);
CREATE INDEX iObjectExtStringItemId ON M_OBJECT_EXT_STRING(ITEM_ID);
CREATE INDEX iOrgOrgTypeOid ON M_ORG_ORG_TYPE(ORG_OID);

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
CREATE TABLE m_global_metadata (
    name TEXT/*VARCHAR(255)*/ PRIMARY KEY,
    value TEXT/*VARCHAR(255)*/
);

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
