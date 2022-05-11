/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AutoCompleteSearchItemPanel extends AbstractSearchItemPanel<AutoCompleteSearchItemWrapper> {

    public AutoCompleteSearchItemPanel(String id, IModel<AutoCompleteSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField() {
        return createAutoCompetePanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), AutoCompleteSearchItemWrapper.F_VALUE),
                getModelObject().getLookupTable());
    }

}
