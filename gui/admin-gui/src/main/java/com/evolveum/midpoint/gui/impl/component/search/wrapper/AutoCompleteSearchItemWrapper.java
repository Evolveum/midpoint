/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.impl.component.search.panel.AutoCompleteSearchItemPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

public class AutoCompleteSearchItemWrapper extends PropertySearchItemWrapper<String> {

    private String lookupTableOid;

    public AutoCompleteSearchItemWrapper(ItemPath path, String lookupTableOid) {
        super(path);
        this.lookupTableOid = lookupTableOid;
    }

    public Class<AutoCompleteSearchItemPanel> getSearchItemPanelClass() {
        return AutoCompleteSearchItemPanel.class;
    }

    public String getLookupTableOid() {
        return lookupTableOid;
    }

    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }


}
