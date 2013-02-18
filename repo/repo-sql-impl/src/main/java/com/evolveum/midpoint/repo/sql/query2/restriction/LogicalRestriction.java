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

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class LogicalRestriction<T extends LogicalFilter> extends Restriction<T> {

    public LogicalRestriction(QueryContext context, ObjectQuery query, T filter) {
        super(context, query, filter);
    }

    public LogicalRestriction(Restriction parent, QueryContext context, ObjectQuery query, T filter) {
        super(parent, context, query, filter);
    }

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (filter instanceof LogicalFilter) {
            return true;
        }

        return false;
    }

    @Override
    public Criterion interpret() {
//        switch (operation) {
//            case NOT:
//                return Restrictions.not()
//
//            NotFilter notFilter = (NotFilter) filterPart;
//            if (notFilter.getFilter() == null) {
//                throw new QueryException("Logical filter (not) must have sepcified condition.");
//            }
//            return getInterpreter().interpret(notFilter.getFilter(), newPushNot);
//            case AND:
//                Conjunction conjunction = Restrictions.conjunction();
//                AndFilter andFilter = (AndFilter) filterPart;
//                updateJunction(andFilter.getCondition(),  conjunction);
//                return conjunction;
//            case OR:
//                Disjunction disjunction = Restrictions.disjunction();
//                OrFilter orFilter = (OrFilter) filterPart;
//                updateJunction(orFilter.getCondition(),  disjunction);
//
//                return disjunction;
//        }
        return null;
    }

//    private Junction updateJunction(List<? extends ObjectFilter> conditions, Junction junction)
//            throws QueryException {
//        for (ObjectFilter condition : conditions) {
//            junction.add(getInterpreter().interpret(condition));
//        }
//
//        return junction;
//    }
}
