/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
