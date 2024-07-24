/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.H2Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.util.MidPointOracleDialect;
import com.evolveum.midpoint.repo.sql.util.MidPointPostgreSQLDialect;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;

@SuppressWarnings("deprecation")
public enum Database {

    // Order of dialects is important, the first value is the default.
    H2("org.h2.Driver",
            H2Dialect.class.getName()),
    POSTGRESQL("org.postgresql.Driver",
            MidPointPostgreSQLDialect.class.getName(),
            org.hibernate.dialect.PostgreSQLDialect.class.getName(),
            org.hibernate.dialect.PostgresPlusDialect.class.getName()),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver",
            UnicodeSQLServer2008Dialect.class.getName(),
            org.hibernate.dialect.SQLServerDialect.class.getName()),
    ORACLE("oracle.jdbc.OracleDriver",
            MidPointOracleDialect.class.getName(),
            org.hibernate.dialect.OracleDialect.class.getName()
            );

    @NotNull List<String> drivers;
    @NotNull List<String> dialects;

    Database(String driver, String... dialects) {
        this.drivers = Collections.singletonList(driver);
        this.dialects = Arrays.asList(dialects);
    }

    public static Database findDatabase(String databaseName) {
        if (StringUtils.isBlank(databaseName)) {
            return null;
        }
        for (Database database : values()) {
            if (database.name().equalsIgnoreCase(databaseName)) {
                return database;
            }
        }
        throw new IllegalArgumentException("Unsupported database type: " + databaseName);
    }

    public String getDefaultHibernateDialect() {
        return dialects.get(0);
    }

    public String getDefaultDriverClassName() {
        return drivers.get(0);
    }

    public boolean containsDriver(String driverClassName) {
        return drivers.contains(driverClassName);
    }

    public boolean containsDialect(String hibernateDialect) {
        return dialects.contains(hibernateDialect);
    }

    @Nullable
    public static Database findByDriverClassName(String driverClassName) {
        if (driverClassName != null) {
            return Arrays.stream(values())
                    .filter(db -> db.containsDriver(driverClassName))
                    .findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static Database findByHibernateDialect(String hibernateDialect) {
        if (hibernateDialect != null) {
            return Arrays.stream(values())
                    .filter(db -> db.containsDialect(hibernateDialect))
                    .findFirst().orElse(null);
        } else {
            return null;
        }
    }
}
