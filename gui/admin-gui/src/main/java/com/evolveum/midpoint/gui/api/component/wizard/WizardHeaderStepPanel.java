/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardHeaderStepPanel extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_STATUS = "status";
    private static final String ID_CIRCLE = "circle";
    private static final String ID_LABEL = "label";

    private final int activeIndex;
    private final int index;

    public WizardHeaderStepPanel(String id, int index, int activeIndex, IModel<String> model) {
        super(id, model);

        this.index = index;
        this.activeIndex = activeIndex;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> isActiveIndex() ? "step active" : "step"));
        add(AttributeAppender.append("aria-current", () -> isActiveIndex() ? "step" : null));
        add(AttributeAppender.append("tabindex", ()-> isActiveIndex() ? "0" : "-1"));

        Label stepStatus = new Label(ID_STATUS, () -> {
            String key = "";
            if (isActiveIndex()) {
                key = "WizardHeaderStepPanel.status.current";
            } else if (index < activeIndex) {
                key = "WizardHeaderStepPanel.status.completed";
            }
            return LocalizationUtil.translate(key);
        });
        stepStatus.setOutputMarkupId(true);
        add(stepStatus);

        add(new Label(ID_CIRCLE, () -> index + 1));

        Label stepTitle = new Label(ID_LABEL, this::getModelObject);
        stepTitle.setOutputMarkupId(true);
        add(stepTitle);

        add(AttributeAppender.append("aria-labelledby", String.format("%s %s", stepStatus.getMarkupId(), stepTitle.getMarkupId())));
    }

    private boolean isActiveIndex() {
        return activeIndex == index;
    }
}
