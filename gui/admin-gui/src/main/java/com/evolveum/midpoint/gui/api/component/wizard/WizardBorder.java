/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardBorder extends Border {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_STEP = "step";
    private static final String ID_LINE = "line";

    private IModel<Wizard> model;

    public WizardBorder(String id, IModel<Wizard> model) {
        super(id);

        this.model = model;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    private void initLayout() {
        add(AttributeAppender.prepend("class", "bs-stepper"));

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.setOutputMarkupId(true);
        addToBorder(header);

        ListView<IModel<String>> steps = new ListView<>(ID_STEPS, () -> model.getObject().getStepLabels()) {

            @Override
            protected void populateItem(ListItem<IModel<String>> listItem) {
                WizardStepPanel step = new WizardStepPanel(ID_STEP, listItem.getIndex(), listItem.getModelObject());
                step.add(AttributeAppender.append("class", () -> model.getObject().getActiveStepIndex() == listItem.getIndex() ? "active" : null));
                listItem.add(step);

                WebMarkupContainer line = new WebMarkupContainer(ID_LINE);
                // hide last "line"
                line.add(new VisibleBehaviour(() -> listItem.getIndex() < model.getObject().getStepLabels().size() - 1));
                listItem.add(line);
            }
        };
        header.add(steps);
    }
}
