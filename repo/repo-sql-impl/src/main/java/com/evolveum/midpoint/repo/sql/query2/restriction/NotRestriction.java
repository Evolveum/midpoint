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

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class NotRestriction extends UnaryLogicalRestriction<NotFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (!super.canHandle(filter)) {
            return false;
        }

        return (filter instanceof NotFilter);
    }


    @Override
    public Criterion interpret(NotFilter filter, ObjectQuery query, QueryContext context, Restriction parent)
            throws QueryException {
        if (!isFilterValid(filter)) {
            //todo what to do here? probably skip and if there is one sub filter, otherwise exception...or maybe skip it all
        }

        QueryInterpreter interpreter = context.getInterpreter();
        Restriction restriction = interpreter.findAndCreateRestriction(filter.getFilter());
        Criterion criterion = restriction.interpret(filter.getFilter(), query, context, this);

        return Restrictions.not(criterion);
    }
}
