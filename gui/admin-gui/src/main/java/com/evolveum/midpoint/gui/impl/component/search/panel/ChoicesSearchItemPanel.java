/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.ChoicesSearchItemWrapper;

public class ChoicesSearchItemPanel<T extends Serializable> extends PropertySearchItemPanel<ChoicesSearchItemWrapper<T>> {

    public ChoicesSearchItemPanel(String id, IModel<ChoicesSearchItemWrapper<T>> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField(String id) {
        return WebComponentUtil.createDropDownChoices(id,
                new PropertyModel<>(getModel(), ChoicesSearchItemWrapper.F_DISPLAYABLE_VALUE),
                () -> getModelObject().getAvailableValues(),
                allowNull());
    }

    private boolean allowNull() {
        if (getModel() != null && getModelObject() != null) {
            return getModelObject().allowNull();
        }
        return true;
    }
}
