/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;

public abstract class PropertySearchItemPanel<P extends PropertySearchItemWrapper> extends SingleSearchItemPanel<P> {

    private static final long serialVersionUID = 1L;

    protected static final String ID_SEARCH_ITEM_FIELD = "searchItemField";

    public PropertySearchItemPanel(String id, IModel<P> model) {
        super(id, model);
    }
}
