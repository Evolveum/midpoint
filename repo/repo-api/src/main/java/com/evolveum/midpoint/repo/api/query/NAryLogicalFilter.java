/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.query;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public class NAryLogicalFilter extends LogicalFilter {

    private List<QueryFilter> filters;
    private NAryLogicalFilterType filterType;

    public NAryLogicalFilter() {
    }

    public NAryLogicalFilter(NAryLogicalFilterType filterType, QueryFilter... filters) {
        setFilterType(filterType);
        setFilters(Arrays.asList(filters));
    }

    public List<QueryFilter> getFilters() {
        return filters;
    }

    public NAryLogicalFilterType getFilterType() {
        return filterType;
    }

    public void setFilters(List<QueryFilter> filters) {
        Validate.notNull(filters, "Filter list must not be null.");
        Validate.isTrue(filters.size() >= 2, "You must use two or more filters.");
        this.filters = filters;
    }

    public void setFilterType(NAryLogicalFilterType filterType) {
        Validate.notNull(filterType, "Filter type must not be null.");
        this.filterType = filterType;
    }

    @Override
    public void toDOM(Element parent) {
        //todo implement
    }
}
