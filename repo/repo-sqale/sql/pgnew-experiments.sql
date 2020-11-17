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
-- DROP TRIGGER m_resource_oid_check_tr ON m_resource;

-- DB data initialization (after pgnew-repo.sql)
-- one user with random name
INSERT INTO m_user (oid, name_orig, version)
VALUES (gen_random_uuid(), md5(random()::TEXT), 1);

-- should fail the second time because oid is PK of the table
insert into m_resource (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'resource1', 1);
-- this should fail after previous due to cross-table m_object unique constraint
insert into m_user (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'conflict', 1);

-- inner transaction should fail due to cross-table m_object unique constraint
delete from m_object where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';
-- switch Tx to manual in IDE to avoid autocommit
START TRANSACTION;
insert into m_resource (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'resource1', 1);

    START TRANSACTION;
    insert into m_user (oid, name_orig, version) VALUES ('66eb4861-867d-4a41-b6f0-41a3874bd48f', 'conflict', 1);
    commit;
commit;
-- switch Tx back to Auto if desired - only resource1 should be inserted
select * from m_object where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';

-- adding x users
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

-- MUST fail on OID constraint if existing OID is used in SET:
update m_object
set oid='66eb4861-867d-4a41-b6f0-41a3874bd48f'
where oid='f7a0362f-37a5-4dea-ac16-9c84dce333dc';

select * from m_user where name_norm is not null;

select * from m_object
where oid='66eb4861-867d-4a41-b6f0-41a3874bd48f';

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
