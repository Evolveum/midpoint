/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import java.util.List;

public class ChoicesSearchItemWrapper<T> extends PropertySearchItemWrapper {

    List<DisplayableValue<T>> availableValues;

    public ChoicesSearchItemWrapper(SearchItemType searchItem, List<DisplayableValue<T>> availableValues) {
        super(searchItem);
        this.availableValues = availableValues;
    }

    public Class<ChoicesSearchItemPanel> getSearchItemPanelClass() {
        return ChoicesSearchItemPanel.class;
    }

    public List<DisplayableValue<T>> getAvailableValues() {
        return availableValues;
    }

    public DisplayableValue<T> getDefaultValue() {
        return new SearchValue();
    }

}
