CREATE USER create_midpoint_schema_test WITH PASSWORD 'create_midpoint_schema_test' LOGIN SUPERUSER;

CREATE DATABASE create_midpoint_schema_test WITH OWNER = create_midpoint_schema_test ENCODING = 'UTF8'
    TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1 TEMPLATE = template0;
