/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSearchItemConfigurationType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class ObjectTypeSearchItemWrapper extends AbstractSearchItemWrapper<QName> {

    ObjectTypeSearchItemConfigurationType config;

    public ObjectTypeSearchItemWrapper(ObjectTypeSearchItemConfigurationType config) {
        this.config = config;
    }

    public Class<ObjectTypeSearchItemPanel> getSearchItemPanelClass() {
        return ObjectTypeSearchItemPanel.class;
    }

    public List<DisplayableValue<QName>> getAvailableValues() {
        List<DisplayableValue<QName>> availableValues = new ArrayList<>();
        config.getSupportedTypes().forEach(type -> availableValues.add(new SearchValue(type)));
        return availableValues;
    }

    public DisplayableValue<QName> getDefaultValue() {
        return new SearchValue();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getHelp() {
        return "";
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter() {
        return true;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        return PrismContext.get().queryFor((Class<? extends Containerable>) WebComponentUtil.qnameToClass(PrismContext.get(), config.getDefaultValue()))
                .buildFilter();
    }
}
