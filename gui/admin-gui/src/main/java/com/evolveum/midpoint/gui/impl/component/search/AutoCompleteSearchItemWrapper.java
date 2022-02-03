/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;


public class AutoCompleteSearchItemWrapper extends PropertySearchItemWrapper {

    private LookupTableType lookupTable;

    public AutoCompleteSearchItemWrapper(SearchItemType searchItem, LookupTableType lookupTable) {
        super(searchItem);
        this.lookupTable = lookupTable;
    }

    public Class<AutoCompleteSearchItemPanel> getSearchItemPanelClass() {
        return AutoCompleteSearchItemPanel.class;
    }

    public LookupTableType getLookupTable() {
        return lookupTable;
    }

    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }


}
