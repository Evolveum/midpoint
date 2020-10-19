/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SearchItemDefinition implements Serializable {

    private ItemPath path;
    private ItemDefinition def;
    private SearchItemType predefinedFilter;
    private PolyStringType displayName;
    private List<?> allowedValues;

    public SearchItemDefinition(ItemPath path, ItemDefinition def, List<?> allowedValues) {
        this.path = path;
        this.def = def;
        this.allowedValues = allowedValues;
    }

    public SearchItemDefinition(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public ItemPath getPath() {
        return path;
    }

    public ItemDefinition getDef() {
        return def;
    }

    public List<?> getAllowedValues() {
        return allowedValues;
    }

    public SearchItemType getPredefinedFilter() {
        return predefinedFilter;
    }

    public void setPredefinedFilter(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public PolyStringType getDisplayName() {
        return displayName;
    }

    public void setDisplayName(PolyStringType displayName) {
        this.displayName = displayName;
    }
}
