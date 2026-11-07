/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * State of the search text and the filters of an attribute table.
 *
 * @param <T> row type of the table
 */
public class AttributesFilterState<T> implements Serializable {

    /**
     * Text typed in the search field.
     */
    private String searchText;

    /**
     * Filters offered next to the search field, each keeping only the rows meeting its condition while it is on.
     */
    private final List<AttributesFilter<T>> filters;

    @SafeVarargs
    public AttributesFilterState(AttributesFilter<T>... filters) {
        this.filters = List.of(filters);
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public List<AttributesFilter<T>> getFilters() {
        return filters;
    }

    /**
     * Tells whether the row passes the search text and all applied filters.
     *
     * @param attributeName the displayed name of the attribute in the row
     * @param row the row of the table
     * @return true if the row should be shown
     */
    public boolean matches(String attributeName, T row) {
        return matchesSearchText(attributeName)
                && filters.stream().allMatch(filter -> !filter.isApplied() || filter.condition().test(row));
    }

    /**
     * Tells whether any filter is applied, so a change of a row can change which rows are shown.
     *
     * @return true if at least one filter is applied
     */
    public boolean isAnyFilterApplied() {
        return filters.stream().anyMatch(AttributesFilter::isApplied);
    }

    private boolean matchesSearchText(String attributeName) {
        String text = StringUtils.trim(searchText);
        return StringUtils.isEmpty(text) || StringUtils.containsIgnoreCase(attributeName, text);
    }
}
