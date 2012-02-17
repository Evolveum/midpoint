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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.api.query;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class NotLogicalFilter extends LogicalFilter {

    private QueryFilter filter;

    public NotLogicalFilter(QueryFilter filter) {
        setFilter(filter);
    }

    public QueryFilter getFilter() {
        return filter;
    }

    public void setFilter(QueryFilter filter) {
        Validate.notNull(filter, "Filter must not be null.");
        this.filter = filter;
    }

    @Override
    public void toDOM(Element parent) {
        //todo implement
    }
}
