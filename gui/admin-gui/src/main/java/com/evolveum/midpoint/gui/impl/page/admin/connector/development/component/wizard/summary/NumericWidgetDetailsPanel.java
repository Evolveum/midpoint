/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Map;

public class NumericWidgetDetailsPanel extends BasePanel<NumericWidgetDetailsDto> {

    private static final String ID_COLUMN = "column";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";

    public NumericWidgetDetailsPanel(String id, IModel<NumericWidgetDetailsDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<Map.Entry<IModel<String>, IModel<Integer>>> columns = new ListView<>(ID_COLUMN, () -> new ArrayList<>(getModelObject().values().entrySet())) {
            @Override
            protected void populateItem(ListItem<Map.Entry<IModel<String>, IModel<Integer>>> item) {
                if (item.getIndex() != getModelObject().size() - 1) {
                    item.add(AttributeAppender.append("class", "border-right"));
                }
                Label label = new Label(ID_LABEL, item.getModelObject().getKey());
                label.setOutputMarkupId(true);
                item.add(label);

                Label value = new Label(ID_VALUE, item.getModelObject().getValue());
                value.setOutputMarkupId(true);
                item.add(value);
            }
        };
        columns.setOutputMarkupId(true);
        add(columns);
    }
}
