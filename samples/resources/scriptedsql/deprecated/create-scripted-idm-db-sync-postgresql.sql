CREATE USER mdp_scriptedsql WITH PASSWORD 'password' LOGIN;
CREATE DATABASE mdp_scriptedsql WITH OWNER = mdp_scriptedsql ENCODING = 'UTF8' TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1;
