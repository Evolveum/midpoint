/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ItemPathSearchItemWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class ItemPathSearchItemPanel extends SingleSearchItemPanel<ItemPathSearchItemWrapper> {

    public ItemPathSearchItemPanel(String id, IModel<ItemPathSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component getSearchItemFieldPanel() {
        Component component = super.getSearchItemFieldPanel();
        if (component != null) {
            return component.get(PopoverSearchPanel.ID_TEXT_FIELD);
        }
        return null;
    }

    @Override
    protected Component initSearchItemField(String id) {
        return new ItemPathSearchPanel(id, new PropertyModel(getModel(), ItemPathSearchItemWrapper.F_VALUE));

    }

}
