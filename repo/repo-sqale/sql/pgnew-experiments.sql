-- @formatter:off because of terribly unreliable IDEA reformat for SQL

-- various internal PG selects
SELECT version();
select * from pg_tables where tableowner='midpoint' order by tablename ;
select * from pg_tables where schemaname='public' order by tablename ;
select * from pg_trigger order by tgname;
select * from pg_available_extensions order by name;

-- DB clean: drop schema does it all with one command
-- drop schema public CASCADE;
-- drop table m_object;
-- DROP TRIGGER m_resource_oid_insert_tr ON m_resource;

-- DB data initialization (after pgnew-repo.sql)
-- one user with random name
INSERT INTO m_user (oid, name_norm, name_orig, version)
VALUES (gen_random_uuid(), md5(random()::TEXT), md5(random()::TEXT), 1);

select * from m_resource;
-- creates new row with generated UUID, repeated run must fail on unique name_norm
insert into m_resource (name_norm, name_orig, version) VALUES ('resource0', 'resource0', 1) RETURNING OID;
-- should fail the second time because oid is PK of the table (even with changed name_norm)
insert into m_resource (oid, name_norm, name_orig, version)
    VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'resource1', 'resource1', 1);
-- this should fail after previous due to cross-table m_object unique constraint
insert into m_user (oid, name_norm, name_orig, version)
    VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'conflict', 'conflict', 1);
-- must fail, update trigger does not allow OID changes
update m_object set oid='66eb4861-867d-4a41-b6f0-41a3874bd48e'
    where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';

SELECT * from m_object;
SELECT * from m_object_oid where oid not in (SELECT oid FROM m_object);

-- inner transaction should fail due to cross-table m_object unique constraint
delete from m_object where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';
-- switch Tx to manual in IDE to avoid autocommit
START TRANSACTION;
insert into m_resource (oid, name_norm, name_orig, version)
    VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'resource1', 'resource1', 1);

    START TRANSACTION;
    insert into m_user (oid, name_norm, name_orig, version)
        VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'conflict', 'conflict', 1);
    commit;
commit;

-- switch Tx back to Auto if desired - only resource1 should be inserted
select * from m_object where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';

-- Delete in two steps without trigger, much faster than normal.
SET session_replication_role = replica; -- disables triggers for the current session
-- HERE the delete you want, e.g.:
delete from m_user where name_norm > 'user-0001000000';

-- this is the cleanup of unused OIDs
DELETE FROM m_object_oid oo WHERE NOT EXISTS (SELECT * from m_object o WHERE o.oid = oo.oid);
SET session_replication_role = default; -- re-enables normal operation (triggers)
SHOW session_replication_role;

-- TODO measure impact of UPDATE trigger on the performance

-- adding x users
-- 100_000 inserts: 3 inherited tables ~6s, for 25 inherited tables ~13s, for 50 ~20s, for 100 ~34s
-- change with volume (100 inherited tables): 200k previous rows ~34s, with 1m rows ~37s
-- with 3 inherited tables and 5M existing rows, adding 100k rows takes ~7s
-- delete from m_object;
-- delete from m_object_oid;
select count(*) from m_object_oid;
explain
select count(*) from m_user;
-- vacuum full analyze; -- this requires exclusive lock on processed table and can be very slow, with 1M rows it takes 10s
vacuum analyze; -- this is normal operation version (can run in parallel, ~25s/25m rows)

-- 100k takes 6s, whether we commit after each 1000 or not
-- This answer also documents that LOOP is 2x slower than generate_series: https://stackoverflow.com/a/53242452/658826
DO $$ BEGIN
    FOR r IN 1000001..1100000 LOOP
        INSERT INTO m_user (name_norm, name_orig, version)
        VALUES ('user-' || LPAD(r::text, 10, '0'), 'user-' || LPAD(r::text, 10, '0'), 1);
--      INSERT INTO m_user (oid, name_orig, version)
--      VALUES (gen_random_uuid(), 'user-' || LPAD(r::text, 10, '0'), 1);

        -- regular commit to keep transactions reasonable (negligible performance impact)
        IF r % 1000 = 0 THEN
            COMMIT;
        END IF;
    END LOOP;
END; $$;

-- 100k takes 4s, gets slower with volume, of course
INSERT INTO m_user (name_norm, name_orig, version)
    SELECT 'user-' || LPAD(n::text, 10, '0'), 'user-' || LPAD(n::text, 10, '0'), 1
    FROM generate_series(38000001, 40000000) AS n;

-- MUST fail on OID constraint if existing OID is used in SET:
update m_object
set oid='66eb4861-867d-4a41-b6f0-41a3874bd48f'
where oid='f7a0362f-37a5-4dea-ac16-9c84dce333dc';

select * from m_user;

select * from m_object
where oid<'812c5e7a-8a94-4bd1-944d-389e7294b831'
order by oid;

-- EXPLAIN selects
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT *
FROM m_object
where oid='cf72947b-f7b5-4b44-a2b1-07452b9056cc'
;

EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT count(*)
-- SELECT *
FROM m_focus
;

--------------
-- sandbox

select ctid, * from m_object
;

select count(*) from pg_inherits
;

-- creating more tables inherited from m_object or m_focus
DO
$$
    BEGIN
        FOR r IN 51..75
            LOOP
                EXECUTE 'CREATE TABLE m_omore' || r || '(
                    objectTypeClass INT4 GENERATED ALWAYS AS (101) STORED,
                    PRIMARY KEY (oid)
                )
                    INHERITS (m_object)';
                EXECUTE 'CREATE TABLE m_fmore' || r || '(
                    objectTypeClass INT4 GENERATED ALWAYS AS (101) STORED,
                    PRIMARY KEY (oid)
                )
                    INHERITS (m_focus)';
            END LOOP;
    END;
$$;

DO
$$
    BEGIN
        FOR r IN 1..47
            LOOP
                EXECUTE 'DROP TABLE m_fmore' || r ;
                EXECUTE 'DROP TABLE m_omore' || r ;
            END LOOP;
    END
$$;

-- MANAGEMENT queries

-- See: https://wiki.postgresql.org/wiki/Disk_Usage

-- biggest relations
SELECT nspname || '.' || relname AS "relation",
    pg_size_pretty(pg_relation_size(C.oid)) AS "size"
FROM pg_class C
    LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
WHERE nspname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_relation_size(C.oid) DESC
LIMIT 20;

vacuum full analyze;
-- database size
SELECT pg_size_pretty( pg_database_size('midpoint') );
