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
    private IModel<String> titleModel;

    public abstract IModel<String> getIcon();

    IModel<String> getTitleIconCss() {
        return Model.of();
    }

    protected void setTitleModel(IModel<String> titleModel) {
        this.titleModel = titleModel;
    }

    public IModel<String> getTitle() {
        return titleModel;
    }

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
