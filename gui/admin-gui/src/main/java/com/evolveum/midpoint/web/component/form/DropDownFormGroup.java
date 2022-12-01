/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 */
public class DropDownFormGroup<T> extends BasePanel<T> {

    private static final String ID_SELECT = "select";
    private static final String ID_SELECT_WRAPPER = "selectWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_ADDITIONAL_INFO = "additionalInfo";
    private static final String ID_PROPERTY_LABEL = "propertyLabel";
    private static final String ID_ROW = "row";

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
            IModel<String> label, String labelCssClass, String textCssClass, boolean required, boolean isSimilarAsPropertyPanel) {
        this(id, value, choices, renderer, label, Model.of(), labelCssClass, textCssClass, required, isSimilarAsPropertyPanel);
    }

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
            IModel<String> label, String labelCssClass, String textCssClass, boolean required) {
        this(id, value, choices, renderer, label, Model.of(), labelCssClass, textCssClass, required, false);
    }

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
            IModel<String> label, IModel<String> tooltipKey, String labelCssClass, String textCssClass, boolean required) {
        this(id, value, choices, renderer, label, tooltipKey, labelCssClass, textCssClass, required, false);
    }

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer, IModel<String> label, IModel<String> tooltipModel,
            String labelCssClass, String textCssClass, boolean required, boolean isSimilarAsPropertyPanel) {
        super(id, value);

        initLayout(choices, renderer, label, tooltipModel, labelCssClass, textCssClass, required, isSimilarAsPropertyPanel);
    }

    private void initLayout(IModel<List<T>> choices, IChoiceRenderer<T> renderer, IModel<String> label, final IModel<String> tooltipModel,
            String labelCssClass, String textCssClass, final boolean required,
            boolean isSimilarAsPropertyPanel) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.add(new VisibleBehaviour(() -> label != null && StringUtils.isNotEmpty(label.getObject())));
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
        tooltipLabel.add(new AttributeAppender("data-original-title", tooltipModel));
        tooltipLabel.add(new InfoTooltipBehavior());
        tooltipLabel.add(new VisibleBehaviour(() -> tooltipModel != null && StringUtils.isNotEmpty(tooltipModel.getObject())));
        tooltipLabel.setOutputMarkupId(true);
        tooltipLabel.setOutputMarkupPlaceholderTag(true);
        labelContainer.add(tooltipLabel);

        WebMarkupContainer requiredContainer = new WebMarkupContainer(ID_REQUIRED);
        requiredContainer.add(new VisibleBehaviour(() -> required));
        labelContainer.add(requiredContainer);

        WebMarkupContainer propertyLabel = new WebMarkupContainer(ID_PROPERTY_LABEL);
        WebMarkupContainer rowLabel = new WebMarkupContainer(ID_ROW);
        WebMarkupContainer selectWrapper = new WebMarkupContainer(ID_SELECT_WRAPPER);
        if (StringUtils.isNotEmpty(textCssClass)) {
            selectWrapper.add(AttributeAppender.prepend("class", textCssClass));
        }
        if (isSimilarAsPropertyPanel) {
            propertyLabel.add(AttributeAppender.prepend("class", " col-md-10 prism-property-value "));
            rowLabel.add(AttributeAppender.prepend("class", " row "));
        }
        propertyLabel.add(rowLabel);
        rowLabel.add(selectWrapper);
        add(propertyLabel);

        DropDownChoice<T> select = createDropDown(ID_SELECT, choices, renderer, required);
        select.setLabel(label);
        selectWrapper.add(select);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(select));
        feedback.setOutputMarkupId(true);
        selectWrapper.add(feedback);

        Component additionalInfo = createAdditionalInfoComponent(ID_ADDITIONAL_INFO);
        if (additionalInfo == null) {
            additionalInfo = new Label(ID_ADDITIONAL_INFO, "");
        }
        selectWrapper.add(additionalInfo);
    }

    protected Component createAdditionalInfoComponent(String id) {
        return null;
    }

    public Component getAdditionalInfoComponent() {
        return get(createComponentPath(ID_PROPERTY_LABEL, ID_ROW, ID_SELECT_WRAPPER, ID_ADDITIONAL_INFO));
    }

    protected DropDownChoice<T> createDropDown(String id, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
            boolean required) {
        DropDownChoice<T> choice = new DropDownChoice<T>(id, getModel(), choices, renderer) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return DropDownFormGroup.this.getNullValidDisplayValue();
            }

            @Override
            public IModel<? extends List<? extends T>> getChoicesModel() {
                IModel<? extends List<? extends T>> choices = super.getChoicesModel();
                return Model.ofList(WebComponentUtil.sortDropDownChoices(choices, renderer));
            }
        };
        choice.setNullValid(!required);
        choice.setRequired(required);
        return choice;
    }

    public DropDownChoice<T> getInput() {
        //noinspection unchecked
        return (DropDownChoice<T>) get(createComponentPath(ID_PROPERTY_LABEL, ID_ROW, ID_SELECT_WRAPPER, ID_SELECT));
    }

    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.empty");
    }
}
