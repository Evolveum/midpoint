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
    oid UUID NOT NULL DEFAULT gen_random_uuid(),

    CONSTRAINT m_object_oid_pk PRIMARY KEY (oid)
);

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

-- Enumeration/code tables

-- Describes m_object.objectClassType
CREATE TABLE m_objtype (
    id INT PRIMARY KEY,
    name VARCHAR(64),
    table_name VARCHAR(64)
);

-- Based on RObjectType
INSERT INTO m_objtype VALUES (0, 'CONNECTOR', 'm_connector');
INSERT INTO m_objtype VALUES (1, 'CONNECTOR_HOST', 'm_connector_host');
INSERT INTO m_objtype VALUES (2, 'GENERIC_OBJECT', 'm_generic_object');
INSERT INTO m_objtype VALUES (3, 'OBJECT', 'm_object');
INSERT INTO m_objtype VALUES (4, 'VALUE_POLICY', 'm_value_policy');
INSERT INTO m_objtype VALUES (5, 'RESOURCE', 'm_resource');
INSERT INTO m_objtype VALUES (6, 'SHADOW', 'm_shadow');
INSERT INTO m_objtype VALUES (7, 'ROLE', 'm_role');
INSERT INTO m_objtype VALUES (8, 'SYSTEM_CONFIGURATION', 'm_system_configuration');
INSERT INTO m_objtype VALUES (9, 'TASK', 'm_task');
INSERT INTO m_objtype VALUES (10, 'USER', 'm_user');
INSERT INTO m_objtype VALUES (11, 'REPORT', 'm_report');
INSERT INTO m_objtype VALUES (12, 'REPORT_DATA', 'm_report_output');
INSERT INTO m_objtype VALUES (13, 'OBJECT_TEMPLATE', 'm_object_template');
INSERT INTO m_objtype VALUES (14, 'NODE', 'm_node');
INSERT INTO m_objtype VALUES (15, 'ORG', 'm_org');
INSERT INTO m_objtype VALUES (16, 'ABSTRACT_ROLE', 'm_abstract_role');
INSERT INTO m_objtype VALUES (17, 'FOCUS', 'm_focus');
INSERT INTO m_objtype VALUES (18, 'ASSIGNMENT_HOLDER', NULL);
INSERT INTO m_objtype VALUES (19, 'SECURITY_POLICY', 'm_security_policy');
INSERT INTO m_objtype VALUES (20, 'LOOKUP_TABLE', 'm_lookup_table');
INSERT INTO m_objtype VALUES (21, 'ACCESS_CERTIFICATION_DEFINITION', 'm_acc_cert_definition');
INSERT INTO m_objtype VALUES (22, 'ACCESS_CERTIFICATION_CAMPAIGN', 'm_acc_cert_campaign');
INSERT INTO m_objtype VALUES (23, 'SEQUENCE', 'm_sequence');
INSERT INTO m_objtype VALUES (24, 'SERVICE', 'm_service');
INSERT INTO m_objtype VALUES (25, 'FORM', 'm_form');
INSERT INTO m_objtype VALUES (26, 'CASE', 'm_case');
INSERT INTO m_objtype VALUES (27, 'FUNCTION_LIBRARY', 'm_function_library');
INSERT INTO m_objtype VALUES (28, 'OBJECT_COLLECTION', 'm_object_collection');
INSERT INTO m_objtype VALUES (29, 'ARCHETYPE', 'm_archetype');
INSERT INTO m_objtype VALUES (30, 'DASHBOARD', 'm_dashboard');

-- Describes m_reference.referenceType
CREATE TABLE m_reftype (
    id INT PRIMARY KEY,
    name VARCHAR(64)
);

-- Based on RReferenceType
INSERT INTO m_reftype VALUES (0, 'OBJECT_PARENT_ORG');
INSERT INTO m_reftype VALUES (1, 'USER_ACCOUNT');
INSERT INTO m_reftype VALUES (2, 'RESOURCE_BUSINESS_CONFIGURATION_APPROVER');
INSERT INTO m_reftype VALUES (3, '(DEPRECATED) ROLE_APPROVER');
INSERT INTO m_reftype VALUES (4, '(DEPRECATED) SYSTEM_CONFIGURATION_ORG_ROOT');
INSERT INTO m_reftype VALUES (5, 'CREATE_APPROVER');
INSERT INTO m_reftype VALUES (6, 'MODIFY_APPROVER');
INSERT INTO m_reftype VALUES (7, 'INCLUDE');
INSERT INTO m_reftype VALUES (8, 'ROLE_MEMBER');
INSERT INTO m_reftype VALUES (9, 'DELEGATED');
INSERT INTO m_reftype VALUES (10, 'PERSONA');
INSERT INTO m_reftype VALUES (11, 'ARCHETYPE');


-- Catalog of used Q-names.
-- Never update values of "uri" manually to change URI for some objects
-- (unless you really want to migrate old URI to a new one).
CREATE TABLE m_qname (
    id SERIAL,
--     alias VARCHAR(32), -- TODO how to fill it? Perhaps not needed here.
    uri VARCHAR(255) NOT NULL,

    PRIMARY KEY (id)
);

-- ALTER TABLE m_qname ADD CONSTRAINT m_qname_alias_key UNIQUE (alias); -- TODO see above
ALTER TABLE m_qname ADD CONSTRAINT m_qname_uri_key UNIQUE (uri);

-- Purely abstract table (no entries are allowed).
-- Following is recommended for each concrete table (see m_resource just below for example):
-- 1) override OID like this (PK+FK): oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
-- 2) define object type class (change value): objectTypeClass INTEGER GENERATED ALWAYS AS (5) STORED,
-- 3) add three triggers <table_name>_oid_{insert|update|delete}_tr as shown below
-- 4) add indexes for name_norm and name_orig columns (name_norm as unique)
-- 5) the rest varies on the concrete table, other indexes or constraints, etc.
-- 6) any required FK must be created on the concrete table, even for inherited columns
CREATE TABLE m_object (
    -- Default OID value is covered by INSERT triggers. No PK defined on abstract tables.
    oid UUID NOT NULL,
    -- objectTypeClass will be overridden with GENERATED value in concrete table
    objectTypeClass INTEGER NOT NULL DEFAULT 3, -- soft-references m_objtype
    name_norm VARCHAR(255) NOT NULL,
    name_orig VARCHAR(255) NOT NULL,
    fullObject BYTEA,
    creatorRef_targetOid UUID,
    creatorRef_targetType INTEGER, -- soft-references m_objtype
    creatorRef_relation_id INTEGER, -- soft-references m_qname,
    createChannel_id INTEGER, -- soft-references m_qname
    createTimestamp TIMESTAMPTZ,
    modifierRef_targetOid UUID,
    modifierRef_targetType INTEGER, -- soft-references m_objtype
    modifierRef_relation_id INTEGER, -- soft-references m_qname
    modifyChannel_id INTEGER, -- soft-references m_qname,
    modifyTimestamp TIMESTAMPTZ,
    tenantRef_targetOid UUID,
    tenantRef_targetType INTEGER, -- soft-references m_objtype
    tenantRef_relation_id INTEGER, -- soft-references m_qname,
    lifecycleState VARCHAR(255), -- TODO what is this? how many distinct values?
    version INTEGER NOT NULL DEFAULT 1,
    -- add GIN index for concrete tables where more than hundreds of entries are expected (see m_user)
    ext JSONB,

    -- these are purely DB-managed metadata, not mapped to in midPoint
    db_created TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    db_modified TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, -- updated in update trigger

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
);

-- TODO do we want to index channels and relations? what is the variability of these values?
--  In any case, indexes must be created for each sub-table (unless very small).

-- "concrete" table, allows insert and defines "final" objectTypeClass with GENERATED
CREATE TABLE m_resource (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (5) STORED,
    administrativeState INTEGER,
    connectorRef_targetOid UUID,
    connectorRef_targetType INTEGER, -- soft-references m_objtype
    connectorRef_relation_id INTEGER, -- soft-references m_qname
    o16_lastAvailabilityStatus INTEGER
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

-- extending m_object, but still abstract, hence DEFAULT for objectTypeClass and CHECK (false)
CREATE TABLE m_focus (
    -- will be overridden with GENERATED value in concrete table
    objectTypeClass INTEGER NOT NULL DEFAULT 17,
    administrativeStatus INTEGER,
    archiveTimestamp TIMESTAMPTZ,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus INTEGER,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus INTEGER,
    costCenter VARCHAR(255),
    emailAddress VARCHAR(255),
    photo BYTEA, -- will be TOAST-ed if necessary
    locale VARCHAR(255),
    locality_norm VARCHAR(255),
    locality_orig VARCHAR(255),
    preferredLanguage VARCHAR(255),
    telephoneNumber VARCHAR(255),
    timezone VARCHAR(255),
    passwordCreateTimestamp TIMESTAMPTZ,
    passwordModifyTimestamp TIMESTAMPTZ,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_object);

CREATE TABLE m_user (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (10) STORED,
    additionalName_norm VARCHAR(255),
    additionalName_orig VARCHAR(255),
    employeeNumber VARCHAR(255),
    familyName_norm VARCHAR(255),
    familyName_orig VARCHAR(255),
    fullName_norm VARCHAR(255),
    fullName_orig VARCHAR(255),
    givenName_norm VARCHAR(255),
    givenName_orig VARCHAR(255),
    honorificPrefix_norm VARCHAR(255),
    honorificPrefix_orig VARCHAR(255),
    honorificSuffix_norm VARCHAR(255),
    honorificSuffix_orig VARCHAR(255),
    nickName_norm VARCHAR(255),
    nickName_orig VARCHAR(255),
    title_norm VARCHAR(255),
    title_orig VARCHAR(255)
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
CREATE INDEX m_user_ext_idx ON m_user USING gin (ext);

CREATE TABLE m_shadow (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (6) STORED,
    objectClass VARCHAR(157) NOT NULL,
    resourceRef_targetOid UUID,
    resourceRef_targetType INTEGER, -- soft-references m_qname
    resourceRef_relation_id INTEGER, -- soft-references m_qname
    intent VARCHAR(255),
    kind INTEGER,
    attemptNumber INTEGER,
    dead BOOLEAN,
    exist BOOLEAN,
    failedOperationType INTEGER,
    fullSynchronizationTimestamp TIMESTAMPTZ,
    pendingOperationCount INTEGER,
    primaryIdentifierValue VARCHAR(255),
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
CREATE INDEX m_shadow_ext_idx ON m_shadow USING gin (ext);

-- TODO: if we never need mix of inducements and assignments then let's have two separate tables
-- consult with Rado/Katka/Palo
-- TODO: partitioning, not by object type, it's not even... hash-something?
-- select assignmentowner, count(*) From m_assignment group by assignmentowner;
--1	45 (inducements)
--0	48756229
CREATE TABLE m_assignment (
    id INTEGER NOT NULL, -- TODO serial? how if partitioned?
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    -- new column may avoid join to object for some queries
    owner_type INTEGER NOT NULL,
    administrativeStatus INTEGER,
    archiveTimestamp TIMESTAMPTZ,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus INTEGER,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus INTEGER,
    assignmentOwner INTEGER,
    createChannel VARCHAR(255),
    createTimestamp TIMESTAMPTZ,
    creatorRef_targetOid UUID,
    creatorRef_targetType INTEGER, -- soft-references m_objtype
    creatorRef_relation_id INTEGER, -- soft-references m_qname
    lifecycleState VARCHAR(255),
    modifierRef_targetOid UUID,
    modifierRef_targetType INTEGER, -- soft-references m_objtype
    modifierRef_relation_id INTEGER, -- soft-references m_qname
    modifyChannel VARCHAR(255),
    modifyTimestamp TIMESTAMPTZ,
    orderValue INTEGER,
    orgRef_targetOid UUID,
    orgRef_targetType INTEGER, -- soft-references m_objtype
    orgRef_relation_id INTEGER, -- soft-references m_qname
    resourceRef_targetOid UUID,
    resourceRef_targetType INTEGER, -- soft-references m_objtype
    resourceRef_relation_id INTEGER, -- soft-references m_qname
    targetRef_targetOid UUID,
    targetRef_targetType INTEGER, -- soft-references m_objtype
    targetRef_relation_id INTEGER, -- soft-references m_qname
    tenantRef_targetOid UUID,
    tenantRef_targetType INTEGER, -- soft-references m_objtype
    tenantRef_relation_id INTEGER, -- soft-references m_qname
    extId INTEGER, -- TODO what is this?
    extOid VARCHAR(36), -- is this UUID too?
    ext JSONB,

    CONSTRAINT m_assignment_pk PRIMARY KEY (owner_oid, id)
    -- no need to index owner_oid, it's part of the PK index
);

CREATE INDEX m_assignment_ext_idx ON m_assignment USING gin (ext);

CREATE TABLE m_acc_cert_campaign (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (22) STORED,
    definitionRef_targetOid UUID,
    definitionRef_targetType INTEGER, -- soft-references m_objtype
    definitionRef_relation_id INTEGER, -- soft-references m_qname
    endTimestamp TIMESTAMPTZ,
    handlerUri VARCHAR(255),
    iteration INTEGER NOT NULL,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    ownerRef_targetOid UUID,
    ownerRef_targetType INTEGER, -- soft-references m_objtype
    ownerRef_relation_id INTEGER, -- soft-references m_qname
    stageNumber INTEGER,
    startTimestamp TIMESTAMPTZ,
    state INTEGER
)
    INHERITS (m_object);

CREATE TRIGGER m_acc_cert_campaign_oid_insert_tr BEFORE INSERT ON m_acc_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_acc_cert_campaign_update_tr BEFORE UPDATE ON m_acc_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_acc_cert_campaign_oid_delete_tr AFTER DELETE ON m_acc_cert_campaign
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_acc_cert_campaign_name_orig_idx ON m_acc_cert_campaign (name_orig);
ALTER TABLE m_acc_cert_campaign ADD CONSTRAINT m_acc_cert_campaign_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_acc_cert_campaign_ext_idx ON m_acc_cert_campaign USING gin (ext);

CREATE TABLE m_acc_cert_case (
    id INTEGER NOT NULL,
    owner_oid VARCHAR(36) NOT NULL,
    administrativeStatus INTEGER,
    archiveTimestamp TIMESTAMPTZ,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus INTEGER,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus INTEGER,
    currentStageOutcome VARCHAR(255),
    fullObject BYTEA,
    iteration INTEGER NOT NULL,
    objectRef_targetOid UUID,
    objectRef_targetType INTEGER, -- soft-references m_objtype
    objectRef_relation_id INTEGER, -- soft-references m_qname
    orgRef_targetOid UUID,
    orgRef_targetType INTEGER, -- soft-references m_objtype
    orgRef_relation_id INTEGER, -- soft-references m_qname
    outcome VARCHAR(255),
    remediedTimestamp TIMESTAMPTZ,
    reviewDeadline TIMESTAMPTZ,
    reviewRequestedTimestamp TIMESTAMPTZ,
    stageNumber INTEGER,
    targetRef_targetOid UUID,
    targetRef_targetType INTEGER, -- soft-references m_objtype
    targetRef_relation_id INTEGER, -- soft-references m_qname
    tenantRef_targetOid UUID,
    tenantRef_targetType INTEGER, -- soft-references m_objtype
    tenantRef_relation_id INTEGER, -- soft-references m_qname

    PRIMARY KEY (owner_oid, id)
);

CREATE TABLE m_acc_cert_definition (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (21) STORED,
    handlerUri VARCHAR(255),
    lastCampaignClosedTimestamp TIMESTAMPTZ,
    lastCampaignStartedTimestamp TIMESTAMPTZ,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    ownerRef_targetOid UUID,
    ownerRef_targetType INTEGER, -- soft-references m_objtype
    ownerRef_relation_id INTEGER -- soft-references m_qname
)
    INHERITS (m_object);

CREATE TRIGGER m_acc_cert_definition_oid_insert_tr BEFORE INSERT ON m_acc_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_acc_cert_definition_update_tr BEFORE UPDATE ON m_acc_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_acc_cert_definition_oid_delete_tr AFTER DELETE ON m_acc_cert_definition
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_acc_cert_definition_name_orig_idx ON m_acc_cert_definition (name_orig);
ALTER TABLE m_acc_cert_definition ADD CONSTRAINT m_acc_cert_definition_name_norm_key UNIQUE (name_norm);
CREATE INDEX m_acc_cert_definition_ext_idx ON m_acc_cert_definition USING gin (ext);

CREATE TABLE m_acc_cert_wi (
    id INTEGER NOT NULL,
    owner_id INTEGER NOT NULL,
    owner_owner_oid UUID NOT NULL,
    closeTimestamp TIMESTAMPTZ,
    iteration INTEGER NOT NULL,
    outcome VARCHAR(255),
    outputChangeTimestamp TIMESTAMPTZ,
    performerRef_relation_id INTEGER, -- soft-references m_qname
    performerRef_targetOid UUID,
    performerRef_targetType INTEGER,
    stageNumber INTEGER,

    PRIMARY KEY (owner_owner_oid, owner_id, id)
);

CREATE TABLE m_acc_cert_wi_reference (
    owner_id INTEGER NOT NULL,
    owner_owner_id INTEGER NOT NULL,
    owner_owner_owner_oid UUID NOT NULL,
    targetOid UUID NOT NULL,
    targetType INTEGER, -- soft-references m_objtype
    relation_id INTEGER NOT NULL, -- soft-references m_qname

    PRIMARY KEY (owner_owner_owner_oid, owner_owner_id, owner_id, relation_id, targetOid)
);

CREATE TABLE m_node (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (14) STORED,
    nodeIdentifier VARCHAR(255)
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

CREATE TABLE m_system_configuration (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (8) STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_system_configuration_oid_insert_tr BEFORE INSERT ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_system_configuration_update_tr BEFORE UPDATE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_system_configuration_oid_delete_tr AFTER DELETE ON m_system_configuration
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- no need for the name index, m_system_configuration table is very small

CREATE TABLE m_security_policy (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (19) STORED
)
    INHERITS (m_object);

CREATE TRIGGER m_security_policy_oid_insert_tr BEFORE INSERT ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_security_policy_update_tr BEFORE UPDATE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE before_update_object();
CREATE TRIGGER m_security_policy_oid_delete_tr AFTER DELETE ON m_security_policy
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- no need for the name index, m_security_policy table is very small

CREATE TABLE m_object_collection (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (19) STORED
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

CREATE TABLE m_task (
    oid UUID NOT NULL PRIMARY KEY REFERENCES m_object_oid(oid),
    objectTypeClass INTEGER GENERATED ALWAYS AS (9) STORED,
    binding INTEGER,
    category VARCHAR(255),
    completionTimestamp TIMESTAMPTZ,
    executionStatus INTEGER,
    fullResult BYTEA,
    handlerUri VARCHAR(255), -- TODO q_name?
    lastRunFinishTimestamp TIMESTAMPTZ,
    lastRunStartTimestamp TIMESTAMPTZ,
    node VARCHAR(255), -- TODO why not FK?
    objectRef_targetOid UUID,
    objectRef_targetType INTEGER, -- soft-references m_objtype
    objectRef_relation_id INTEGER, -- soft-references m_qname
    ownerRef_targetOid UUID,
    ownerRef_targetType INTEGER, -- soft-references m_objtype
    ownerRef_relation_id INTEGER, -- soft-references m_qname
    parent VARCHAR(255), -- TODO why not FK?
    recurrence INTEGER,
    status INTEGER,
    taskIdentifier VARCHAR(255),
    threadStopAction INTEGER,
    waitingReason INTEGER
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
    task_oid  VARCHAR(36) NOT NULL,
    dependent VARCHAR(255)
);
ALTER TABLE IF EXISTS m_task_dependent
    ADD CONSTRAINT fk_task_dependent FOREIGN KEY (task_oid) REFERENCES m_task;
CREATE INDEX iTaskDependentOid ON M_TASK_DEPENDENT(TASK_OID);
*/

-- TODO: catalog unused at the moment
CREATE TABLE m_ext_item (
    id SERIAL NOT NULL,
    kind INTEGER, -- see RItemKind, is this necessary? does it contain cardinality?
    name VARCHAR(157), -- no path for nested props needed?
    type VARCHAR(32), -- data type
    storageType VARCHAR(32) NOT NULL default 'EXT_JSON', -- type of storage (JSON, column, table separate/common, etc.)
    storageInfo VARCHAR(32), -- optional storage detail, name of column or table if necessary

    PRIMARY KEY (id)
);

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
    orig VARCHAR(255) NOT NULL,
    norm VARCHAR(255),
    PRIMARY KEY (owner_oid, ext_item_id, orig)
);
CREATE TABLE m_object_ext_reference (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    target_oid UUID NOT NULL,
    relation_id INTEGER REFERENCES m_qname(id),
    targetType INTEGER,
    PRIMARY KEY (owner_oid, ext_item_id, target_oid)
);
CREATE TABLE m_object_ext_string (
    owner_oid UUID NOT NULL REFERENCES m_object_oid(oid),
    ext_item_id VARCHAR(32) NOT NULL,
    value VARCHAR(255) NOT NULL,
    PRIMARY KEY (owner_oid, ext_item_id, value)
);


-- TODO what of assignment extensions? Can they change for various types of assignments?
--  Then what? Inheritance is impractical, legacy extension tables are unwieldy.

-- TODO other indexes, only PKs/FKs are defined at the moment

/*
CREATE TABLE m_assignment_ext_boolean (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  booleanValue                 BOOLEAN     NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)
);
CREATE TABLE m_assignment_ext_date (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  dateValue                    TIMESTAMP   NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)
);
CREATE TABLE m_assignment_ext_long (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  longValue                    BIGINT        NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)
);
CREATE TABLE m_assignment_ext_poly (
  item_id                      INTEGER         NOT NULL,
  anyContainer_owner_id        INTEGER         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  norm                         VARCHAR(255),
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)
);
CREATE TABLE m_assignment_ext_reference (
  item_id                      INTEGER        NOT NULL,
  anyContainer_owner_id        INTEGER        NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36) NOT NULL,
  targetoid                    VARCHAR(36) NOT NULL,
  relation                     VARCHAR(157),
  targetType                   INTEGER,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)
);
CREATE TABLE m_assignment_ext_string (
  item_id                      INTEGER         NOT NULL,
  anyContainer_owner_id        INTEGER         NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  PRIMARY KEY (anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)
);
CREATE TABLE m_assignment_extension (
  owner_id        INTEGER        NOT NULL,
  owner_owner_oid VARCHAR(36) NOT NULL,
  PRIMARY KEY (owner_owner_oid, owner_id)
);
-- TODO HERE
CREATE TABLE m_assignment_policy_situation (
  assignment_id   INTEGER        NOT NULL,
  assignment_oid  VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
);
CREATE TABLE m_assignment_reference (
  owner_id        INTEGER         NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INTEGER         NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, relation, targetOid)
);
CREATE TABLE m_audit_delta (
  checksum          VARCHAR(32) NOT NULL,
  record_id         BIGINT        NOT NULL,
  delta             BYTEA,
  deltaOid          VARCHAR(36),
  deltaType         INTEGER,
  fullResult        BYTEA,
  objectName_norm   VARCHAR(255),
  objectName_orig   VARCHAR(255),
  resourceName_norm VARCHAR(255),
  resourceName_orig VARCHAR(255),
  resourceOid       VARCHAR(36),
  status            INTEGER,
  PRIMARY KEY (record_id, checksum)
);
CREATE TABLE m_audit_event (
  id                BIGSERIAL NOT NULL,
  attorneyName      VARCHAR(255),
  attorneyOid       VARCHAR(36),
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    VARCHAR(255),
  initiatorName     VARCHAR(255),
  initiatorOid      VARCHAR(36),
  initiatorType     INTEGER,
  message           VARCHAR(1024),
  nodeIdentifier    VARCHAR(255),
  outcome           INTEGER,
  parameter         VARCHAR(255),
  remoteHostAddress VARCHAR(255),
  requestIdentifier VARCHAR(255),
  result            VARCHAR(255),
  sessionIdentifier VARCHAR(255),
  targetName        VARCHAR(255),
  targetOid         VARCHAR(36),
  targetOwnerName   VARCHAR(255),
  targetOwnerOid    VARCHAR(36),
  targetOwnerType   INTEGER,
  targetType        INTEGER,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_item (
  changedItemPath VARCHAR(900) NOT NULL,
  record_id       BIGINT         NOT NULL,
  PRIMARY KEY (record_id, changedItemPath)
);
CREATE TABLE m_audit_prop_value (
  id        BIGSERIAL NOT NULL,
  name      VARCHAR(255),
  record_id BIGINT,
  value     VARCHAR(1024),
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_ref_value (
  id              BIGSERIAL NOT NULL,
  name            VARCHAR(255),
  oid             VARCHAR(36),
  record_id       BIGINT,
  targetName_norm VARCHAR(255),
  targetName_orig VARCHAR(255),
  type            VARCHAR(255),
  PRIMARY KEY (id)
);
CREATE TABLE m_audit_resource (
  resourceOid       VARCHAR(255) NOT NULL,
  record_id       BIGINT         NOT NULL,
  PRIMARY KEY (record_id, resourceOid)
);
CREATE TABLE m_case_wi (
  id                                INTEGER        NOT NULL,
  owner_oid                         VARCHAR(36) NOT NULL,
  closeTimestamp                    TIMESTAMP,
  createTimestamp                   TIMESTAMP,
  deadline                          TIMESTAMP,
  originalAssigneeRef_relation      VARCHAR(157),
  originalAssigneeRef_targetOid     VARCHAR(36),
  originalAssigneeRef_targetType    INTEGER,
  outcome                           VARCHAR(255),
  performerRef_relation             VARCHAR(157),
  performerRef_targetOid            VARCHAR(36),
  performerRef_targetType           INTEGER,
  stageNumber                       INTEGER,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_case_wi_reference (
  owner_id        INTEGER         NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  reference_type  INTEGER         NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  targetType      INTEGER,
  PRIMARY KEY (owner_owner_oid, owner_id, reference_type, targetOid, relation)
);
CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
);
CREATE TABLE m_ext_item (
  id       SERIAL NOT NULL,
  kind     INTEGER,
  itemName VARCHAR(157),
  itemType VARCHAR(157),
  PRIMARY KEY (id)
);
CREATE TABLE m_object_policy_situation (
  object_oid      VARCHAR(36) NOT NULL,
  policySituation VARCHAR(255)
);
CREATE TABLE m_object_ext_boolean (
  item_id      INTEGER        NOT NULL,
  owner_oid    VARCHAR(36) NOT NULL,
  ownerType    INTEGER        NOT NULL,
  booleanValue BOOLEAN     NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, booleanValue)
);
CREATE TABLE m_object_ext_date (
  item_id   INTEGER        NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INTEGER        NOT NULL,
  dateValue TIMESTAMP   NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, dateValue)
);
CREATE TABLE m_object_ext_long (
  item_id   INTEGER        NOT NULL,
  owner_oid VARCHAR(36) NOT NULL,
  ownerType INTEGER        NOT NULL,
  longValue BIGINT        NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, longValue)
);
CREATE TABLE m_object_ext_poly (
  item_id   INTEGER         NOT NULL,
  owner_oid VARCHAR(36)  NOT NULL,
  ownerType INTEGER         NOT NULL,
  orig      VARCHAR(255) NOT NULL,
  norm      VARCHAR(255),
  PRIMARY KEY (owner_oid, ownerType, item_id, orig)
);
CREATE TABLE m_object_ext_reference (
  item_id    INTEGER        NOT NULL,
  owner_oid  VARCHAR(36) NOT NULL,
  ownerType  INTEGER        NOT NULL,
  targetoid  VARCHAR(36) NOT NULL,
  relation   VARCHAR(157),
  targetType INTEGER,
  PRIMARY KEY (owner_oid, ownerType, item_id, targetoid)
);
CREATE TABLE m_object_ext_string (
  item_id     INTEGER         NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INTEGER         NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, ownerType, item_id, stringValue)
);
-- obsolete already, if we need it perhaps JSON[]?
CREATE TABLE m_object_subtype (
  object_oid VARCHAR(36) NOT NULL,
  subtype    VARCHAR(255)
);
CREATE TABLE m_object_text_info (
  owner_oid VARCHAR(36)  NOT NULL,
  text      VARCHAR(255) NOT NULL,
  PRIMARY KEY (owner_oid, text)
);
CREATE TABLE m_operation_execution (
  id                        INTEGER        NOT NULL,
  owner_oid                 VARCHAR(36) NOT NULL,
  initiatorRef_relation     VARCHAR(157),
  initiatorRef_targetOid    VARCHAR(36),
  initiatorRef_targetType   INTEGER,
  status                    INTEGER,
  taskRef_relation          VARCHAR(157),
  taskRef_targetOid         VARCHAR(36),
  taskRef_targetType        INTEGER,
  timestampValue            TIMESTAMP,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_org_closure (
  ancestor_oid   VARCHAR(36) NOT NULL,
  descendant_oid VARCHAR(36) NOT NULL,
  val            INTEGER,
  PRIMARY KEY (ancestor_oid, descendant_oid)
);
CREATE TABLE m_org_org_type (
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
);
-- TODO: split to tables per reference_type, see RReferenceType, currently 10 different tables
select reference_type, count(*) from m_reference group by reference_type order by 1;
--0	9712164 -- parent refs (orgs)
--1	36702345 -- accounts
--7	1 -- include, is it any good in DB?
--8	48756878 -- roles
--11 5 -- will grow to some fraction of role refs
CREATE TABLE m_reference (
  owner_oid      VARCHAR(36)  NOT NULL,
  reference_type INTEGER         NOT NULL,
  relation       VARCHAR(157) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  targetType     INTEGER,
  PRIMARY KEY (owner_oid, reference_type, relation, targetOid)
);
CREATE TABLE m_service_type (
  service_oid VARCHAR(36) NOT NULL,
  serviceType VARCHAR(255)
);
CREATE TABLE m_shadow (
  attemptNumber                INTEGER,
  dead                         BOOLEAN,
  exist                        BOOLEAN,
  failedOperationType          INTEGER,
  fullSynchronizationTimestamp TIMESTAMP,
  intent                       VARCHAR(255),
  kind                         INTEGER,
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  objectClass                  VARCHAR(157),
  pendingOperationCount        INTEGER,
  primaryIdentifierValue       VARCHAR(255),
  resourceRef_relation         VARCHAR(157),
  resourceRef_targetOid        VARCHAR(36),
  resourceRef_targetType       INTEGER,
  status                       INTEGER,
  synchronizationSituation     INTEGER,
  synchronizationTimestamp     TIMESTAMP,
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
);
CREATE TABLE m_user_organization (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);
CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
);
CREATE TABLE m_abstract_role (
  approvalProcess       VARCHAR(255),
  autoassign_enabled    BOOLEAN,
  displayName_norm      VARCHAR(255),
  displayName_orig      VARCHAR(255),
  identifier            VARCHAR(255),
  ownerRef_relation     VARCHAR(157),
  ownerRef_targetOid    VARCHAR(36),
  ownerRef_targetType   INTEGER,
  requestable           BOOLEAN,
  riskLevel             VARCHAR(255),
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_archetype (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_case (
  closeTimestamp            TIMESTAMP,
  name_norm                 VARCHAR(255),
  name_orig                 VARCHAR(255),
  objectRef_relation        VARCHAR(157),
  objectRef_targetOid       VARCHAR(36),
  objectRef_targetType      INTEGER,
  parentRef_relation        VARCHAR(157),
  parentRef_targetOid       VARCHAR(36),
  parentRef_targetType      INTEGER,
  requestorRef_relation     VARCHAR(157),
  requestorRef_targetOid    VARCHAR(36),
  requestorRef_targetType   INTEGER,
  state                     VARCHAR(255),
  targetRef_relation        VARCHAR(157),
  targetRef_targetOid       VARCHAR(36),
  targetRef_targetType      INTEGER,
  oid                       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector (
  connectorBundle               VARCHAR(255),
  connectorHostRef_relation     VARCHAR(157),
  connectorHostRef_targetOid    VARCHAR(36),
  connectorHostRef_targetType   INTEGER,
  connectorType                 VARCHAR(255),
  connectorVersion              VARCHAR(255),
  framework                     VARCHAR(255),
  name_norm                     VARCHAR(255),
  name_orig                     VARCHAR(255),
  oid                           VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_connector_host (
  hostname  VARCHAR(255),
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  port      VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_dashboard (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    oid       VARCHAR(36) NOT NULL,
    PRIMARY KEY (oid)
);
CREATE TABLE m_focus (
  administrativeStatus    INTEGER,
  archiveTimestamp        TIMESTAMP,
  disableReason           VARCHAR(255),
  disableTimestamp        TIMESTAMP,
  effectiveStatus         INTEGER,
  enableTimestamp         TIMESTAMP,
  validFrom               TIMESTAMP,
  validTo                 TIMESTAMP,
  validityChangeTimestamp TIMESTAMP,
  validityStatus          INTEGER,
  costCenter              VARCHAR(255),
  emailAddress            VARCHAR(255),
  hasPhoto                BOOLEAN DEFAULT FALSE NOT NULL,
  locale                  VARCHAR(255),
  locality_norm           VARCHAR(255),
  locality_orig           VARCHAR(255),
  preferredLanguage       VARCHAR(255),
  telephoneNumber         VARCHAR(255),
  timezone                VARCHAR(255),
  passwordCreateTimestamp TIMESTAMP,
  passwordModifyTimestamp TIMESTAMP,
  oid                     VARCHAR(36)           NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_form (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_function_library (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_global_metadata (
  name  VARCHAR(255) NOT NULL,
  value VARCHAR(255),
  PRIMARY KEY (name)
);
CREATE TABLE m_lookup_table (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_lookup_table_row (
  id                  INTEGER        NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  row_key             VARCHAR(255),
  label_norm          VARCHAR(255),
  label_orig          VARCHAR(255),
  lastChangeTimestamp TIMESTAMP,
  row_value           VARCHAR(255),
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INTEGER,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_org (
  displayOrder INTEGER,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  tenant       BOOLEAN,
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report (
  export              INTEGER,
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  orientation         INTEGER,
  parent              BOOLEAN,
  useHibernateSession BOOLEAN,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_report_output (
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  reportRef_relation    VARCHAR(157),
  reportRef_targetOid   VARCHAR(36),
  reportRef_targetType  INTEGER,
  oid                   VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_resource (
  administrativeState        INTEGER,
  connectorRef_relation      VARCHAR(157),
  connectorRef_targetOid     VARCHAR(36),
  connectorRef_targetType    INTEGER,
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  o16_lastAvailabilityStatus INTEGER,
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_sequence (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_service (
  displayOrder INTEGER,
  name_norm    VARCHAR(255),
  name_orig    VARCHAR(255),
  oid          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_trigger (
  id             INTEGER        NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue TIMESTAMP,
  PRIMARY KEY (owner_oid, id)
);
CREATE TABLE m_user (
  additionalName_norm  VARCHAR(255),
  additionalName_orig  VARCHAR(255),
  employeeNumber       VARCHAR(255),
  familyName_norm      VARCHAR(255),
  familyName_orig      VARCHAR(255),
  fullName_norm        VARCHAR(255),
  fullName_orig        VARCHAR(255),
  givenName_norm       VARCHAR(255),
  givenName_orig       VARCHAR(255),
  honorificPrefix_norm VARCHAR(255),
  honorificPrefix_orig VARCHAR(255),
  honorificSuffix_norm VARCHAR(255),
  honorificSuffix_orig VARCHAR(255),
  name_norm            VARCHAR(255),
  name_orig            VARCHAR(255),
  nickName_norm        VARCHAR(255),
  nickName_orig        VARCHAR(255),
  title_norm           VARCHAR(255),
  title_orig           VARCHAR(255),
  oid                  VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);
CREATE INDEX iCertCampaignNameOrig
  ON m_acc_cert_campaign (name_orig);
ALTER TABLE IF EXISTS m_acc_cert_campaign
  ADD CONSTRAINT uc_acc_cert_campaign_name UNIQUE (name_norm);
CREATE INDEX iCaseObjectRefTargetOid
  ON m_acc_cert_case (objectRef_targetOid);
CREATE INDEX iCaseTargetRefTargetOid
  ON m_acc_cert_case (targetRef_targetOid);
CREATE INDEX iCaseTenantRefTargetOid
  ON m_acc_cert_case (tenantRef_targetOid);
CREATE INDEX iCaseOrgRefTargetOid
  ON m_acc_cert_case (orgRef_targetOid);
CREATE INDEX iCertDefinitionNameOrig
  ON m_acc_cert_definition (name_orig);
ALTER TABLE IF EXISTS m_acc_cert_definition
  ADD CONSTRAINT uc_acc_cert_definition_name UNIQUE (name_norm);
CREATE INDEX iCertWorkItemRefTargetOid
  ON m_acc_cert_wi_reference (targetOid);
CREATE INDEX iAssignmentAdministrative
  ON m_assignment (administrativeStatus);
CREATE INDEX iAssignmentEffective
  ON m_assignment (effectiveStatus);
CREATE INDEX iAssignmentValidFrom
  ON m_assignment (validFrom);
CREATE INDEX iAssignmentValidTo
  ON m_assignment (validTo);
CREATE INDEX iTargetRefTargetOid
  ON m_assignment (targetRef_targetOid);
CREATE INDEX iTenantRefTargetOid
  ON m_assignment (tenantRef_targetOid);
CREATE INDEX iOrgRefTargetOid
  ON m_assignment (orgRef_targetOid);
CREATE INDEX iResourceRefTargetOid
  ON m_assignment (resourceRef_targetOid);
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

ALTER TABLE IF EXISTS m_ext_item
  ADD CONSTRAINT iExtItemDefinition UNIQUE (itemName, itemType, kind);
CREATE INDEX iObjectNameOrig
  ON m_object (name_orig);
CREATE INDEX iObjectNameNorm
  ON m_object (name_norm);
CREATE INDEX iObjectTypeClass
  ON m_object (objectTypeClass);
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
CREATE INDEX iReferenceTargetTypeRelation
  ON m_reference (targetOid, reference_type, relation);
CREATE INDEX iShadowResourceRef
  ON m_shadow (resourceRef_targetOid);
CREATE INDEX iShadowDead
  ON m_shadow (dead);
CREATE INDEX iShadowKind
  ON m_shadow (kind);
CREATE INDEX iShadowIntent
  ON m_shadow (intent);
CREATE INDEX iShadowObjectClass
  ON m_shadow (objectClass);
CREATE INDEX iShadowFailedOperationType
  ON m_shadow (failedOperationType);
CREATE INDEX iShadowSyncSituation
  ON m_shadow (synchronizationSituation);
CREATE INDEX iShadowPendingOperationCount
  ON m_shadow (pendingOperationCount);
CREATE INDEX iShadowNameOrig
  ON m_shadow (name_orig);
CREATE INDEX iShadowNameNorm
  ON m_shadow (name_norm);
ALTER TABLE IF EXISTS m_shadow
    ADD CONSTRAINT iPrimaryIdentifierValueWithOC UNIQUE (primaryIdentifierValue, objectClass, resourceRef_targetOid);
CREATE INDEX iAbstractRoleIdentifier
  ON m_abstract_role (identifier);
CREATE INDEX iRequestable
  ON m_abstract_role (requestable);
CREATE INDEX iAutoassignEnabled ON m_abstract_role(autoassign_enabled);
CREATE INDEX iArchetypeNameOrig ON m_archetype(name_orig);
CREATE INDEX iArchetypeNameNorm ON m_archetype(name_norm);
CREATE INDEX iCaseNameOrig
  ON m_case (name_orig);
CREATE INDEX iCaseTypeObjectRefTargetOid ON m_case(objectRef_targetOid);
CREATE INDEX iCaseTypeTargetRefTargetOid ON m_case(targetRef_targetOid);
CREATE INDEX iCaseTypeParentRefTargetOid ON m_case(parentRef_targetOid);
CREATE INDEX iCaseTypeRequestorRefTargetOid ON m_case(requestorRef_targetOid);
CREATE INDEX iCaseTypeCloseTimestamp ON m_case(closeTimestamp);
CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig);
CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm);
CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig);
ALTER TABLE IF EXISTS m_connector_host
  ADD CONSTRAINT uc_connector_host_name UNIQUE (name_norm);
CREATE INDEX iDashboardNameOrig ON m_dashboard(name_orig);
ALTER TABLE IF EXISTS m_dashboard
    ADD CONSTRAINT u_dashboard_name UNIQUE (name_norm);
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
ALTER TABLE IF EXISTS m_form
  ADD CONSTRAINT uc_form_name UNIQUE (name_norm);
CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig);
ALTER TABLE IF EXISTS m_function_library
  ADD CONSTRAINT uc_function_library_name UNIQUE (name_norm);
CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig);
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT uc_generic_object_name UNIQUE (name_norm);
CREATE INDEX iLookupTableNameOrig
  ON m_lookup_table (name_orig);
ALTER TABLE IF EXISTS m_lookup_table
  ADD CONSTRAINT uc_lookup_name UNIQUE (name_norm);
ALTER TABLE IF EXISTS m_lookup_table_row
  ADD CONSTRAINT uc_row_key UNIQUE (owner_oid, row_key);
CREATE INDEX iNodeNameOrig
  ON m_node (name_orig);
ALTER TABLE IF EXISTS m_node
  ADD CONSTRAINT uc_node_name UNIQUE (name_norm);
CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig);
ALTER TABLE IF EXISTS m_object_template
  ADD CONSTRAINT uc_object_template_name UNIQUE (name_norm);
CREATE INDEX iDisplayOrder
  ON m_org (displayOrder);
CREATE INDEX iOrgNameOrig
  ON m_org (name_orig);
ALTER TABLE IF EXISTS m_org
  ADD CONSTRAINT uc_org_name UNIQUE (name_norm);
CREATE INDEX iReportParent
  ON m_report (parent);
CREATE INDEX iReportNameOrig
  ON m_report (name_orig);
ALTER TABLE IF EXISTS m_report
  ADD CONSTRAINT uc_report_name UNIQUE (name_norm);
CREATE INDEX iReportOutputNameOrig
  ON m_report_output (name_orig);
CREATE INDEX iReportOutputNameNorm
  ON m_report_output (name_norm);
CREATE INDEX iResourceNameOrig
  ON m_resource (name_orig);
ALTER TABLE IF EXISTS m_resource
  ADD CONSTRAINT uc_resource_name UNIQUE (name_norm);
CREATE INDEX iRoleNameOrig
  ON m_role (name_orig);
ALTER TABLE IF EXISTS m_role
  ADD CONSTRAINT uc_role_name UNIQUE (name_norm);
CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig);
ALTER TABLE IF EXISTS m_sequence
  ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);
CREATE INDEX iServiceNameOrig
  ON m_service (name_orig);
CREATE INDEX iServiceNameNorm
  ON m_service (name_norm);
ALTER TABLE m_service
  ADD CONSTRAINT uc_service_name UNIQUE (name_norm);
CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig);
ALTER TABLE IF EXISTS m_system_configuration
  ADD CONSTRAINT uc_system_configuration_name UNIQUE (name_norm);
CREATE INDEX iTriggerTimestamp
  ON m_trigger (timestampValue);
CREATE INDEX iFullName
  ON m_user (fullName_orig);
CREATE INDEX iFamilyName
  ON m_user (familyName_orig);
CREATE INDEX iGivenName
  ON m_user (givenName_orig);
CREATE INDEX iEmployeeNumber
  ON m_user (employeeNumber);
CREATE INDEX iUserNameOrig
  ON m_user (name_orig);
ALTER TABLE IF EXISTS m_user
  ADD CONSTRAINT uc_user_name UNIQUE (name_norm);
CREATE INDEX iValuePolicyNameOrig
  ON m_value_policy (name_orig);
ALTER TABLE IF EXISTS m_value_policy
  ADD CONSTRAINT uc_value_policy_name UNIQUE (name_norm);
ALTER TABLE IF EXISTS m_acc_cert_campaign
  ADD CONSTRAINT fk_acc_cert_campaign FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_acc_cert_case
  ADD CONSTRAINT fk_acc_cert_case_owner FOREIGN KEY (owner_oid) REFERENCES m_acc_cert_campaign;
ALTER TABLE IF EXISTS m_acc_cert_definition
  ADD CONSTRAINT fk_acc_cert_definition FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_acc_cert_wi
  ADD CONSTRAINT fk_acc_cert_wi_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_acc_cert_case;
ALTER TABLE IF EXISTS m_acc_cert_wi_reference
  ADD CONSTRAINT fk_acc_cert_wi_ref_owner FOREIGN KEY (owner_owner_owner_oid, owner_owner_id, owner_id) REFERENCES m_acc_cert_wi;
ALTER TABLE IF EXISTS m_assignment
  ADD CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_owner FOREIGN KEY (anyContainer_owner_owner_oid, anyContainer_owner_id) REFERENCES m_assignment_extension;
ALTER TABLE IF EXISTS m_assignment_policy_situation
  ADD CONSTRAINT fk_assignment_policy_situation FOREIGN KEY (assignment_oid, assignment_id) REFERENCES m_assignment;
ALTER TABLE IF EXISTS m_assignment_reference
  ADD CONSTRAINT fk_assignment_reference FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_assignment;

-- These are created manually
ALTER TABLE IF EXISTS m_assignment_ext_boolean
  ADD CONSTRAINT fk_a_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_date
  ADD CONSTRAINT fk_a_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_long
  ADD CONSTRAINT fk_a_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_poly
  ADD CONSTRAINT fk_a_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_reference
  ADD CONSTRAINT fk_a_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_assignment_ext_string
  ADD CONSTRAINT fk_a_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

ALTER TABLE IF EXISTS m_audit_delta
  ADD CONSTRAINT fk_audit_delta FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_item
  ADD CONSTRAINT fk_audit_item FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_prop_value
  ADD CONSTRAINT fk_audit_prop_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_ref_value
  ADD CONSTRAINT fk_audit_ref_value FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_audit_resource
  ADD CONSTRAINT fk_audit_resource FOREIGN KEY (record_id) REFERENCES m_audit_event;
ALTER TABLE IF EXISTS m_case_wi
  ADD CONSTRAINT fk_case_wi_owner FOREIGN KEY (owner_oid) REFERENCES m_case;
ALTER TABLE IF EXISTS m_case_wi_reference
  ADD CONSTRAINT fk_case_wi_reference_owner FOREIGN KEY (owner_owner_oid, owner_id) REFERENCES m_case_wi;
ALTER TABLE IF EXISTS m_connector_target_system
  ADD CONSTRAINT fk_connector_target_system FOREIGN KEY (connector_oid) REFERENCES m_connector;
ALTER TABLE IF EXISTS m_focus_photo
  ADD CONSTRAINT fk_focus_photo FOREIGN KEY (owner_oid) REFERENCES m_focus;
ALTER TABLE m_object_policy_situation
  ADD CONSTRAINT fk_object_policy_situation FOREIGN KEY (object_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_long
  ADD CONSTRAINT fk_object_ext_long FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_ext_string
  ADD CONSTRAINT fk_object_ext_string FOREIGN KEY (owner_oid) REFERENCES m_object;

-- These are created manually
ALTER TABLE IF EXISTS m_object_ext_boolean
  ADD CONSTRAINT fk_o_ext_boolean_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_date
  ADD CONSTRAINT fk_o_ext_date_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_long
  ADD CONSTRAINT fk_o_ext_long_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_poly
  ADD CONSTRAINT fk_o_ext_poly_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_reference
  ADD CONSTRAINT fk_o_ext_reference_item FOREIGN KEY (item_id) REFERENCES m_ext_item;
ALTER TABLE IF EXISTS m_object_ext_string
  ADD CONSTRAINT fk_o_ext_string_item FOREIGN KEY (item_id) REFERENCES m_ext_item;

ALTER TABLE IF EXISTS m_object_subtype
  ADD CONSTRAINT fk_object_subtype FOREIGN KEY (object_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_text_info
  ADD CONSTRAINT fk_object_text_info_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_operation_execution
  ADD CONSTRAINT fk_op_exec_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_closure
  ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_closure
  ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org_org_type
  ADD CONSTRAINT fk_org_org_type FOREIGN KEY (org_oid) REFERENCES m_org;
ALTER TABLE IF EXISTS m_reference
  ADD CONSTRAINT fk_reference_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_service_type
  ADD CONSTRAINT fk_service_type FOREIGN KEY (service_oid) REFERENCES m_service;
ALTER TABLE IF EXISTS m_shadow
  ADD CONSTRAINT fk_shadow FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_user_employee_type
  ADD CONSTRAINT fk_user_employee_type FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_user_organization
  ADD CONSTRAINT fk_user_organization FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_user_organizational_unit
  ADD CONSTRAINT fk_user_org_unit FOREIGN KEY (user_oid) REFERENCES m_user;
ALTER TABLE IF EXISTS m_abstract_role
  ADD CONSTRAINT fk_abstract_role FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_archetype
  ADD CONSTRAINT fk_archetype FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_case
  ADD CONSTRAINT fk_case FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_connector
  ADD CONSTRAINT fk_connector FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_connector_host
  ADD CONSTRAINT fk_connector_host FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_dashboard
  ADD CONSTRAINT fk_dashboard FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_focus
  ADD CONSTRAINT fk_focus FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_form
  ADD CONSTRAINT fk_form FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_function_library
  ADD CONSTRAINT fk_function_library FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_generic_object
  ADD CONSTRAINT fk_generic_object FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_lookup_table
  ADD CONSTRAINT fk_lookup_table FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_lookup_table_row
  ADD CONSTRAINT fk_lookup_table_owner FOREIGN KEY (owner_oid) REFERENCES m_lookup_table;
ALTER TABLE IF EXISTS m_node
  ADD CONSTRAINT fk_node FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_object_template
  ADD CONSTRAINT fk_object_template FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_org
  ADD CONSTRAINT fk_org FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_report
  ADD CONSTRAINT fk_report FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_report_output
  ADD CONSTRAINT fk_report_output FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_resource
  ADD CONSTRAINT fk_resource FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_role
  ADD CONSTRAINT fk_role FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_sequence
  ADD CONSTRAINT fk_sequence FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_service
  ADD CONSTRAINT fk_service FOREIGN KEY (oid) REFERENCES m_abstract_role;
ALTER TABLE IF EXISTS m_system_configuration
  ADD CONSTRAINT fk_system_configuration FOREIGN KEY (oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_trigger
  ADD CONSTRAINT fk_trigger_owner FOREIGN KEY (owner_oid) REFERENCES m_object;
ALTER TABLE IF EXISTS m_user
  ADD CONSTRAINT fk_user FOREIGN KEY (oid) REFERENCES m_focus;
ALTER TABLE IF EXISTS m_value_policy
  ADD CONSTRAINT fk_value_policy FOREIGN KEY (oid) REFERENCES m_object;

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
CREATE INDEX iAssignmentPolicySituationId ON M_ASSIGNMENT_POLICY_SITUATION(ASSIGNMENT_OID, ASSIGNMENT_ID);
CREATE INDEX iConnectorTargetSystemOid ON M_CONNECTOR_TARGET_SYSTEM(CONNECTOR_OID);
CREATE INDEX iObjectPolicySituationOid ON M_OBJECT_POLICY_SITUATION(OBJECT_OID);
CREATE INDEX iObjectExtBooleanItemId ON M_OBJECT_EXT_BOOLEAN(ITEM_ID);
CREATE INDEX iObjectExtDateItemId ON M_OBJECT_EXT_DATE(ITEM_ID);
CREATE INDEX iObjectExtLongItemId ON M_OBJECT_EXT_LONG(ITEM_ID);
CREATE INDEX iObjectExtPolyItemId ON M_OBJECT_EXT_POLY(ITEM_ID);
CREATE INDEX iObjectExtReferenceItemId ON M_OBJECT_EXT_REFERENCE(ITEM_ID);
CREATE INDEX iObjectExtStringItemId ON M_OBJECT_EXT_STRING(ITEM_ID);
CREATE INDEX iObjectSubtypeOid ON M_OBJECT_SUBTYPE(OBJECT_OID);
CREATE INDEX iOrgOrgTypeOid ON M_ORG_ORG_TYPE(ORG_OID);
CREATE INDEX iServiceTypeOid ON M_SERVICE_TYPE(SERVICE_OID);

INSERT INTO m_global_metadata VALUES ('databaseSchemaVersion', '4.2');

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

commit;

 */