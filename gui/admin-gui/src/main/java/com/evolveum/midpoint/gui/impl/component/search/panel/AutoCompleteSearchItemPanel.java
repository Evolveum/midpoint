/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AutoCompleteSearchItemWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AutoCompleteSearchItemPanel extends SingleSearchItemPanel<AutoCompleteSearchItemWrapper> {

    public AutoCompleteSearchItemPanel(String id, IModel<AutoCompleteSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField(String id) {
        return createAutoCompetePanel(id, new PropertyModel<>(getModel(), AutoCompleteSearchItemWrapper.F_VALUE),
                getModelObject().getLookupTableOid());
    }

}
