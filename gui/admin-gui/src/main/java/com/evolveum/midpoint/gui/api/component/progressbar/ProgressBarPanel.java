/*
 * Copyright (c) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.progressbar;

import java.util.List;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

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
        ListView<ProgressBar> bars = new ListView<>(ID_BARS, getModelObject()) {
            @Override
            protected void populateItem(ListItem<ProgressBar> item) {
                item.add(createBar(ID_BAR, item.getModel()));
            }
        };
        add(bars);

        IModel<String> textModel = createTextModel(getModel());
        Label text = new Label(ID_TEXT, textModel);
        text.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(textModel.getObject())));
        add(text);
    }

    private Component createBar(String id, IModel<ProgressBar> model) {
        WebComponent bar = new WebComponent(id);
        bar.add(AttributeAppender.append("class", () -> model.getObject().getState().getCssClass()));
        bar.add(AttributeAppender.append("style", () -> "width: " + model.getObject().getValue() + "%;"));

        return bar;
    }

    protected IModel<String> createTextModel(IModel<List<ProgressBar>> model) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                Object[] texts = model.getObject().stream()
                        .filter(bar -> bar.getText() != null)
                        .map(bar -> WebComponentUtil.translateMessage(bar.getText()))
                        .filter(StringUtils::isNotEmpty)
                        .toArray();

                return StringUtils.joinWith(" / ", texts);
            }
        };
    }
}
