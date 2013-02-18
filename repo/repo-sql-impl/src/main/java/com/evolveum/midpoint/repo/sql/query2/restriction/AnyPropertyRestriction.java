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

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends ItemRestriction {


    public AnyPropertyRestriction(QueryContext context, ObjectQuery query, ValueFilter filter) {
        super(context, query, filter);
    }

    public AnyPropertyRestriction(Restriction parent, QueryContext context, ObjectQuery query, ValueFilter filter) {
        super(parent, context, query, filter);
    }
}
