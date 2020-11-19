-- @formatter:off because of terribly unreliable IDEA reformat for SQL

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

CREATE TABLE m_object (
    oid UUID NOT NULL DEFAULT gen_random_uuid(),
-- will be overridden with GENERATED value in concrete table
    objectTypeClass INT4 DEFAULT 3,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
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

-- prevents inserts to this table, but not to inherited ones
    CHECK (FALSE) NO INHERIT
);

-- See: http://blog.ioguix.net/postgresql/2015/02/05/Partitionning-and-constraints-part-1.html
-- This checks that provided OID is unique across all tables inherited from m_object.
-- Docs for trigger functions and triggers: https://www.postgresql.org/docs/13/plpgsql-trigger.html
CREATE OR REPLACE FUNCTION object_oid_pk()
    RETURNS trigger
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- If update doesn't change OID, we don't want the expansive check.
    -- This can be also done with WHEN on CREATE TRIGGER, but only when INSERT and UPDATE
    -- triggers are set separately.
    IF NEW.oid = OLD.oid THEN
        -- returned VALUE ignored for AFTER trigger
        RETURN NULL;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtext(NEW.oid::text));

    IF count(1) > 1 FROM m_object WHERE oid = NEW.oid THEN
    RAISE EXCEPTION 'duplicate m_object.OID value "%" while inserting into "%"',
        NEW.oid, TG_TABLE_NAME;
END IF;

    RETURN NULL;
END
$function$;

-- "concrete" table, allows insert and defines "final" objectTypeClass with GENERATED
CREATE TABLE m_resource (
    objectTypeClass INT4 GENERATED ALWAYS AS (5) STORED,
    administrativeState INT4,
    connectorRef_relation VARCHAR(157),
    connectorRef_targetOid VARCHAR(36),
    connectorRef_targetType INT4,
    o16_lastAvailabilityStatus INT4,

    CONSTRAINT m_resource_pk PRIMARY KEY (oid)
)
    INHERITS (m_object);

-- See: http://blog.ioguix.net/postgresql/2015/02/05/Partitionning-and-constraints-part-1.html
-- It discusses CONSTRAINT TRIGGER and various DEFERRABLE options and the impact.
CREATE CONSTRAINT TRIGGER m_resource_oid_check_tr AFTER INSERT OR UPDATE ON m_resource
    DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE object_oid_pk();

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

    CONSTRAINT m_user_pk PRIMARY KEY (oid)
)
    INHERITS (m_focus);

CREATE CONSTRAINT TRIGGER m_user_oid_check_tr AFTER INSERT OR UPDATE ON m_user
    DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE object_oid_pk();

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

    CONSTRAINT m_shadow_pk PRIMARY KEY (oid)
)
    INHERITS (m_object);

-- Shadows with partitions
CREATE CONSTRAINT TRIGGER m_shadow_oid_check_tr AFTER INSERT OR UPDATE ON m_shadow
    DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE object_oid_pk();

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

    CONSTRAINT m_assignment_pk PRIMARY KEY (owner_oid, id)
);

-- This can't be done without separate table where OID is truly unique
ALTER TABLE IF EXISTS m_assignment
    ADD CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_oid) REFERENCES m_object(oid);

-- TODO other indexes, only PK is defined at the moment
