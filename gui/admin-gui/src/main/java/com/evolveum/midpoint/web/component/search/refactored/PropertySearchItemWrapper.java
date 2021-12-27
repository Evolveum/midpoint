/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

public class PropertySearchItemWrapper<T extends Serializable> extends AbstractSearchItemWrapper<T> {

    SearchItemType searchItem;

    public PropertySearchItemWrapper (SearchItemType searchItem) {
        this.searchItem = searchItem;
    }

    @Override
    public Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass() {
        return null;
    }

    @Override
    public DisplayableValue<T> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public String getName() {
        if (searchItem.getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(searchItem.getDisplayName());
        }
        return "";
//        String key = getDefinition().getDef().getDisplayName();
//        if (StringUtils.isEmpty(key)) {
//            key = getSearch().getTypeClass().getSimpleName() + '.' + getDefinition().getDef().getItemName().getLocalPart();
//        }
//
//        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
//        if (nameModel != null) {
//            if (StringUtils.isNotEmpty(nameModel.getString())) {
//                return nameModel.getString();
//            }
//        }
//        String name = getDefinition().getDef().getDisplayName();
//        if (StringUtils.isNotEmpty(name)) {
//            return name;
//        }
//
//        return getDefinition().getDef().getItemName().getLocalPart();
//        if (getDisplayName() != null){
//            return WebComponentUtil.getTranslatedPolyString(getDisplayName());
//        }

//        if (getDef() != null && StringUtils.isNotEmpty(getDef().getDisplayName())) {
//            return PageBase.createStringResourceStatic(null, getDef().getDisplayName()).getString();
//        }
//        return WebComponentUtil.getItemDefinitionDisplayNameOrName(getDef(), null);
    }

    public SearchItemType getSearchItem() {
        return searchItem;
    }

    public void setSearchItem(SearchItemType searchItem) {
        this.searchItem = searchItem;
    }

    @Override
    public String getHelp() {
        return "";
    }

    public String getTitle() {
        return "";
    }

}
