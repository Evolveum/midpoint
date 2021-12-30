/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public abstract class AbstractRoleSearchItemWrapper extends AbstractSearchItemWrapper {

    private SearchConfigurationWrapper searchConfig;

    public static final String F_SEARCH_CONFIG = "searchConfig";

    public AbstractRoleSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        this.searchConfig = searchConfig;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase) {
        return null;
    }

    public SearchConfigurationWrapper getSearchConfig() {
        return searchConfig;
    }

//    public void setSearchConfig(SearchConfigurationWrapper searchConfig) {
//        this.searchConfig = searchConfig;
//    }
}
