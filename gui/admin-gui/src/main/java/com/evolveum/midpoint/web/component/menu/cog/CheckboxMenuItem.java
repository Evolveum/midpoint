/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

public class CheckboxMenuItem extends InlineMenuItem {

    private IModel<Boolean> checkBoxModel;

    public CheckboxMenuItem(IModel<String> label, IModel<Boolean> checkBoxModel) {
        super(label);
        this.checkBoxModel = checkBoxModel;
    }

    @Override
    public InlineMenuItemAction initAction() {
        return null;
    }

    public IModel<Boolean> getCheckBoxModel() {
        return checkBoxModel;
    }
}
