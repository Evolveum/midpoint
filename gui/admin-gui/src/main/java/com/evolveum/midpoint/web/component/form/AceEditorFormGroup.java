/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 */
public class AceEditorFormGroup extends BasePanel<String> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";

    private static final int DEFAULT_NUMBER_OF_ROWS = 2;

    public AceEditorFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize) {
        this(id, value, label, labelSize, textSize, false);
    }

    public AceEditorFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize,
            boolean required) {
        this(id, value, label, labelSize, textSize, required, DEFAULT_NUMBER_OF_ROWS);
    }

    public AceEditorFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize,
            boolean required, int rowNumber) {
        this(id, value, label, null, false, labelSize, textSize, required, rowNumber);
    }

    public AceEditorFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey,
            boolean isTooltipInModal, String labelSize, String textSize, boolean required, int rowNumber) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModal, labelSize, textSize, required, rowNumber);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelSize,
            String textSize, boolean required, int rowNumber) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        add(labelContainer);

        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            labelContainer.add(AttributeAppender.prepend("class", labelSize));
        }
        labelContainer.add(l);

        Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
        tooltipLabel.add(new AttributeAppender("data-original-title",
                (IModel<String>) () -> getString(tooltipKey)));
        tooltipLabel.add(new InfoTooltipBehavior(isTooltipInModal));
        tooltipLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return tooltipKey != null;
            }
        });
        tooltipLabel.setOutputMarkupId(true);
        tooltipLabel.setOutputMarkupPlaceholderTag(true);
        labelContainer.add(tooltipLabel);

        WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            textWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(textWrapper);

        AceEditor text = new AceEditor(ID_TEXT, getModel());
        text.add(new AttributeModifier("rows", rowNumber));
        text.setOutputMarkupId(true);
        text.setRequired(required);
        text.setLabel(label);
        text.add(AttributeAppender.replace("placeholder", label));
        textWrapper.add(text);
    }

    public void setRows(int rows) {
        TextArea<?> area = (TextArea<?>) get(createComponentPath(ID_TEXT_WRAPPER, ID_TEXT));
        area.add(AttributeModifier.replace("rows", rows));
    }
}
