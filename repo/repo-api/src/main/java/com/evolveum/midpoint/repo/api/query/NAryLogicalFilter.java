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

import javax.xml.namespace.QName;
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
