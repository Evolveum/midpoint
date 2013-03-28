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

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class AndRestriction extends NaryLogicalRestriction<AndFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        return (filter instanceof AndFilter);
    }

    @Override
    public Criterion interpret(AndFilter filter) throws QueryException {
        validateFilter(filter);
        Conjunction conjunction = Restrictions.conjunction();
        updateJunction(filter.getCondition(), conjunction);

        return conjunction;
    }

    @Override
    public AndRestriction cloneInstance() {
        return new AndRestriction();
    }
}
