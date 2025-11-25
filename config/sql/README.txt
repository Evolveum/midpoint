Subdirectories contain SQL schema scripts for the repository implementations:

Check our documentation for further details on this topic:

* Repository: https://docs.evolveum.com/midpoint/reference/repository/
* Databases: https://docs.evolveum.com/midpoint/reference/repository/db/
* Configuration: https://docs.evolveum.com/midpoint/reference/repository/configuration/
* DB support: https://docs.evolveum.com/midpoint/reference/repository/repository-database-support/

SQL schema for the repository is split into the following files:

* postgres.sql - main part of the repository; this is always needed.
* postgres-audit.sql - audit tables; this can be applied on top of the main
repository or in a new database if separate audit database is desired.
* postgres-quartz.sql - tables for Quartz scheduler, can be applied safely.

Unless you plan to use separate database for audit, just apply all these schema
files in the order named above. Even if tables are not needed, no harm is done.

See also:

* Using native repo: https://docs.evolveum.com/midpoint/reference/repository/repository-native-usage/
