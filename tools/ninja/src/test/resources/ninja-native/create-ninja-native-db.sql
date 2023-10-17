/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

CREATE USER ninja_native_tests WITH PASSWORD 'ninja_native_tests' LOGIN SUPERUSER;

COMMIT;

CREATE DATABASE ninja_native_tests WITH OWNER = ninja_native_tests ENCODING = 'UTF8'
    TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1 TEMPLATE = template0;
