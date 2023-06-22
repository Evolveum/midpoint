/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TextFieldLabelPanel extends Panel {

    private static final String ID_INPUT = "input";
    private static final String ID_DETAILS = "details";

    IModel<?> model;
    String labelInfo;

    public TextFieldLabelPanel(String id, IModel<?> model, String labelInfo) {
        super(id);
        this.model = model;
        this.labelInfo = labelInfo;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label label = new Label(ID_DETAILS, Model.of(labelInfo));
        label.setOutputMarkupId(true);
        add(label);
        TextField<?> thresholdField2 = new TextField<>(ID_INPUT, model);
        thresholdField2.setOutputMarkupId(true);

        thresholdField2.setOutputMarkupPlaceholderTag(true);
        thresholdField2.setVisible(true);
        add(thresholdField2);

    }


    public TextField<?> getBaseFormComponent() {
        return (TextField<?>) get(ID_INPUT);
    }

    public Label getLabelFormComponent() {
        return (Label) get(ID_DETAILS);
    }
}
