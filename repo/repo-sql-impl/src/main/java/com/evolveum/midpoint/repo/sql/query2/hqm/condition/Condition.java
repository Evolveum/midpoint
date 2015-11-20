/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;

import java.io.Serializable;
import java.util.List;

/**
 * Condition in HQL.
 *
 * @author mederly
 */
public class Condition {

    public void dumpToHql(StringBuilder sb, int indent) {
        if (indent >= 0) {
            HibernateQuery.indent(sb, indent);
        }
        // TODO
    }

    public static void dumpToHql(StringBuilder sb, List<Condition> conditions, int indent) {
        boolean first = true;
        for (Condition condition : conditions) {
            if (first) {
                first = false;
            } else {
                sb.append(" and\n");
            }
            condition.dumpToHql(sb, indent);
        }
    }

    public static Condition gt(String propertyPath, Object value) {
        //TODO
    }

    public static Condition eq(String propertyPath, String value, Class<? extends Serializable> type) {
        return null;
    }

    public static Condition and(List<Condition> conditions) {
        return null;
    }
    public static AndCondition and(Condition... conditions) {
        return null;
    }

    public static Condition isNull(String propertyName) {
        return null;
    }

    public static Condition eq(String propertyName, Object value) {
        return null;
    }

    public static Condition ge(String propertyName, Object value) {
        return null;
    }

    public static Condition lt(String propertyName, Object value) {
        return null;
    }

    public static Condition le(String propertyName, Object value) {
        return null;
    }

    public static Condition isNotNull(String propertyName) {
        return null;
    }

    public static Condition like(String propertyName, String value, MatchMode matchMode) {
        return null;
    }

    public void ignoreCase() {

    }
}
