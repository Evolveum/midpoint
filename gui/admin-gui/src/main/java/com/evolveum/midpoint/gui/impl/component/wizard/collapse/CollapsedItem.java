/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

public abstract class CollapsedItem implements Serializable {

    private boolean selected;

    public abstract IModel<String> getIcon();

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int countOfObject() {
        return 0;
    }

    public abstract IModel<String> getTitle();

    public boolean isVisible() {
        return true;
    }

    public abstract Component getPanel(String id, WizardModelWithParentSteps wizardModel);
}
