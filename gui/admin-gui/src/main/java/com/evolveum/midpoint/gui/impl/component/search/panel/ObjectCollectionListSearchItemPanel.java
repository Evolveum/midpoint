/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectCollectionListSearchItemWrapper;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ObjectCollectionListSearchItemPanel extends SingleSearchItemPanel<ObjectCollectionListSearchItemWrapper> {

    public ObjectCollectionListSearchItemPanel(String id, IModel<ObjectCollectionListSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        return WebComponentUtil.createDropDownChoices(
                id, new PropertyModel(getModel(), "value"),
                Model.ofList(getModelObject().getViewNameList()), true);
    }
}
