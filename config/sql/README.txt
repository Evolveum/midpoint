Subdirectories contain SQL schema scripts for two repository implementations:

* generic: This was the only available implementation before midPoint 4.4.
It supports various SQL databases, currently Oracle and Microsoft
SQL Server is supported. Repository implementation uses Hibernate ORM system.
This repository is planned for removal after 4.4 LTS cycle.

* native: This is newer repository implementation that is available since
midPoint 4.4. Only PostgreSQL from version 12 higher is supported.
This is the repository we plan to support and improve in the future.

Check our documentation for further details on this topic:

* Repository: https://docs.evolveum.com/midpoint/reference/repository/
* Databases: https://docs.evolveum.com/midpoint/reference/repository/db/
* Configuration: https://docs.evolveum.com/midpoint/reference/repository/configuration/
* DB support: https://docs.evolveum.com/midpoint/reference/repository/repository-database-support/


NATIVE (NEW) REPOSITORY

SQL schema for Native repository is split into the following files:

* postgres.sql - main part of the repository; this is always needed.
* postgres-audit.sql - audit tables; this can be applied on top of the main
repository or in a new database if separate audit database is desired.
* postgres-quartz.sql - tables for Quartz scheduler, can be applied safely.

Unless you plan to use separate database for audit, just apply all these schema
files in the order named above. Even if tables are not needed, no harm is done.

See also:

* Using native repo: https://docs.evolveum.com/midpoint/reference/repository/repository-native-usage/


GENERIC (OLD) REPOSITORY

For each supported database there is a single <database>-<version>-all.sql file
that initializes tables for the main part of the repository, audit tables and
scheduler (Quartz) tables.

There is also a separate <database>-upgrade-<from>-<to>.sql script for upgrade
from the previous midPoint version. For LTS releases another script for upgrade
from previous LTS version is also provided. These are to be used if you already
have running midPoint on such previous version.

See also:

* Upgrade: https://docs.evolveum.com/midpoint/reference/upgrade/database-schema-upgrade/
