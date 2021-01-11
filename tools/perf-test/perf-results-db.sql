-- Copyright (C) 2010-2021 Evolveum and contributors
--
-- This work is dual-licensed under the Apache License 2.0
-- and European Union Public License. See LICENSE file for details.
--
-- @formatter:off because of terribly unreliable IDEA reformat for SQL

-- PERF TEST RESULTS DB
-- mst_ prefix means: "MidScale Test"
-- TODO: add indexes (and/or views) as necessary for Grafana queries

-- drop view v_stopwatch;
-- drop table mst_stopwatch; drop table mst_glob_perf_info; drop table mst_query; drop table mst_build;
-- delete from mst_glob_perf_info; delete from mst_query; delete from mst_stopwatch; delete from mst_build;

create table mst_build (
    id SERIAL NOT NULL, -- surrogate PK
    build VARCHAR(32) NOT NULL UNIQUE, -- build #
    branch VARCHAR(64) NOT NULL,
    commit_hash VARCHAR(40) NOT NULL UNIQUE, -- we don't want to process the same commit multiple times
    date TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (id)
);

create table mst_stopwatch (
    build_id SERIAL NOT NULL REFERENCES mst_build(id),
    test VARCHAR(256) NOT NULL,
    monitor VARCHAR(512) NOT NULL,
    count INTEGER NOT NULL,
    total_us BIGINT NOT NULL,
    avg_us BIGINT NOT NULL,
    min_us BIGINT NOT NULL,
    max_us BIGINT NOT NULL,
-- note VARCHAR(1024), not imported yet due to quoting/escaping problems

    PRIMARY KEY (build_id, test, monitor)
);

create table mst_glob_perf_info (
    build_id SERIAL NOT NULL REFERENCES mst_build(id),
    test VARCHAR(256) NOT NULL,
    operation VARCHAR(512) NOT NULL,
    count INTEGER NOT NULL,
    total_ms NUMERIC NOT NULL,
    avg_ms NUMERIC NOT NULL,
    min_ms NUMERIC NOT NULL,
    max_ms NUMERIC NOT NULL,

    PRIMARY KEY (build_id, test, operation)
);

create table mst_query (
    build_id SERIAL NOT NULL REFERENCES mst_build(id),
    test VARCHAR(256) NOT NULL,
    metric VARCHAR(512) NOT NULL,
    count INTEGER NOT NULL,

    PRIMARY KEY (build_id, test, metric)
);

create or replace view v_stopwatch as
select build_id, build, branch, commit_hash, date,
    test, monitor, count, total_us, avg_us, min_us, max_us
from mst_build b
    join mst_stopwatch s on b.id = s.build_id;

create or replace view v_glob_perf_info as
select build_id, build, branch, commit_hash, date,
    test, operation, count, total_ms, avg_ms, min_ms, max_ms
from mst_build b
    join mst_glob_perf_info m on b.id = m.build_id;

create or replace view v_query as
select build_id, build, branch, commit_hash, date,
    test, metric, count
from mst_build b
    join mst_query m on b.id = m.build_id;
