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

INSERT INTO m_user (name_norm, name_orig, createtimestamp, modifytimestamp, version)
VALUES (md5(random()::TEXT), md5(random()::TEXT), current_timestamp, current_timestamp, 1);

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

-- MUST fail on OID constraint if existing OID is used in SET:
update m_object
set oid='66eb4861-867d-4a41-b6f0-41a3874bd48f'
where oid='f7a0362f-37a5-4dea-ac16-9c84dce333dc';

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

-- adding x users
-- 100_000 inserts: 3 inherited tables ~6s, for 25 inherited tables ~13s, for 50 ~20s, for 100 ~34s
-- change with volume (100 inherited tables): 200k previous rows ~34s, with 1m rows ~37s
-- with 3 inherited tables and 5M existing rows, adding 100k rows takes ~7s
-- delete from m_object;
-- delete from m_object_oid;
select count(*) from m_object_oid;
EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
select count(*) from m_user;
-- vacuum full analyze; -- this requires exclusive lock on processed table and can be very slow, with 1M rows it takes 10s
vacuum analyze; -- this is normal operation version (can run in parallel, ~25s/25m rows)

INSERT INTO m_resource (name_norm, name_orig, fullobject, version)
SELECT 'resource-' || LPAD(r::text, 10, '0'),
    'resource-' || LPAD(r::text, 10, '0'),
    random_bytea(100, 20000),
    1
from generate_series(1, 10) as r;

INSERT INTO m_user (name_norm, name_orig, fullobject, ext, version)
SELECT 'user-' || LPAD(r::text, 10, '0'),
    'user-' || LPAD(r::text, 10, '0'),
    random_bytea(100, 2000),
    CASE
        WHEN r % 10 <= 1 THEN
            ('{"likes": ' || array_to_json(random_pick(ARRAY['eating', 'books', 'music', 'dancing', 'walking', 'jokes', 'video', 'photo'], 0.4))::text || '}')::jsonb
        WHEN r % 10 <= 3 THEN
            -- some entries have no extension
            NULL
        WHEN r % 10 <= 5 THEN
            ('{"email": "user' || r || '@mycompany.com", "other-key-' || r || '": "other-value-' || r || '"}')::jsonb
        WHEN r % 10 = 6 THEN
            -- some extensions are massive JSONs, we want to see they are TOAST-ed
            (select '{' || string_agg('"key-' || i || '": "value"', ',') || '}' from generate_series(1, 1000) i)::jsonb
        WHEN r % 10 = 7 THEN
            -- let's add some numbers and wannabe "dates"
            ('{"hired": "' || current_date - width_bucket(random(), 0, 1, 1000) || '", "rating": ' || width_bucket(random(), 0, 1, 10) || '}')::jsonb
        ELSE
            ('{"hired": "' || current_date - width_bucket(random(), 0, 1, 1000) || '",' ||
                '"hobbies": ' || array_to_json(random_pick(ARRAY['eating', 'books', 'music', 'dancing', 'walking', 'jokes', 'video', 'photo'], 0.5))::text || '}')::jsonb
        END,
    1
from generate_series(2000001, 3000000) as r;

select * from m_user;

select * from m_object
where oid<'812c5e7a-8a94-4bd1-944d-389e7294b831'
order by oid;

-- EXPLAIN selects
EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
SELECT *
FROM m_object
where oid='cf72947b-f7b5-4b44-a2b1-07452b9056cc'
;

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
SELECT count(*)
-- SELECT *
FROM m_focus
;

--------------
-- sandbox

-- JSON experiments
CREATE INDEX m_user_ext_email_idx ON m_user ((ext ->> 'email'));
-- with WHERE the index is smaller, but the same condition must be used in WHERE as well
CREATE INDEX m_user_ext_email2_idx ON m_user ((ext ->> 'email')) WHERE ext ? 'email';
CREATE INDEX m_user_ext_hired_idx ON m_user ((ext ->> 'hired'));
CREATE INDEX m_user_ext_hired2_idx ON m_user ((ext ->> 'hired')) WHERE ext ? 'hired';
-- DROP INDEX m_user_ext_hired2_idx;
-- set jit=on; -- JIT can sometimes be slower when planner guesses wrong

-- see also https://www.postgresql.org/docs/13/functions-json.html some stuff is only for JSONB
EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
-- select count(*)
select oid, name_norm, ext
from m_user
    where
--           ext?'hobbies' and -- faster, uses GIN index
          ext @> '{"hobbies":["video"]}'
--     where ext->>'hobbies' is not null -- seq-scan
;

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
select ext->>'hobbies' from m_user where ext ? 'hobbies'; -- return values of ext as text (or number), uses GIN index

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
-- contains in [] or as value directly, but use GIN index on (ext)
-- it would use GIN index on ext->'hobbies' though
select count(*) from m_user where ext->'hobbies' @> '"video"';

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
-- select *
select count(*)
from m_user
-- where ext ? 'email' -- uses index, still takes time with many matches (depending on limit)
--     and ext->>'email' > 'user12666133@mycompany.com'
where ext->>'email' > 'user12666133@mycompany.com' -- uses m_user_ext_email_idx
--     and ext?'email' -- with this it uses m_user_ext_email2_idx, but also can benefit just from default GIN index

-- where NOT (ext->>'hired' >= '2020-06-01' AND ext?'hired') -- not using function index
--     and ext?'hired' -- using function index with this clause, super fast
-- order by ext->>'hired'
;

analyse;

-- where ext @> '{"email": "user12666133@mycompany.com"}' -- uses index, fast
-- where ext @> '{"hobbies": ["video"]}' -- uses index, fast
-- where ext->'email' ? 'user12666133@mycompany.com' -- doesn't use GIN(ext), but can use gin ((ext -> 'email')), then it's fast
-- where ext->>'email' = 'user12666133@mycompany.com' -- doesn't use neither GIN(ext) nor gin ((ext -> 'email'))
-- and id >= 900000
-- order by id
-- limit 500
;

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
-- select count(*)
-- select * -- "select *" with big bytea stuff can dominate the total cost
select oid, length(fullobject) -- much faster than *
from m_user
where
    ext?'hobbies' and
    -- without array test jsonb_array_elements_text function can fail on scalar value (if allowed)
    exists (select from jsonb_array_elements_text(ext->'hobbies') v
--         where jsonb_typeof(ext->'hobbies') = 'array'
--             and upper(v::text) LIKE '%ING')
        where upper(v::text) LIKE '%ING')
--     and oid > 14000000
order by oid
;

-- MANAGEMENT queries

-- See: https://wiki.postgresql.org/wiki/Disk_Usage

-- top 20 biggest tables or their TOAST (large object storage) from public schema
SELECT
    t.oid,
    CASE
        WHEN tft.relname IS NOT NULL
            THEN tft.relname || ' (TOAST)'
        ELSE t.relname
    END AS object,
    pg_size_pretty(pg_relation_size(t.oid)) AS size
FROM pg_class t
    INNER JOIN pg_namespace ns ON ns.oid = t.relnamespace
    -- table for toast
    LEFT JOIN pg_class tft ON tft.reltoastrelid = t.oid
    LEFT JOIN pg_namespace tftns ON tftns.oid = tft.relnamespace
WHERE 'public' IN (ns.nspname, tftns.nspname)
ORDER BY pg_relation_size(t.oid) DESC
LIMIT 50;

vacuum full analyze;
-- database size
SELECT pg_size_pretty(pg_database_size('midpoint'));

-- show tables + their toast tables ordered from the largest toast table
-- ut = user table, tt = toast table
select ut.oid, ut.relname, ut.relkind, tt.relkind, tt.relname, tt.relpages, tt.reltuples
from pg_class ut
    inner join pg_class tt on ut.reltoastrelid = tt.oid
    inner join pg_namespace ns ON ut.relnamespace = ns.oid
where ut.relkind = 'r' and tt.relkind = 't'
    and ns.nspname = 'public'
order by relpages desc;

-- find sequence name for serial column (e.g. to alter its value later)
select pg_get_serial_sequence('m_qname', 'id');

-- optional, little overhead, adds visibility, reqiures postgresql.conf change:
-- shared_preload_libraries = 'pg_stat_statements' + restart
-- create extension pg_stat_statements; -- is this necessary?
select * from pg_stat_statements; -- this fails without the pg.conf change + restart

-- PRACTICAL UTILITY FUNCTIONS

-- based on https://dba.stackexchange.com/a/22571
CREATE OR REPLACE FUNCTION random_bytea(min_len integer, max_len integer)
    RETURNS bytea
    LANGUAGE sql
    -- VOLATILE - default behavior, can't be optimized, other options are IMMUTABLE or STABLE
AS $$
    SELECT decode(string_agg(lpad(to_hex(width_bucket(random(), 0, 1, 256) - 1), 2, '0'), ''), 'hex')
    -- width_bucket starts with 1, we counter it with series from 2; +1 is there to includes upper bound too
    -- should be marginally more efficient than: generate_series(1, $1 + trunc(random() * ($2 - $1 + 1))::integer)
    FROM generate_series(2, $1 + width_bucket(random(), 0, 1, $2 - $1 + 1));
$$;

CREATE OR REPLACE FUNCTION zero_bytea(min_len integer, max_len integer)
    RETURNS bytea
    LANGUAGE sql
AS $$
    SELECT decode(string_agg('00', ''), 'hex')
    FROM generate_series(2, $1 + width_bucket(random(), 0, 1, $2 - $1 + 1));
$$;

-- should return 10 and 20, just to check the ranges
select min(length(i)), max(length(i))
from (select random_bytea(10, 20) as i from generate_series(1, 200)) q;

-- returns random element from array (NULL for empty arrays)
CREATE OR REPLACE FUNCTION random_pick(vals ANYARRAY)
    RETURNS ANYELEMENT
    LANGUAGE plpgsql
AS $$ BEGIN
    -- array_lower is used if array subscript doesn't start with 1 (which is default)
    RETURN vals[array_lower(vals, 1) - 1 + width_bucket(random(), 0, 1, array_length(vals, 1))];
END $$;

-- returns random elements from array
-- output is of random length based on ratio (0-1), 0 returns nothing, 1 everything
CREATE OR REPLACE FUNCTION random_pick(vals ANYARRAY, ratio numeric, ignore ANYELEMENT = NULL)
    RETURNS ANYARRAY
    LANGUAGE plpgsql
AS $$
DECLARE
    rval vals%TYPE := '{}';
    val ignore%TYPE;
BEGIN
    IF vals IS NULL THEN
        RETURN NULL;
    END IF;

    -- Alternative FOR i IN array_lower(vals, 1) .. array_upper(vals, 1) LOOP does not need "ignore" parameter.
    -- Functions array_lower/upper are better if array subscript doesn't start with 1 (which is default).
    FOREACH val IN ARRAY vals LOOP
        IF random() < ratio THEN
            rval := array_append(rval, val); -- alt. vals[i]
        END IF;
    END LOOP;
    -- It's also possible to iterate without index with , but requires
    -- more
    RETURN rval;
END $$;
