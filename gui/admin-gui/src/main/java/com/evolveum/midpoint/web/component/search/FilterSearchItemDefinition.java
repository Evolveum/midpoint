/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FilterSearchItemDefinition extends AbstractSearchItemDefinition {

    private SearchItemType predefinedFilter;

    public FilterSearchItemDefinition(@NotNull SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
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
    public int hashCode() {
        return Objects.hash(predefinedFilter);
    }

}
