/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class TemporaryTableDialect {

    /**
     * Generate a temporary table name given the base table.
     *
     * @param baseTableName The table name from which to base the temp table name.
     * @return The generated temp table name.
     */
    public String generateTemporaryTableName(String baseTableName) {
        return "HT_" + baseTableName;
    }

    /**
     * Command used to create a temporary table.
     *
     * @return The command used to create a temporary table.
     */
    public String getCreateTemporaryTableString() {
        return "create table";
    }

    /**
     * Get any fragments needing to be postfixed to the command for
     * temporary table creation.
     *
     * @return Any required postfix.
     */
    public String getCreateTemporaryTablePostfix() {
        return "";
    }

    /**
     * Command used to drop a temporary table.
     *
     * @return The command used to drop a temporary table.
     */
    public String getDropTemporaryTableString() {
        return "drop table";
    }

    /**
     * Do we need to drop the temporary table after use?
     *
     * @return True if the table should be dropped.
     */
    public boolean dropTemporaryTableAfterUse() {
        return true;
    }

    /**
     * Returns instance of {@link TemporaryTableDialect} or throws, never returns null.
     *
     * @throws SystemException if temporary tables are not supported
     */
    public static @NotNull TemporaryTableDialect getTempTableDialect(
            @NotNull SupportedDatabase database) {
        switch (database) {
            case POSTGRESQL:
                return new PostgreSQLTempTableDialect();
            case ORACLE:
                return new OracleTempTableDialect();
            case SQLSERVER:
                return new SQLServerTempTableDialect();
        }

        throw new SystemException(
                "Temporary tables are not supported for database type " + database);
    }

    private static class PostgreSQLTempTableDialect extends TemporaryTableDialect {

        @Override
        public String getCreateTemporaryTableString() {
            return "create temporary table if not exists";
        }

        @Override
        public String getCreateTemporaryTablePostfix() {
            return "on commit drop";
        }
    }

    private static class SQLServerTempTableDialect extends TemporaryTableDialect {

        @Override
        public String generateTemporaryTableName(String baseTableName) {
            return "#" + baseTableName;
        }
    }

    private static class OracleTempTableDialect extends TemporaryTableDialect {

        @Override
        public String generateTemporaryTableName(String baseTableName) {
            final String name = super.generateTemporaryTableName(baseTableName);
            return name.length() > 30 ? name.substring(0, 30) : name;
        }

        @Override
        public String getCreateTemporaryTableString() {
            return "create global temporary table";
        }

        @Override
        public String getCreateTemporaryTablePostfix() {
            return "on commit delete rows";
        }

        @Override
        public boolean dropTemporaryTableAfterUse() {
            return false;
        }
    }
}
