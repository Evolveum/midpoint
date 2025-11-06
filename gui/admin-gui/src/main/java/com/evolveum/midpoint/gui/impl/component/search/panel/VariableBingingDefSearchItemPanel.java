/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.TextSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.VariableBindingDefSearchItemWrapper;

import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class VariableBingingDefSearchItemPanel extends SingleSearchItemPanel<VariableBindingDefSearchItemWrapper> {

    public VariableBingingDefSearchItemPanel(String id, IModel<VariableBindingDefSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        return new TextPanel<String>(id, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE));
    }
}
