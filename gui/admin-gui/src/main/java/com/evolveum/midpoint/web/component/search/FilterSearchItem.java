/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * @author honchar
 */
public class FilterSearchItem extends SearchItem {

    private static final long serialVersionUID = 1L;

    public static final String F_APPLY_FILTER = "applyFilter";

    private SearchItemType predefinedFilter;
    private boolean applyFilter;

    public FilterSearchItem(Search search, @NotNull SearchItemType predefinedFilter) {
        super(search);
        Validate.notNull(predefinedFilter, "Filter must not be null.");
        this.predefinedFilter = predefinedFilter;
    }

    @Override
    public String getName(){
        return WebComponentUtil.getTranslatedPolyString(predefinedFilter.getDisplayName());
    }

    @Override
    public Type getType(){
        return Type.FILTER;
    }

    @Override
    protected String getTitle(){
        return getPredefinedFilter().getFilter() != null ? getPredefinedFilter().getFilter().toString() : "";
    }

    public SearchItemType getPredefinedFilter() {
        return predefinedFilter;
    }

    public void setPredefinedFilter(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public boolean isApplyFilter() {
        return applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("search", getSearch())
                .append("predefinedFilter", predefinedFilter)
                .toString();
    }
}
