/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ChoicesSearchItemPanel<T> extends PropertySearchItemPanel<ChoicesSearchItemWrapper> {

    public ChoicesSearchItemPanel(String id, IModel<ChoicesSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField() {
        return WebComponentUtil.createDropDownChoices(
                ID_SEARCH_ITEM_FIELD, new PropertyModel(getModel(), ChoicesSearchItemWrapper.F_DISPLAYABLE_VALUE), Model.ofList(getModelObject().getAvailableValues()), allowNull(), getPageBase());
    }

    private boolean allowNull() {
        if (getModel() != null && getModelObject() != null) {
            return getModelObject().allowNull();
        }
        return true;
    }

}
