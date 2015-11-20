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

package com.evolveum.midpoint.repo.sql.query2.matcher;

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

/**
 * @author lazyman
 */
public abstract class Matcher<T> {

    /**
     * Create hibernate {@link Criterion} based on matcher defined in filter.
     *
     * @param operation
     * @param propertyName
     * @param value
     * @param matcher      Now type of {@link String}, but will be updated to {@link javax.xml.namespace.QName}
     *                     type after query-api update
     * @return
     * @throws QueryException
     */
    public abstract Condition match(ItemRestrictionOperation operation, String propertyName, T value, String matcher)
            throws QueryException;

    protected Condition basicMatch(ItemRestrictionOperation operation, String propertyName, Object value,
                                   boolean ignoreCase) throws QueryException {
        Condition condition;
        switch (operation) {
            case EQ:
                if (value == null) {
                    condition = Condition.isNull(propertyName);
                } else {
                    condition = Condition.eq(propertyName, value);
                }
                break;
            case GT:
                condition = Condition.gt(propertyName, value);
                break;
            case GE:
                condition = Condition.ge(propertyName, value);
                break;
            case LT:
                condition = Condition.lt(propertyName, value);
                break;
            case LE:
                condition = Condition.le(propertyName, value);
                break;
            case NOT_NULL:
                condition = Condition.isNotNull(propertyName);
                break;
            case NULL:
                condition = Condition.isNull(propertyName);
                break;
            case STARTS_WITH:
                condition = Condition.like(propertyName, (String) value, MatchMode.START);
                break;
            case ENDS_WITH:
                condition = Condition.like(propertyName, (String) value, MatchMode.END);
                break;
            case SUBSTRING:
                condition = Condition.like(propertyName, (String) value, MatchMode.ANYWHERE);
                break;
            default:
                throw new QueryException("Unknown operation '" + operation + "'.");
        }

        if (ignoreCase && (value instanceof String)) {
            condition.ignoreCase();
        }

        return condition;
    }
}
