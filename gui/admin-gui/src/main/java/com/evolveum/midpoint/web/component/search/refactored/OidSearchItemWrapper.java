/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

public class OidSearchItemWrapper extends AbstractSearchItemWrapper {

    @Override
    public Class<OidSearchItemPanel> getSearchItemPanelClass() {
        return OidSearchItemPanel.class;
    }

    @Override
    public String getName() {
        return "SearchPanel.oid";
    }

    @Override
    public String getHelp() {
        return "SearchPanel.oid.help";
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

}
