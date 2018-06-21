/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.Validate;
import org.hibernate.dialect.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class TemporaryTableDialect {

    /**
     * Does this dialect support temporary tables?
     *
     * @return True if temp tables are supported; false otherwise.
     */
    public boolean supportsTemporaryTables() {
        return true;
    }

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

    public static TemporaryTableDialect getTempTableDialect(Dialect dialect) {
        Validate.notNull(dialect, "Dialect must not be null");

        if (dialect instanceof H2Dialect) {
            return new H2TempTableDialect();
        }

        if (dialect instanceof MySQL5InnoDBDialect) {
            return new MysqlTempTableDialect();
        }

        if (dialect instanceof PostgreSQL95Dialect) {
            return new PostgreSQLTempTableDialect();
        }

        if (dialect instanceof Oracle12cDialect) {
            return new OracleTempTableDialect();
        }

        if (dialect instanceof SQLServer2008Dialect) {
            return new SQLServerTempTableDialect();
        }

        throw new SystemException("Unknown dialect " + dialect.getClass().getName());
    }

    private static class H2TempTableDialect extends TemporaryTableDialect {

        @Override
        public String getCreateTemporaryTableString() {
            return "create cached local temporary table if not exists";
        }

        @Override
        public String getCreateTemporaryTablePostfix() {
            // actually 2 different options are specified here:
            //		1) [on commit drop] - says to drop the table on transaction commit
            //		2) [transactional] - says to not perform an implicit commit of any current transaction
            return "on commit drop transactional";
        }

        @Override
        public boolean dropTemporaryTableAfterUse() {
            return false;
        }
    }

    private static class MysqlTempTableDialect extends TemporaryTableDialect {

        @Override
        public String getCreateTemporaryTableString() {
            return "create temporary table if not exists";
        }

        @Override
        public String getDropTemporaryTableString() {
            return "drop temporary table";
        }

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
