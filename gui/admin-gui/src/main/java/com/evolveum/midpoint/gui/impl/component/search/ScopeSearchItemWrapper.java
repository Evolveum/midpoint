/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public class ScopeSearchItemWrapper extends AbstractRoleSearchItemWrapper {

    public ScopeSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
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
        return new SearchValue<>(SearchBoxScopeType.ONE_LEVEL);
    }

    @Override
    public DisplayableValue<SearchBoxScopeType> getValue() {
        return new SearchValue<>(getSearchConfig().getDefaultScope());
    }

    @Override
    protected String getNameResourceKey() {
        return "abstractRoleMemberPanel.searchScope";
    }

    @Override
    protected String getHelpResourceKey() {
        return "abstractRoleMemberPanel.searchScope.tooltip";
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
    }

}
