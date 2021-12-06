/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- This is the update script for the MAIN REPOSITORY, it will not work for a separate audit database.
-- It is safe to run this script repeatedly, so if you're not sure you're up to date.

-- SCHEMA-COMMIT is a commit which should be used to initialize the DB for testing changes below it.
-- Check out that commit and initialize a fresh DB with postgres-new-audit.sql to test upgrades.

-- Initializing the last change number used in postgres-new-upgrade.sql.
call apply_change(0, $$ SELECT 1 $$, true);

-- SCHEMA-COMMIT 4.0: commit 69e8c29b

-- changes for 4.4.1
