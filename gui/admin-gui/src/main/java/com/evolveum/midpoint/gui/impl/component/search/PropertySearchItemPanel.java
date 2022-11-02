/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import org.apache.wicket.model.IModel;

public abstract class PropertySearchItemPanel<P extends PropertySearchItemWrapper> extends SingleSearchItemPanel<P> {

    private static final long serialVersionUID = 1L;

    protected static final String ID_SEARCH_ITEM_FIELD = "searchItemField";

    public PropertySearchItemPanel(String id, IModel<P> model) {
        super(id, model);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        PropertySearchItemWrapper item = getModelObject();
//        if (!item.isEditWhenVisible()) {
//            return;
//        }
//        item.setEditWhenVisible(false);
    }
}
