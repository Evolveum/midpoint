/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.impl.component.search.panel.ScopeSearchItemPanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScopeSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public class ScopeSearchItemWrapper extends AbstractSearchItemWrapper<SearchBoxScopeType> {

    private ScopeSearchItemConfigurationType scopeConfig;

    public ScopeSearchItemWrapper(ScopeSearchItemConfigurationType scopeConfig) {
        super();
        this.scopeConfig = scopeConfig;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    @Override
    public Class<ScopeSearchItemPanel> getSearchItemPanelClass() {
        return ScopeSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<SearchBoxScopeType> getDefaultValue() {
        return new SearchValue<>(scopeConfig.getDefaultValue());
    }

    @Override
    public String getName() {
        return "abstractRoleMemberPanel.searchScope";
    }

    @Override
    public String getHelp() {
        return "abstractRoleMemberPanel.searchScope.tooltip";
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

//    @Override
//    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
//        return SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
//    }

}
