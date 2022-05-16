/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 */
public class CheckFormGroup extends BasePanel<Boolean> {

    private static final String ID_CHECK = "check";
    private static final String ID_CHECK_WRAPPER = "checkWrapper";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label) {
        this(id, value, label, null ,null);
    }

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelSize, String textSize) {
        this(id, value, label, null, labelSize, textSize);
    }

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label, String tooltipKey,
            String labelSize, String textSize) {
        super(id, value);

        initLayout(label, tooltipKey, labelSize, textSize);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, String labelSize, String textSize) {
        add(AttributeAppender.prepend("class", "form-check"));

        Label l = new Label(ID_LABEL, label);
        l.add(new VisibleBehaviour(() -> getLabelVisible()));

        if (StringUtils.isNotEmpty(labelSize)) {
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
        tooltipLabel.add(AttributeAppender.append("data-original-title", () -> getString(tooltipKey)));
        tooltipLabel.add(new InfoTooltipBehavior());
        tooltipLabel.add(new VisibleBehaviour(() -> tooltipKey != null));
        tooltipLabel.setOutputMarkupId(true);
        add(tooltipLabel);

        CheckBox check = new CheckBox(ID_CHECK, getModel());
        check.add(AttributeAppender.prepend("class", textSize));
        check.setOutputMarkupId(true);
        check.setLabel(label);
        add(check);
    }

    protected boolean getLabelVisible() {
        return true;
    }

    public CheckBox getCheck() {
        return (CheckBox) get(ID_CHECK);
    }

    public Boolean getValue() {
        return getCheck().getModelObject();
    }
}
