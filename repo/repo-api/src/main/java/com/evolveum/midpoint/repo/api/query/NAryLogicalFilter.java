/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
