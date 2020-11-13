-- @formatter:off because of terribly unreliable IDEA reformat for SQL

-- TODO: unique oid across tables:
--  http://blog.ioguix.net/postgresql/2015/02/05/Partitionning-and-constraints-part-1.html
--  or having dedicated centralized unrelated m_object_oid table?

-- To support gen_random_uuid() pgcrypto extension must be enabled for the database.
-- select * from pg_available_extensions order by name;
-- create EXTENSION pgcrypto;

drop table m_user;
drop table m_focus;
drop table m_resource;
drop table m_object;

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

-- "concrete" table, allows insert and defines "final" objectTypeClass with GENERATED
CREATE TABLE m_resource (
    objectTypeClass INT4 GENERATED ALWAYS AS (5) STORED,
    administrativeState INT4,
    connectorRef_relation VARCHAR(157),
    connectorRef_targetOid VARCHAR(36),
    connectorRef_targetType INT4,
    o16_lastAvailabilityStatus INT4,

    CONSTRAINT m_resource_pk PRIMARY KEY (OID)
)
    INHERITS (m_object);

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
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    nickName_norm VARCHAR(255),
    nickName_orig VARCHAR(255),
    title_norm VARCHAR(255),
    title_orig VARCHAR(255),

    CONSTRAINT m_user_pk PRIMARY KEY (OID)
)
    INHERITS (m_focus);

-- See: http://blog.ioguix.net/postgresql/2015/02/05/Partitionning-and-constraints-part-1.html
-- this checks provided OID, we will probably want unique generated OID
CREATE OR REPLACE FUNCTION object_oid_pk()
    RETURNS trigger
    LANGUAGE plpgsql
AS $function$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext(NEW.oid::text));

    IF count(1) > 1 FROM m_object WHERE oid = NEW.oid THEN
        RAISE EXCEPTION 'duplicate m_object.OID value "%" while inserting into "%"',
            NEW.oid, TG_TABLE_NAME;
    END IF;

    RETURN NULL;
END
$function$;

DROP TRIGGER m_resource_oid_check_tr ON m_resource;
DROP TRIGGER m_user_oid_check_tr ON m_user;
CREATE TRIGGER m_resource_oid_check_tr AFTER INSERT OR UPDATE ON m_resource
    FOR EACH ROW EXECUTE PROCEDURE object_oid_pk();
CREATE TRIGGER m_user_oid_check_tr AFTER INSERT OR UPDATE ON m_user
    FOR EACH ROW EXECUTE PROCEDURE object_oid_pk();


-- one user with random name
INSERT INTO m_user (oid, name_orig, version)
VALUES (gen_random_uuid(), md5(random()::TEXT), 1);

insert into m_resource (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'resource1', 1);
insert into m_user (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'conflict', 1);

SELECT count(*)
-- SELECT *
FROM m_focus
;

DO
$$
    BEGIN
        FOR r IN 1..100000
            LOOP
                INSERT INTO m_user (oid, name_orig, version)
                VALUES (gen_random_uuid(), 'user-' || LPAD(r::text, 7, '0'), 1);

                -- regular commit to avoid running out of memory with locks
                IF r % 1000 = 0 THEN
                    COMMIT;
                END IF;
            END LOOP;
    END;
$$;

EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT *
FROM m_object
where oid='50c7746e-3c6d-4e24-8d7b-b18043c6c7bb'
;

select hashtext(gen_random_uuid()::text);

select * from pg_trigger