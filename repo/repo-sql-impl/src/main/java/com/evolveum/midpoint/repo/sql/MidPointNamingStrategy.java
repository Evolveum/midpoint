/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.EJB3NamingStrategy;

/**
 * @author lazyman
 */
public class MidPointNamingStrategy extends EJB3NamingStrategy {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointNamingStrategy.class);

    @Override
    public String classToTableName(String className) {
        String name = className.substring(1);
        //change cammel case to underscore delimited
        name = name.replaceAll(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
        ), "_");

        String result = "m_" + name.toLowerCase();
        LOGGER.trace("classToTableName {} to {}", new Object[]{className, result});
        return result;
    }

    @Override
    public String logicalColumnName(String columnName, String propertyName) {
        String result;
        if (StringUtils.isNotEmpty(columnName)) {
            result = columnName;
        } else {
            if (propertyName.startsWith("credentials.") || propertyName.startsWith("activation.")) {
                //credentials and activation are embedded and doesn't need to be qualified
                result = super.propertyToColumnName(propertyName);
            } else {
                result = propertyName.replaceAll("\\.", "_");
            }
        }
        LOGGER.trace("logicalColumnName {} {} to {}", new Object[]{columnName, propertyName, result});
        return result;
    }

    @Override
    public String propertyToColumnName(String propertyName) {
        String result = propertyName.replaceAll("\\.", "_");
        if (propertyName.contains("&&")) {
            result = super.propertyToColumnName(propertyName);
        } else if (propertyName.startsWith("credentials.") || propertyName.startsWith("activation.")) {
            //credentials and activation are embedded and doesn't need to be qualified
            result = super.propertyToColumnName(propertyName);
        }

        LOGGER.trace("propertyToColumnName {} to {} (original: {})",
                new Object[]{propertyName, result, super.propertyToColumnName(propertyName)});
        return result;
    }

    @Override
    public String joinKeyColumnName(String joinedColumn, String joinedTable) {
        String result = super.joinKeyColumnName(joinedColumn, joinedTable);
        LOGGER.trace("joinKeyColumnName {} {} to {}", new Object[]{joinedColumn, joinedTable, result});
        return result;
    }

    @Override
    public String columnName(String columnName) {
        String result = super.columnName(columnName);
        LOGGER.trace("columnName {} to {}", new Object[]{columnName, result});
        return result;
    }

    @Override
    public String foreignKeyColumnName(String propertyName, String propertyEntityName,
            String propertyTableName, String referencedColumnName) {

        String result = super.foreignKeyColumnName(propertyName, propertyEntityName,
                propertyTableName, referencedColumnName);
        LOGGER.trace("foreignKeyColumnName {} {} {} {} to {}", new Object[]{propertyName,
                propertyEntityName, propertyTableName, referencedColumnName, result});
        return result;
    }
}
