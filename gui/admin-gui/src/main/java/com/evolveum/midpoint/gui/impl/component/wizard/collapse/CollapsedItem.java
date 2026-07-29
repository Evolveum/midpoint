/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class CollapsedItem<M extends DrawerDescriptor>
        implements Serializable {

    private boolean selected;

    public abstract IModel<String> getIcon();

    IModel<String> getTitleIconCss() {
        return Model.of();
    }

    public abstract IModel<String> getTitle();

    public abstract Component getPanel(String id, M model);

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int countOfObject() {
        return 0;
    }

    public boolean isVisible() {
        return true;
    }
}
