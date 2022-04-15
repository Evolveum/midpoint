/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public class TenantSearchItemWrapper extends AbstractRoleSearchItemWrapper {

    public TenantSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public boolean isVisible() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public Class<TenantSearchItemPanel> getSearchItemPanelClass() {
        return TenantSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<SearchBoxScopeType> getDefaultValue() {
        return new SearchValue<>(SearchBoxScopeType.ONE_LEVEL);
    }

    @Override
    public String getName() {
//        if (getSearchConfig().getConfig().getTenantConfiguration() == null
//                || getSearchConfig().getConfig().getTenantConfiguration().getDisplay() == null) {
            return "abstractRoleMemberPanel.tenant";
//        }
//        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getTenantConfiguration().getDisplay().getLabel());
    }

    @Override
    public String getHelp() {
//        if (getSearchConfig().getConfig().getTenantConfiguration() == null
//                || getSearchConfig().getConfig().getTenantConfiguration().getDisplay() == null) {
            return "";
//        }
//        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getTenantConfiguration().getDisplay().getHelp());
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    public boolean isApplyFilter() {
        //todo check
        return SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getScope());
    }

}
