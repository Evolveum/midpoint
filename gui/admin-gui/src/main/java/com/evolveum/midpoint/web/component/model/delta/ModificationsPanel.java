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
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ModificationsPanel extends BasePanel<DeltaDto> {

    private static final Trace LOGGER = TraceManager.getTrace(ModificationsPanel.class);

    private static final String ID_MODIFICATION = "modification";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_VALUE = "value";

    public ModificationsPanel(String id, IModel<DeltaDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

        add(new ListView<ModificationDto>(ID_MODIFICATION, new PropertyModel(getModel(), DeltaDto.F_MODIFICATIONS)) {
            @Override
            protected void populateItem(ListItem<ModificationDto> item) {
                item.add(new Label(ID_ATTRIBUTE, () -> item.getModelObject().getAttribute()));
                item.add(new Label(ID_CHANGE_TYPE, () -> item.getModelObject().getChangeType()));

                if (item.getModelObject().getValue() instanceof ContainerValueDto) {
                    item.add(new ContainerValuePanel(ID_VALUE, () -> (ContainerValueDto) item.getModelObject().getValue()));
                } else {    // should be String
                    item.add(new Label(ID_VALUE, () -> {
                        Object value = item.getModelObject().getValue();
                        if (value instanceof PolyString) {
                            return WebComponentUtil.getTranslatedPolyString((PolyString) value);
                        }

                        return value;
                    }));
                }
            }
        });
    }
}
