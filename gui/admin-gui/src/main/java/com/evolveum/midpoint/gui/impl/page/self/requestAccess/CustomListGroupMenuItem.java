/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

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

    abstract Component createMenuItemPanel(String id, IModel<ListGroupMenuItem<T>> model, SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<T>> oncClickHandler);
}
