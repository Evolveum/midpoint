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
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class OrRestriction extends NaryLogicalRestriction<OrFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        return (filter instanceof OrFilter);
    }

    @Override
    public Criterion interpret(OrFilter filter)
            throws QueryException {

        validateFilter(filter);

        Disjunction disjunction = Restrictions.disjunction();
        updateJunction(filter.getCondition(), disjunction);

        return disjunction;
    }

    @Override
    public OrRestriction cloneInstance() {
        return new OrRestriction();
    }
}
