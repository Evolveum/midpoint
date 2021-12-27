/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathSearchItemWrapper extends PropertySearchItemWrapper {

    public ItemPathSearchItemWrapper(SearchItemType searchItem) {
        super(searchItem);
    }

    @Override
    public Class<ItemPathSearchItemPanel> getSearchItemPanelClass() {
        return ItemPathSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ItemPathType> getDefaultValue() {
        return null;
    }

}
