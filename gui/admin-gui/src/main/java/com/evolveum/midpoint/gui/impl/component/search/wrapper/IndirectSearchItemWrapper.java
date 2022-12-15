/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.IndirectSearchItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IndirectSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

public class IndirectSearchItemWrapper extends AbstractSearchItemWrapper<Boolean> {

    private IndirectSearchItemConfigurationType indirectConfig;
    public IndirectSearchItemWrapper(IndirectSearchItemConfigurationType indirectConfig) {
        super();
        this.indirectConfig = indirectConfig;
    }

//    @Override
//    public boolean isEnabled() {
//        return getSearchConfig().isSearchScope(SearchBoxScopeType.SUBTREE);
//    }

    //TODO in panel!
//    public boolean isVisible() {
//        return CollectionUtils.isNotEmpty(getSearchConfig().getSupportedRelations())
//                && !getSearchConfig().isSearchScope(SearchBoxScopeType.SUBTREE);
//    }

    @Override
    public Class<IndirectSearchItemPanel> getSearchItemPanelClass() {
        return IndirectSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<Boolean> getDefaultValue() {
        return new SearchValue<>(indirectConfig.isIndirect());
    }

    @Override
    public String getName() {
        return "abstractRoleMemberPanel.indirectMembers";
    }

    @Override
    public String getHelp() {
        return "abstractRoleMemberPanel.indirectMembers.tooltip";
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return isVisible();
    }
}
