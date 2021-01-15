/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

/**
 * Enumeration of supported SQL/databases (RDBMS).
 */
public enum SupportedDatabase {
    H2,
    POSTGRESQL,
    ORACLE,
    SQLSERVER,
    MYSQL,
    MARIADB;

    public boolean supportsLimitOffset() {
        return this == H2 || this == POSTGRESQL || this == MYSQL || this == MARIADB;
    }
}
