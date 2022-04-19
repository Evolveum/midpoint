/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

public class RelationSearchItemWrapper extends AbstractRoleSearchItemWrapper {

    public RelationSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return CollectionUtils.isNotEmpty(getSearchConfig().getSupportedRelations());
    }

    public boolean isVisible() {
        return true;
    }

    @Override
    public Class<RelationSearchItemPanel> getSearchItemPanelClass() {
        return RelationSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<QName> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public String getName() {
//        if (getSearchConfig().getRelationConfiguration().getDisplay() == null) {
            return "relationDropDownChoicePanel.relation";
//        }
//        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getRelationConfiguration().getDisplay().getLabel());
    }

    @Override
    public String getHelp() {
//        if (getSearchConfig().getConfig().getRelationConfiguration() == null
//                || getSearchConfig().getConfig().getRelationConfiguration().getDisplay() == null) {
            return "relationDropDownChoicePanel.tooltip.relation";
//        }
//        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getRelationConfiguration().getDisplay().getHelp());
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return !getSearchConfig().getDefaultScope().equals(SearchBoxScopeType.SUBTREE);
    }
}
