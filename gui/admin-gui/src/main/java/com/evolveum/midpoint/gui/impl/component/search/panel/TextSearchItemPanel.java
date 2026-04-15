/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.TextSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class TextSearchItemPanel extends PropertySearchItemPanel<TextSearchItemWrapper> {

    public TextSearchItemPanel(String id, IModel<TextSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        String valueEnumerationRefOid = getModelObject().getValueEnumerationRefOid();
        if (valueEnumerationRefOid != null) {
            return createAutoCompetePanel(id, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE), valueEnumerationRefOid);
        } else {
            return new TextPanel<String>(id, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE));
        }
    }
}
