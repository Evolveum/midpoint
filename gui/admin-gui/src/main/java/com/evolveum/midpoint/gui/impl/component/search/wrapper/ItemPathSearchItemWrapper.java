/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.ItemPathSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathSearchItemWrapper extends PropertySearchItemWrapper<ItemPathType> {

    public ItemPathSearchItemWrapper(ItemPath path) {
        super(path);
    }

    @Override
    public Class<ItemPathSearchItemPanel> getSearchItemPanelClass() {
        return ItemPathSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ItemPathType> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        ItemPathType itemPath = getValue().getValue();
        return PrismContext.get().queryFor(type)
                    .item(getPath()).eq(itemPath).buildFilter();
    }
}
