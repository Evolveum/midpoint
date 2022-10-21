/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class ContainerValuePanel extends BasePanel<ContainerValueDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM = "item";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_VALUE = "value";

    public ContainerValuePanel(String id, IModel<ContainerValueDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        add(new ListView<>(ID_ITEM, () -> getModelObject().getItemList()) {

            @Override
            protected void populateItem(ListItem<ContainerItemDto> item) {
                item.add(new Label(ID_ATTRIBUTE, () -> item.getModelObject().getAttribute()));

                Object value = item.getModelObject().getValue();
                if (value instanceof ContainerValueDto) {
                    item.add(new ContainerValuePanel(ID_VALUE, () -> (ContainerValueDto) value));
                } else {
                    // should be String
                    item.add(new Label(ID_VALUE, () -> value));
                }
            }
        });
    }
}
