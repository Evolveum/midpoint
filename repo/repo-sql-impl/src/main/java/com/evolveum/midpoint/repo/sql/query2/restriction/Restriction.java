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
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public abstract class Restriction<T extends ObjectFilter> {

    private QueryInterpreter interpreter;
    private Restriction parent;
    private QueryContext context;
    private ObjectQuery query;
    private T filter;

    public Restriction(QueryContext context, ObjectQuery query, T filter) {
        this(null, context, query, filter);
    }

    public Restriction(Restriction parent, QueryContext context, ObjectQuery query, T filter) {
        this.parent = parent;
        this.context = context;
        this.query = query;
        this.filter = filter;
    }

    public Restriction getParent() {
        return parent;
    }

    public QueryContext getContext() {
        return context;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public T getFilter() {
        return filter;
    }

    public QueryInterpreter getInterpreter() {
        return interpreter;
    }

    public abstract Criterion interpret() throws QueryException;

    public abstract boolean canHandle(ObjectFilter filter);
}
