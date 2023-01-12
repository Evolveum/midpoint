/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import javax.xml.namespace.QName;
import java.util.List;

public class ChoicesSearchItemWrapper<T> extends PropertySearchItemWrapper {

    List<DisplayableValue<T>> availableValues;

    public ChoicesSearchItemWrapper(ItemPath path, List<DisplayableValue<T>> availableValues) {
        super(path);
        this.availableValues = availableValues;
    }

    @Override
    public Class<ChoicesSearchItemPanel> getSearchItemPanelClass() {
        return ChoicesSearchItemPanel.class;
    }

    public List<DisplayableValue<T>> getAvailableValues() {
        return availableValues;
    }

    @Override
    public DisplayableValue<T> getDefaultValue() {
        return new SearchValue();
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }
        return PrismContext.get().queryFor(type)
                .item(getPath()).eq(getValue().getValue()).buildFilter();
    }

    public boolean allowNull() {
        return true;
    }
}
