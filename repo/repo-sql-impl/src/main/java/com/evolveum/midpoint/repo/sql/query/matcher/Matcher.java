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

package com.evolveum.midpoint.repo.sql.query.matcher;

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.restriction.ItemRestrictionOperation;
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
    public abstract Criterion match(ItemRestrictionOperation operation, String propertyName, T value, String matcher)
            throws QueryException;

    protected Criterion basicMatch(ItemRestrictionOperation operation, String propertyName, Object value,
                                   boolean ignoreCase) throws QueryException {
        Criterion criterion;
        switch (operation) {
            case EQ:
                if (value == null) {
                    criterion = Restrictions.isNull(propertyName);
                } else {
                    criterion = Restrictions.eq(propertyName, value);
                }
                break;
            case GT:
                criterion = Restrictions.gt(propertyName, value);
                break;
            case GE:
                criterion = Restrictions.ge(propertyName, value);
                break;
            case LT:
                criterion = Restrictions.lt(propertyName, value);
                break;
            case LE:
                criterion = Restrictions.le(propertyName, value);
                break;
            case NOT_NULL:
                criterion = Restrictions.isNotNull(propertyName);
                break;
            case NULL:
                criterion = Restrictions.isNull(propertyName);
                break;
            case STARTS_WITH:
                criterion = Restrictions.like(propertyName, (String) value, MatchMode.START);
                break;
            case ENDS_WITH:
                criterion = Restrictions.like(propertyName, (String) value, MatchMode.END);
                break;
            case SUBSTRING:
                criterion = Restrictions.like(propertyName, (String) value, MatchMode.ANYWHERE);
                break;
            default:
                throw new QueryException("Unknown operation '" + operation + "'.");
        }

        if (ignoreCase && (value instanceof String) && (criterion instanceof SimpleExpression)) {
            criterion = ((SimpleExpression) criterion).ignoreCase();
        }

        return criterion;
    }
}
