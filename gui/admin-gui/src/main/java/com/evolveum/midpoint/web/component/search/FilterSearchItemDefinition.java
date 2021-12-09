/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.io.Filter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class FilterSearchItemDefinition extends AbstractSearchItemDefinition {

    private SearchItemType predefinedFilter;
    private DisplayableValue<? extends Serializable> input = new SearchValue<>();

    public FilterSearchItemDefinition(@NotNull SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public SearchItemType getPredefinedFilter() {
        return predefinedFilter;
    }

    public DisplayableValue<? extends Serializable> getInput() {
        return input;
    }

    public void setInput(DisplayableValue<? extends Serializable> input) {
        this.input = input;
    }

    @Override
    public boolean isVisibleByDefault() {
        return !Boolean.FALSE.equals(predefinedFilter.isVisibleByDefault());
    }

    @Override
    public String getName() {
        if (predefinedFilter.getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(predefinedFilter.getDisplayName());
        }
        return ""; //todo what if no displayName is configured for filter?
    }

    @Override
    public String getHelp(){
        if (StringUtils.isNotBlank(predefinedFilter.getDescription())) {
            return predefinedFilter.getDescription();
        }
        return "";
    }

    @Override
    public FilterSearchItem createSearchItem() {
        return new FilterSearchItem(null, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predefinedFilter);
    }

}
