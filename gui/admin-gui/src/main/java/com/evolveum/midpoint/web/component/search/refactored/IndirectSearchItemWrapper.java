/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public class IndirectSearchItemWrapper extends AbstractRoleSearchItemWrapper {

    public IndirectSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return getSearchConfig().isSearchScope(SearchBoxScopeType.SUBTREE);
    }

    public boolean isVisible() {
        return getSearchConfig().getConfig().getRelationConfiguration() != null
                && getSearchConfig().getConfig().getRelationConfiguration().getSupportedRelations() != null
                && !getSearchConfig().isSearchScope(SearchBoxScopeType.SUBTREE);
    }

    @Override
    public Class<IndirectSearchItemPanel> getSearchItemPanelClass() {
        return IndirectSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<Boolean> getDefaultValue() {
        return new SearchValue<>(Boolean.FALSE);
    }

    @Override
    public String getName() {
        if (getSearchConfig().getConfig().getIndirectConfiguration() == null
                || getSearchConfig().getConfig().getIndirectConfiguration().getDisplay() == null) {
            return "";
        }
        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getIndirectConfiguration().getDisplay().getLabel());
    }

    @Override
    public String getHelp() {
        if (getSearchConfig().getConfig().getIndirectConfiguration() == null
                || getSearchConfig().getConfig().getIndirectConfiguration().getDisplay() == null) {
            return "";
        }
        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getIndirectConfiguration().getDisplay().getHelp());
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return true;
    }
}
