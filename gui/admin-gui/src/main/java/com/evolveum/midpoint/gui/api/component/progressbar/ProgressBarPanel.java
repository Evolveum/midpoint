/*
 * Copyright (c) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.progressbar;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * @author semancik
 */
public class ProgressBarPanel extends BasePanel<List<ProgressBar>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_BARS = "bars";
    private static final String ID_BAR = "bar";
    private static final String ID_TEXT = "text";

    public ProgressBarPanel(String id, IModel<List<ProgressBar>> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "progress rounded"));

        ListView<ProgressBar> bars = new ListView<>(ID_BARS, getModelObject()) {
            @Override
            protected void populateItem(ListItem<ProgressBar> item) {
                item.add(createBar(ID_BAR, item.getModel()));
            }
        };
        add(bars);

        Label text = new Label(ID_TEXT);
        add(text);
    }

    private Component createBar(String id, IModel<ProgressBar> model) {
        WebComponent bar = new WebComponent(id);
        bar.add(AttributeAppender.append("class", () -> model.getObject().getState().getCssClass()));
        bar.add(AttributeAppender.append("style", () -> "width: " + model.getObject().getValue() + "%;"));

        return bar;
    }
}
