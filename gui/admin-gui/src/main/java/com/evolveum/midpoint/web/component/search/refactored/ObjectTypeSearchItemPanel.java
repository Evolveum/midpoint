/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ObjectTypeSearchItemPanel<T> extends AbstractSearchItemPanel<ObjectTypeSearchItemWrapper> {

    public ObjectTypeSearchItemPanel(String id, IModel<ObjectTypeSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField() {
        return WebComponentUtil.createDropDownChoices(
                ID_SEARCH_ITEM_FIELD, new PropertyModel(getModel(), ObjectTypeSearchItemWrapper.F_DISPLAYABLE_VALUE), Model.ofList(getModelObject().getAvailableValues()), true, getPageBase());
    }

}
