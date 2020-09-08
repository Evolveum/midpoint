/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 */
public class TextFormGroup extends BasePanel<String> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_PROPERTY_LABEL = "propertyLabel";
    private static final String ID_ROW = "row";

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String labelCssClass, String textCssClass,
            boolean required, boolean isSimilarAsPropertyPanel) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, required, isSimilarAsPropertyPanel);
    }

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String labelCssClass, String textCssClass,
            boolean required) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, required, false);
    }

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey, boolean isTooltipInModel, String labelCssClass,
            String textCssClass, boolean required, boolean markAsRequired) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, markAsRequired, false);
    }

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey, boolean isTooltipInModel, String labelCssClass,
            String textCssClass, boolean required, boolean markAsRequired, boolean isSimilarAsPropertyPanel) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModel, labelCssClass, textCssClass, required, markAsRequired, isSimilarAsPropertyPanel);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelCssClass, String textCssClass, final boolean required,
            final boolean markAsRequired, boolean isSimilarAsPropertyPanel) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        add(labelContainer);

        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelCssClass)) {
            labelContainer.add(AttributeAppender.prepend("class", labelCssClass));
        }
        if (isSimilarAsPropertyPanel) {
            labelContainer.add(AttributeAppender.prepend("class", " col-xs-2 prism-property-label "));
        } else {
            labelContainer.add(AttributeAppender.prepend("class", " control-label "));
        }
        labelContainer.add(l);

        Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
        tooltipLabel.add(new AttributeAppender("data-original-title", new IModel<String>() {

            @Override
            public String getObject() {
                return getString(tooltipKey);
            }
        }));
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

        WebMarkupContainer requiredContainer = new WebMarkupContainer(ID_REQUIRED);
        requiredContainer.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return markAsRequired;
            }
        });
        labelContainer.add(requiredContainer);
        WebMarkupContainer propertyLabel = new WebMarkupContainer(ID_PROPERTY_LABEL);
        WebMarkupContainer rowLabel = new WebMarkupContainer(ID_ROW);
        WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
        if (StringUtils.isNotEmpty(textCssClass)) {
            textWrapper.add(AttributeAppender.prepend("class", textCssClass));
        }
        if (isSimilarAsPropertyPanel) {
            propertyLabel.add(AttributeAppender.prepend("class", " col-md-10 prism-property-value "));
            rowLabel.add(AttributeAppender.prepend("class", " row "));
        }
        propertyLabel.add(rowLabel);
        rowLabel.add(textWrapper);
        add(propertyLabel);

        TextField text = createText(getModel(), label, required);
        text.setLabel(label);
        textWrapper.add(text);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ContainerFeedbackMessageFilter(this));
        feedback.setOutputMarkupId(true);
        textWrapper.add(feedback);
    }

    protected TextField createText(IModel<String> model, IModel<String> label, boolean required) {
        TextField<?> text = new TextField<>(ID_TEXT, model);
        text.setRequired(required);

        return text;
    }

    public TextField getField() {
        return (TextField) get(createComponentPath(ID_PROPERTY_LABEL, ID_ROW, ID_TEXT_WRAPPER, ID_TEXT));
    }
}
