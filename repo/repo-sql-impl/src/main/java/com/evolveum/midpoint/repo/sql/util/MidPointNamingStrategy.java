/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.EJB3NamingStrategy;

/**
 * @author lazyman
 */
public class MidPointNamingStrategy extends EJB3NamingStrategy {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointNamingStrategy.class);
    private static final int MAX_LENGTH = 30;

    @Override
    public String columnName(String columnName) {
        String rv = super.columnName(columnName);
        LOGGER.trace("columnName {} to {}", columnName, rv);
        return rv;
    }

    @Override
    public String joinKeyColumnName(String joinedColumn, String joinedTable) {
        String rv = super.joinKeyColumnName(joinedColumn, joinedTable);
        LOGGER.trace("joinKeyColumnName joinedColumn={}, joinedTable={} to {}", joinedColumn, joinedTable, rv);
        return rv;
    }

    @Override
    public String classToTableName(String className) {
        String name = className.substring(1);
        //change camel case to underscore delimited
        name = name.replaceAll(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
        ), "_");

        String result = "m_" + name.toLowerCase();
        result = fixLength(result);

        LOGGER.trace("classToTableName {} to {}", className, result);
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
        result = fixLength(result);

        LOGGER.trace("logicalColumnName {} {} to {}", columnName, propertyName, result);
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
        result = fixLength(result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("propertyToColumnName {} to {} (original: {})",
                    propertyName, result, super.propertyToColumnName(propertyName));
        }
        return result;
    }

    @Override
    public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName) {
        // TODO fixme BRUTAL HACK -- we are not able to eliminate columns like 'ownerRefCampaign_targetOid' from the schema (even with @AttributeOverride/@AssociationOverride)
        if ("ownerRefCampaign.target".equals(propertyName) ||
                "ownerRefDefinition.target".equals(propertyName) ||
                "ownerRefTask.target".equals(propertyName)) {
            propertyName = "ownerRef.target";
        }
        String header = propertyName != null ? propertyName.replaceAll("\\.", "_") : propertyTableName;
        String result;
        if (header.endsWith("target") && referencedColumnName.equals("oid")) {
            result = header + "Oid";        // to keep compatibility with existing mappings
        } else {
            result = header + "_" + referencedColumnName;
        }
        result = fixLength(result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("foreignKeyColumnName for propertyName={}, propertyEntityName={}, propertyTableName={}, " +
                            "referencedColumnName={} returns {} (original: {})", propertyName, propertyEntityName,
                    propertyTableName, referencedColumnName, result,
                    super.foreignKeyColumnName(propertyName, propertyEntityName, propertyTableName, referencedColumnName));
        }
        return result;
    }

    private String fixLength(String input) {
        if (input == null || input.length() <= MAX_LENGTH) {
            return input;
        }

        String result = input;
        String[] array = input.split("_");
        for (int i = 0; i < array.length; i++) {
            int length = array[i].length();
            String lengthStr = Integer.toString(length);

            if (length < lengthStr.length()) {
                continue;
            }

            array[i] = array[i].charAt(0) + lengthStr;

            result = StringUtils.join(array, "_");
            if (result.length() < MAX_LENGTH) {
                break;
            }
        }

        return result;
    }
}
