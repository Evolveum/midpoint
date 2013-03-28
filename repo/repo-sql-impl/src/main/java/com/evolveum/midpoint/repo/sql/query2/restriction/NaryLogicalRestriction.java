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

import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class NaryLogicalRestriction<T extends NaryLogicalFilter> extends LogicalRestriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(NaryLogicalRestriction.class);
    private List<Restriction> restrictions;

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (filter instanceof NaryLogicalFilter) {
            return true;
        }

        return false;
    }

    public List<Restriction> getRestrictions() {
        if (restrictions == null) {
            restrictions = new ArrayList<Restriction>();
        }
        return restrictions;
    }

    protected void validateFilter(NaryLogicalFilter filter) throws QueryException {
        if (filter.getCondition() == null || filter.getCondition().isEmpty()) {
            LOGGER.trace("NaryLogicalFilter filter must have at least two conditions in it. " +
                    "Removing logical filter and processing simple condition.");
            throw new QueryException("NaryLogicalFilter filter '" + filter.dump()
                    + "' must have at least two conditions in it. Removing logical filter and processing simple condition.");
        }
    }

    protected Junction updateJunction(List<? extends ObjectFilter> conditions, Junction junction)
            throws QueryException {

        QueryContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();

        for (ObjectFilter condition : conditions) {
            Restriction restriction = interpreter.findAndCreateRestriction(condition, context, this, getQuery());
            Criterion criterion = restriction.interpret(condition);
            junction.add(criterion);
        }

        return junction;
    }
}
