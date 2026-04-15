/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Map;

public class StringValuesWidgetDetailsPanel extends BasePanel<StringValuesWidgetDetailsDto> {

    private static final String ID_ROW = "row";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";

    public StringValuesWidgetDetailsPanel(String id, IModel<StringValuesWidgetDetailsDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<Map.Entry<IModel<String>, IModel<String>>> rows = new ListView<>(ID_ROW, () -> new ArrayList<>(getModelObject().values().entrySet())) {
            @Override
            protected void populateItem(ListItem<Map.Entry<IModel<String>, IModel<String>>> item) {
                Label label = new Label(ID_LABEL, item.getModelObject().getKey());
                label.setOutputMarkupId(true);
                item.add(label);

                Label value = new Label(ID_VALUE, item.getModelObject().getValue());
                value.add(AttributeAppender.append("title", item.getModelObject().getValue()));
                value.add(new TooltipBehavior());
                value.setOutputMarkupId(true);
                item.add(value);
            }
        };
        rows.setOutputMarkupId(true);
        add(rows);
    }
}
