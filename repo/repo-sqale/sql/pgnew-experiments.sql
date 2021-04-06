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

INSERT INTO m_user (name_norm, name_orig, fullobject, ext, policySituations, version)
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
    CASE
        WHEN r % 10 > 4 THEN
            -- let's add some policy situations (IDs of M_URI entries)
            random_pick(ARRAY(SELECT a.n FROM generate_series(1, 100) AS a(n)), r % 10 / 100::decimal)
        -- ELSE NULL is default and redundant
        END,
    1
from generate_series(100001,1000000) as r;

EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
select oid, policysituations from m_user
where policysituations @> '{10}'
-- order by name_norm desc
;

/* 1k rows
Seq Scan on public.m_user  (cost=0.00..14947.00 rows=3437 width=65) (actual time=0.029..39.071 rows=3567 loops=1)
"  Output: oid, policysituations"
  Filter: (m_user.policysituations @> '{10}'::integer[])
  Rows Removed by Filter: 96433
  Buffers: shared hit=11650 read=2047
Planning:
  Buffers: shared hit=7
Planning Time: 0.076 ms
Execution Time: 39.246 ms

1M rows (without analyze 125ms for first 500 rows):
Gather  (cost=1000.00..146779.83 rows=34406 width=65) (actual time=40.305..2390.356 rows=34949 loops=1)
"  Output: oid, policysituations"
  Workers Planned: 2
  Workers Launched: 2
  Buffers: shared hit=10990 read=126405 dirtied=89590 written=78799
  ->  Parallel Seq Scan on public.m_user  (cost=0.00..142339.23 rows=14336 width=65) (actual time=42.050..2216.133 rows=11650 loops=3)
"        Output: oid, policysituations"
        Filter: (m_user.policysituations @> '{10}'::integer[])
        Rows Removed by Filter: 321684
        Buffers: shared hit=10990 read=126405 dirtied=89590 written=78799
        Worker 0:  actual time=37.016..2129.323 rows=10913 loops=1
          JIT:
            Functions: 4
"            Options: Inlining false, Optimization false, Expressions true, Deforming true"
"            Timing: Generation 4.363 ms, Inlining 0.000 ms, Optimization 9.042 ms, Emission 24.312 ms, Total 37.717 ms"
          Buffers: shared hit=3011 read=39940 dirtied=28314 written=25406
        Worker 1:  actual time=50.567..2173.420 rows=11177 loops=1
          JIT:
            Functions: 4
"            Options: Inlining false, Optimization false, Expressions true, Deforming true"
"            Timing: Generation 4.199 ms, Inlining 0.000 ms, Optimization 3.489 ms, Emission 33.603 ms, Total 41.291 ms"
          Buffers: shared hit=3637 read=40546 dirtied=28917 written=25375
Planning Time: 0.088 ms
JIT:
  Functions: 12
"  Options: Inlining false, Optimization false, Expressions true, Deforming true"
"  Timing: Generation 20.183 ms, Inlining 0.000 ms, Optimization 14.305 ms, Emission 87.116 ms, Total 121.604 ms"
Execution Time: 2411.721 ms
 */
DROP INDEX m_user_policySituation_idx;
/* 1k rows
Bitmap Heap Scan on public.m_user  (cost=38.63..7971.34 rows=3437 width=65) (actual time=0.895..6.448 rows=3567 loops=1)
"  Output: oid, policysituations"
  Recheck Cond: (m_user.policysituations @> '{10}'::integer[])
  Heap Blocks: exact=3133
  Buffers: shared hit=2514 read=622
  ->  Bitmap Index Scan on m_user_policysituation_idx  (cost=0.00..37.77 rows=3437 width=0) (actual time=0.550..0.550 rows=3567 loops=1)
        Index Cond: (m_user.policysituations @> '{10}'::integer[])
        Buffers: shared hit=3
Planning:
  Buffers: shared hit=25
Planning Time: 0.228 ms
Execution Time: 6.603 ms

1M rows (but not parallel, much lower cost (1000 to 330), fast without analyze, 96ms for first 500 rows):
Bitmap Heap Scan on public.m_user  (cost=326.49..80473.40 rows=34902 width=64) (actual time=23.726..3046.666 rows=34949 loops=1)
"  Output: oid, policysituations"
  Recheck Cond: (m_user.policysituations @> '{10}'::integer[])
  Heap Blocks: exact=31249
  Buffers: shared hit=13 read=31249 written=4948
  ->  Bitmap Index Scan on m_user_policysituation_idx  (cost=0.00..317.77 rows=34902 width=0) (actual time=15.035..15.035 rows=34949 loops=1)
        Index Cond: (m_user.policysituations @> '{10}'::integer[])
        Buffers: shared hit=13
Planning:
  Buffers: shared hit=73
Planning Time: 0.434 ms
Execution Time: 3053.032 ms
 */
CREATE INDEX m_user_policySituation_idx
    ON m_user USING GIN(policysituations);

/* 1M rows, clear winner, ~60ms without analyze/select of first 500 rows (also the index built under 1.5s, so it's faster even with its creation):
Bitmap Heap Scan on public.m_user  (cost=326.48..80471.49 rows=34900 width=64) (actual time=12.360..120.267 rows=34949 loops=1)
"  Output: oid, policysituations"
  Recheck Cond: (m_user.policysituations @> '{10}'::integer[])
  Heap Blocks: exact=31249
  Buffers: shared hit=450 read=30812 written=165
  ->  Bitmap Index Scan on m_user_policysituation_idx2  (cost=0.00..317.75 rows=34900 width=0) (actual time=6.909..6.910 rows=34949 loops=1)
        Index Cond: (m_user.policysituations @> '{10}'::integer[])
        Buffers: shared hit=13
Planning:
  Buffers: shared hit=1
Planning Time: 0.099 ms
Execution Time: 122.310 ms
 */
CREATE EXTENSION IF NOT EXISTS intarray;

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
-- t = table, tt = toast table
select t.oid as table_oid,
    t.relname as table_name,
    tt.relname as toast_name,
    pg_size_pretty(pg_relation_size(t.oid)) AS table_size,
    pg_size_pretty(pg_relation_size(tt.oid)) AS toast_size,
    pg_size_pretty(pg_relation_size(t.oid) + coalesce(pg_relation_size(tt.oid), 0)) AS total_size
from pg_class t
         left join pg_class tt on t.reltoastrelid = tt.oid and tt.relkind = 't'
where t.relkind = 'r' and t.relnamespace = (select oid from pg_namespace where nspname = 'public')
order by total_size desc;

-- find sequence name for serial column (e.g. to alter its value later)
select pg_get_serial_sequence('m_qname', 'id');

-- optional, little overhead, adds visibility, requires postgresql.conf change:
-- shared_preload_libraries = 'pg_stat_statements' + restart
create extension pg_stat_statements;
select * from pg_stat_statements; -- this fails without the steps above
-- select pg_stat_statements_reset(); -- to reset statistics

-- STATS/perf selects
select
  (total_exec_time / 1000 / 60) as total_min,
  mean_exec_time as avg_ms,
  calls,
  query
from pg_stat_statements
--   order by total_min desc
  order by avg_ms desc
  limit 50;

select query, calls, total_exec_time, rows, 100.0 * shared_blks_hit /
    nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
from pg_stat_statements order by total_exec_time desc limit 5;

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

/* Test: should return 10 and 20, just to check the ranges
select min(length(i)), max(length(i))
from (select random_bytea(10, 20) as i from generate_series(1, 200)) q;
*/

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
