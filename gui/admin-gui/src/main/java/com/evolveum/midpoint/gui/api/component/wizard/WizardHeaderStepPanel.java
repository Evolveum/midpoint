/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import java.io.Serial;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardHeaderStepPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CIRCLE = "circle";
    private static final String ID_LABEL = "label";
    private static final String ID_STEP_DESC = "stepDesc";

    private final int index;
    WizardHeaderStepHelper wizardHeaderStepHelper;

    public WizardHeaderStepPanel(String id, int index, IModel<String> model, WizardHeaderStepHelper wizardHeaderStepHelper) {
        super(id, model);

        this.index = index;
        this.wizardHeaderStepHelper = wizardHeaderStepHelper;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> wizardHeaderStepHelper.isStepActive(index) ?
                "step active" : "step"));
        add(AttributeAppender.append("tabindex", ()-> wizardHeaderStepHelper.isStepActive(index) ? "0" : "-1"));

        Label stepDesc = new Label(ID_STEP_DESC, wizardHeaderStepHelper.getStepPanelAriaLabelModel(index, getModelObject()));
        stepDesc.setOutputMarkupId(true);
        add(stepDesc);

        add(new Label(ID_CIRCLE, () -> index + 1));

        Label stepTitle = new Label(ID_LABEL, this::getModelObject);
        stepTitle.setOutputMarkupId(true);
        add(stepTitle);

//        add(AttributeAppender.append("aria-labelledby", String.format("%s %s", stepStatus.getMarkupId(), stepTitle.getMarkupId())));
    }
}
