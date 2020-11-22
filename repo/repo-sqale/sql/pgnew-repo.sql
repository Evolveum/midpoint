-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- Naming conventions:
-- M_ prefix is used for tables in main part of the repo, MA_ for audit tables (can be separate)
-- Constraints/indexes use table_column(s)_suffix convention, with PK for primary key,
-- FK foreign key, IDX for index, KEY for unique index.
-- TR is suffix for triggers.
-- Names are generally lowercase (despite prefix/suffixes above in uppercase ;-)).

-- just in case PUBLIC schema was dropped (fastest way to remove all midpoint objects)
CREATE SCHEMA IF NOT EXISTS public;

-- To support gen_random_uuid() pgcrypto extension must be enabled for the database (not for PG 13).
-- select * from pg_available_extensions order by name;
DO
$$
    BEGIN
        perform pg_get_functiondef('gen_random_uuid()'::regprocedure);
        raise notice 'gen_random_uuid already exists, skipping create EXTENSION pgcrypto';
    EXCEPTION WHEN undefined_function THEN
        create EXTENSION pgcrypto;
    END;
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
-- Checks that OID is not changed.
CREATE OR REPLACE FUNCTION update_object_oid()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.oid = OLD.oid THEN
        -- must return NEW, NULL would skip the update
        RETURN NEW;
    END IF;

    -- OID changed, forbidden
    RAISE EXCEPTION 'UPDATE on "%" tried to change OID "%" to "%". OID is immutable and cannot be changed.',
        TG_TABLE_NAME, OLD.oid, NEW.oid;
END
$$;

CREATE TABLE m_object (
    -- default value is covered by INSERT triggers
    oid UUID NOT NULL,
    -- objectTypeClass will be overridden with GENERATED value in concrete table
    objectTypeClass INT4 DEFAULT 3,
    name_norm VARCHAR(255) NOT NULL,
    name_orig VARCHAR(255) NOT NULL,
    fullObject BYTEA,
    createChannel VARCHAR(255),
    createTimestamp TIMESTAMP,
    creatorRef_relation VARCHAR(157),
    creatorRef_targetOid VARCHAR(36),
    creatorRef_targetType INT4,
    lifecycleState VARCHAR(255),
    modifierRef_relation VARCHAR(157),
    modifierRef_targetOid VARCHAR(36),
    modifierRef_targetType INT4,
    modifyChannel VARCHAR(255),
    modifyTimestamp TIMESTAMP,
    tenantRef_relation VARCHAR(157),
    tenantRef_targetOid VARCHAR(36),
    tenantRef_targetType INT4,
    version INT4 NOT NULL,

    -- prevents inserts to this table, but not to inherited ones; this makes it "abstract" table
    CHECK (FALSE) NO INHERIT
    -- FK like this must be defined on concrete sub-tables (useless for "abstract" tables):
    -- CONSTRAINT m_object_oid_fk FOREIGN KEY (oid) REFERENCES m_object_oid(oid)
    -- No PK defined here, does not make semantic sense, also prevents accidental FK to this table.
    -- FK should reference m_object_oid table instead, where OID is globally unique.
);

-- "concrete" table, allows insert and defines "final" objectTypeClass with GENERATED
CREATE TABLE m_resource (
    objectTypeClass INT4 GENERATED ALWAYS AS (5) STORED,
    administrativeState INT4,
    connectorRef_relation VARCHAR(157),
    connectorRef_targetOid VARCHAR(36),
    connectorRef_targetType INT4,
    o16_lastAvailabilityStatus INT4,

    CONSTRAINT m_resource_pk PRIMARY KEY (oid),
    CONSTRAINT m_resource_oid_fk FOREIGN KEY (oid) REFERENCES m_object_oid(oid)
)
    INHERITS (m_object);

CREATE TRIGGER m_resource_oid_insert_tr BEFORE INSERT ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_resource_oid_update_tr BEFORE UPDATE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE update_object_oid();
CREATE TRIGGER m_resource_oid_delete_tr AFTER DELETE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

-- extending m_object, but still abstract, hence DEFAULT for objectTypeClass and CHECK (false)
CREATE TABLE m_focus (
    -- will be overridden with GENERATED value in concrete table
    objectTypeClass INT4 DEFAULT 17,
    administrativeStatus INT4,
    archiveTimestamp TIMESTAMP,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMP,
    effectiveStatus INT4,
    enableTimestamp TIMESTAMP,
    validFrom TIMESTAMP,
    validTo TIMESTAMP,
    validityChangeTimestamp TIMESTAMP,
    validityStatus INT4,
    costCenter VARCHAR(255),
    emailAddress VARCHAR(255),
    hasPhoto BOOLEAN DEFAULT FALSE NOT NULL,
    locale VARCHAR(255),
    locality_norm VARCHAR(255),
    locality_orig VARCHAR(255),
    preferredLanguage VARCHAR(255),
    telephoneNumber VARCHAR(255),
    timezone VARCHAR(255),
    passwordCreateTimestamp TIMESTAMP,
    passwordModifyTimestamp TIMESTAMP,

    CHECK (FALSE) NO INHERIT
)
    INHERITS (m_object);

CREATE TABLE m_user (
    objectTypeClass INT4 GENERATED ALWAYS AS (10) STORED,
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
    title_orig VARCHAR(255),

    CONSTRAINT m_user_pk PRIMARY KEY (oid),
    CONSTRAINT m_user_oid_fk FOREIGN KEY (oid) REFERENCES m_object_oid(oid)
)
    INHERITS (m_focus);

CREATE TRIGGER m_user_oid_insert_tr BEFORE INSERT ON m_user
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_user_oid_update_tr BEFORE UPDATE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE update_object_oid();
CREATE TRIGGER m_user_oid_delete_tr AFTER DELETE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE INDEX m_user_name_orig_idx ON m_user (name_orig);
ALTER TABLE m_user ADD CONSTRAINT m_user_name_norm_key UNIQUE (name_norm);

CREATE TABLE m_shadow (
    objectTypeClass INT4 GENERATED ALWAYS AS (6) STORED,
    objectClass VARCHAR(157) NOT NULL,
    resourceRef_targetOid VARCHAR(36),
    resourceRef_targetType INT4,
    resourceRef_relation VARCHAR(157),
    intent VARCHAR(255),
    kind INT4,
    attemptNumber INT4,
    dead BOOLEAN,
    exist BOOLEAN,
    failedOperationType INT4,
    fullSynchronizationTimestamp TIMESTAMP,
    pendingOperationCount INT4,
    primaryIdentifierValue VARCHAR(255),
    status INT4,
    synchronizationSituation INT4,
    synchronizationTimestamp TIMESTAMP,

    CONSTRAINT m_shadow_pk PRIMARY KEY (oid),
    CONSTRAINT m_shadow_oid_fk FOREIGN KEY (oid) REFERENCES m_object_oid(oid)
)
    INHERITS (m_object);

CREATE TRIGGER m_shadow_oid_insert_tr BEFORE INSERT ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE insert_object_oid();
CREATE TRIGGER m_shadow_oid_update_tr BEFORE UPDATE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE update_object_oid();
CREATE TRIGGER m_shadow_oid_delete_tr AFTER DELETE ON m_shadow
    FOR EACH ROW EXECUTE PROCEDURE delete_object_oid();

CREATE TABLE m_assignment (
    id INT4 NOT NULL,
    owner_oid UUID NOT NULL,
    administrativeStatus INT4,
    archiveTimestamp TIMESTAMP,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMP,
    effectiveStatus INT4,
    enableTimestamp TIMESTAMP,
    validFrom TIMESTAMP,
    validTo TIMESTAMP,
    validityChangeTimestamp TIMESTAMP,
    validityStatus INT4,
    assignmentOwner INT4,
    createChannel VARCHAR(255),
    createTimestamp TIMESTAMP,
    creatorRef_relation VARCHAR(157),
    creatorRef_targetOid VARCHAR(36),
    creatorRef_targetType INT4,
    lifecycleState VARCHAR(255),
    modifierRef_relation VARCHAR(157),
    modifierRef_targetOid VARCHAR(36),
    modifierRef_targetType INT4,
    modifyChannel VARCHAR(255),
    modifyTimestamp TIMESTAMP,
    orderValue INT4,
    orgRef_relation VARCHAR(157),
    orgRef_targetOid VARCHAR(36),
    orgRef_targetType INT4,
    resourceRef_relation VARCHAR(157),
    resourceRef_targetOid VARCHAR(36),
    resourceRef_targetType INT4,
    targetRef_relation VARCHAR(157),
    targetRef_targetOid VARCHAR(36),
    targetRef_targetType INT4,
    tenantRef_relation VARCHAR(157),
    tenantRef_targetOid VARCHAR(36),
    tenantRef_targetType INT4,
    extId INT4,
    extOid VARCHAR(36),

    CONSTRAINT m_assignment_pk PRIMARY KEY (owner_oid, id),
    -- no need to index owner_oid, it's part of the PK index
    CONSTRAINT m_assignment_owner_oid_fk FOREIGN KEY (owner_oid) REFERENCES m_object_oid(oid)
);

-- TODO other indexes, only PKs/FKs are defined at the moment
