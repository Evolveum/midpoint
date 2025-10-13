/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.panel.ChoicesSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;

public class ChoicesSearchItemWrapper<T extends Serializable> extends PropertySearchItemWrapper<T> {

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
