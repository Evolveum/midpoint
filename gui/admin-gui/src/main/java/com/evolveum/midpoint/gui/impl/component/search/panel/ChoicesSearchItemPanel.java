/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
