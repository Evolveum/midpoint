/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
