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

/**
 * @author lazyman
 */
public abstract class Matcher<T> {

    public abstract Criterion match(ItemRestrictionOperation operation, String propertyName, T value, String matcher)
            throws QueryException;

    protected Criterion basicMatch(ItemRestrictionOperation operation, String propertyName, Object value)
            throws QueryException {

        switch (operation) {
            case EQ:
                if (value == null) {
                    return Restrictions.isNull(propertyName);
                } else {
                    return Restrictions.eq(propertyName, value);
                }
            case GT:
                return Restrictions.gt(propertyName, value);
            case GE:
                return Restrictions.ge(propertyName, value);
            case LT:
                return Restrictions.lt(propertyName, value);
            case LE:
                return Restrictions.le(propertyName, value);
            case NOT_NULL:
                return Restrictions.isNotNull(propertyName);
            case NULL:
                return Restrictions.isNull(propertyName);
            case SUBSTRING:
                return Restrictions.like(propertyName, "%" + value + "%");
        }

        throw new QueryException("Unknown operation '" + operation + "'.");
    }
}
