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

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class ItemRestriction<T extends ValueFilter> extends Restriction<T> {

    public ItemRestriction(QueryContext context, ObjectQuery query, T filter) {
        super(context, query, filter);
    }

    public ItemRestriction(Restriction parent, QueryContext context, ObjectQuery query, T filter) {
        super(parent, context, query, filter);
    }

    protected String createPropertyName() {
        //todo implement
        return null;
    }

    protected Object createComparableRealValue() {
        //todo implement
        return null;
    }

    @Override
    public Criterion interpret() {
        //todo implement
        return null;
    }

    @Override
    public boolean canHandle(ObjectFilter filter) {
        //todo implement
        return false;
    }

    protected Criterion createCriterion(RestrictionOperation operation) throws QueryException {
        String propertyName = createPropertyName();
        Object value = (operation == RestrictionOperation.NOT_NULL
                || operation == RestrictionOperation.NULL) ?
                null : createComparableRealValue();

        switch (operation) {
            case EQ:
                return Restrictions.eq(propertyName, value);
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
            case SUBSTRING_IGNORE_CASE:
                return Restrictions.like(propertyName, "%" + value + "%").ignoreCase();
        }

        throw new QueryException("Unknown operation.");
    }
}
