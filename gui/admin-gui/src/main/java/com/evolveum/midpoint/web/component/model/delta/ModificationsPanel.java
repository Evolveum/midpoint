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

public class ModificationsPanel extends BasePanel<DeltaDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_MODIFICATION = "modification";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_VALUE = "value";

    public ModificationsPanel(String id, IModel<DeltaDto> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        add(new ListView<>(ID_MODIFICATION, () -> getModelObject().getModifications()) {
            @Override
            protected void populateItem(ListItem<ModificationDto> item) {
                item.add(new Label(ID_ATTRIBUTE, () -> item.getModelObject().getAttribute()));
                item.add(new Label(ID_CHANGE_TYPE, () -> item.getModelObject().getChangeType()));

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
