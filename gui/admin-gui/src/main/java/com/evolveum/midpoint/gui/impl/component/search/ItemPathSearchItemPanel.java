/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.web.component.search.ItemPathSearchPanel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class ItemPathSearchItemPanel extends AbstractSearchItemPanel<ItemPathSearchItemWrapper> {

    public ItemPathSearchItemPanel(String id, IModel<ItemPathSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        return new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD, new PropertyModel(getModel(), ItemPathSearchItemWrapper.F_VALUE));

    }

}
