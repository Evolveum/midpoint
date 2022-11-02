/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

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
