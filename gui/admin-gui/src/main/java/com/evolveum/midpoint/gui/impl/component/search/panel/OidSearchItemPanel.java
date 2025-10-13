/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.OidSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class OidSearchItemPanel extends SingleSearchItemPanel<OidSearchItemWrapper> {

    public OidSearchItemPanel(String id, IModel<OidSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        TextPanel<String> inputPanel = new TextPanel<String>(id,  new PropertyModel(getModel(), OidSearchItemWrapper.F_VALUE));
        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px; max-width: 400px !important;"));
        return inputPanel;
    }

}
