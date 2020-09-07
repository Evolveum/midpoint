/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 * @author skublik
 */
public class TriStateFormGroup extends BasePanel<Boolean> {
    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE = "value";
    private static final String ID_VALUE_WRAPPER = "valueWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_PROPERTY_LABEL = "propertyLabel";
    private static final String ID_ROW = "row";

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelCssClass, String textCssClass, boolean required, boolean isSimilarAsPropertyPanel) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, isSimilarAsPropertyPanel);
    }

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelCssClass, String textCssClass, boolean required) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, false);
    }

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String tooltipKey,
            boolean isTooltipInModal, String labelCssClass, String textCssClass, boolean required) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, false);
    }

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String tooltipKey,
            boolean isTooltipInModal, String labelCssClass, String textCssClass, boolean required, boolean isSimilarAsPropertyPanel) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModal, labelCssClass, textCssClass, required, isSimilarAsPropertyPanel);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelCssClass, String textCssClass,
            boolean required, boolean isSimilarAsPropertyPanel) {
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

        WebMarkupContainer requiredContainer = new WebMarkupContainer(ID_REQUIRED);
        requiredContainer.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return required;
            }
        });
        labelContainer.add(requiredContainer);

        WebMarkupContainer propertyLabel = new WebMarkupContainer(ID_PROPERTY_LABEL);
        WebMarkupContainer rowLabel = new WebMarkupContainer(ID_ROW);
        WebMarkupContainer valueWrapper = new WebMarkupContainer(ID_VALUE_WRAPPER);
        if (StringUtils.isNotEmpty(textCssClass)) {
            valueWrapper.add(AttributeAppender.prepend("class", textCssClass));
        }
        if (isSimilarAsPropertyPanel) {
            propertyLabel.add(AttributeAppender.prepend("class", " col-md-10 prism-property-value "));
            rowLabel.add(AttributeAppender.prepend("class", " row "));
        }
        propertyLabel.add(rowLabel);
        rowLabel.add(valueWrapper);
        add(propertyLabel);

        TriStateComboPanel triStateCombo = new TriStateComboPanel(ID_VALUE, getModel());
        valueWrapper.add(triStateCombo);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(triStateCombo.getBaseFormComponent()));
        feedback.setOutputMarkupId(true);
        valueWrapper.add(feedback);
    }

    public TriStateComboPanel getValue() {
        return (TriStateComboPanel) get(createComponentPath(ID_PROPERTY_LABEL, ID_ROW, ID_VALUE_WRAPPER, ID_VALUE));
    }
}
