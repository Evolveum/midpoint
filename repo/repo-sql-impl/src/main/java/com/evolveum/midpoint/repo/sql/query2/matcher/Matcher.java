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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.matcher;

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.hibernate.criterion.Criterion;
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
            case SUBSTRING:
                criterion = Restrictions.like(propertyName, "%" + value + "%");
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
