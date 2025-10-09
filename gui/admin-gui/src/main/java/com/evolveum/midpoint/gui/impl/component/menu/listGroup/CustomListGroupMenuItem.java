/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.menu.listGroup;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class CustomListGroupMenuItem<T extends Serializable> extends ListGroupMenuItem<T> {

    public CustomListGroupMenuItem() {
    }

    public CustomListGroupMenuItem(String label) {
        super(label);
    }

    public CustomListGroupMenuItem(String iconCss, String label) {
        super(iconCss, label);
    }

    public abstract Component createMenuItemPanel(String id, IModel<ListGroupMenuItem<T>> model, SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<T>> oncClickHandler);
}
