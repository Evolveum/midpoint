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
import com.evolveum.midpoint.prism.query.UnaryLogicalFilter;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class UnaryLogicalRestriction<T extends UnaryLogicalFilter> extends LogicalRestriction<T> {

    private LogicalOperation operation;

    public UnaryLogicalRestriction(QueryContext context, ObjectQuery query, T filter) {
        super(context, query, filter);
    }

    public UnaryLogicalRestriction(Restriction parent, QueryContext context, ObjectQuery query, T filter) {
        super(parent, context, query, filter);
    }

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (filter instanceof UnaryLogicalFilter) {
            return true;
        }

        return false;
    }

    public LogicalOperation getOperation() {
        return operation;
    }

    @Override
    public Criterion interpret() {
        return super.interpret();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
