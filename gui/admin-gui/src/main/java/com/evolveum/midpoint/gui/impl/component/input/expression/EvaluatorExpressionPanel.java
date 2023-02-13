/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class EvaluatorExpressionPanel extends BasePanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE_INPUT = "valueInput";
    private static final String ID_VALUE_CONTAINER = "valueContainer";
    private static final String ID_VALUE_CONTAINER_LABEL = "valueContainerLabel";
    private static final String ID_ADD_BUTTON = "add";
    private static final String ID_REMOVE_BUTTON = "remove";
    private static final String ID_FEEDBACK = "feedback";

    public EvaluatorExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        add(valueContainer);

        valueContainer.add(new Label(ID_VALUE_CONTAINER_LABEL, getValueContainerLabelModel()));

        FeedbackLabels feedback = new FeedbackLabels(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        valueContainer.add(feedback);

        AjaxLink<Void> addButton = new AjaxLink<>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        };
        valueContainer.add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeItem(target);
            }
        };
        valueContainer.add(removeButton);

        valueContainer.add(createDefaultInputPanel());
    }

    public abstract IModel<String> getValueContainerLabelModel();

    protected WebMarkupContainer createDefaultInputPanel() {

        IModel<List<String>> model = new IModel<>() {
            @Override
            public List<String> getObject() {
                return getEvaluatorValues();
            }

            @Override
            public void setObject(List<String> strings) {
                updateEvaluatorValue(getModelObject(), strings);
            }

            @Override
            public void detach() {
            }
        };

        NonEmptyModel<Boolean> readOnlyModel = new NonEmptyLoadableModel<>(false) {
            @NotNull
            @Override
            protected Boolean load() {
                return false;
            }
        };
        MultiValueTextPanel<String> valueInput = new MultiValueTextPanel<>(EvaluatorExpressionPanel.ID_VALUE_INPUT, model, readOnlyModel, false) {

            @Override
            protected Behavior getAddButtonVisibleBehavior(NonEmptyModel<Boolean> readOnlyModel) {
                return VisibleBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createEmptyItemPlaceholder() {
                return () -> "";
            }

            @Override
            protected InputPanel createTextPanel(String id, IModel<String> model) {
                return EvaluatorExpressionPanel.this.createTextPanel(id, model);
            }

            @Override
            protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<String> strings) {
                updateEvaluatorValue(EvaluatorExpressionPanel.this.getModelObject(), strings);
                target.add(getFeedback());
            }
        };
        valueInput.setOutputMarkupId(true);
        return valueInput;
    }

    protected InputPanel createTextPanel(String id, IModel<String> model) {
        return new TextPanel<>(id, model);
    }

    private void removeItem(AjaxRequestTarget target) {
        getModelObject().getExpressionEvaluator().clear();
        target.add(get(ID_VALUE_CONTAINER));
        target.add(getFeedback());
    }

    private void addValue(AjaxRequestTarget target) {
        List<String> values = getEvaluatorValues();
        values.add("");
        updateEvaluatorValue(EvaluatorExpressionPanel.this.getModelObject(), values);
        target.add(get(ID_VALUE_CONTAINER));
        target.add(getFeedback());
    }

    protected FeedbackLabels getFeedback() {
        return (FeedbackLabels) get(createComponentPath(ID_VALUE_CONTAINER, ID_FEEDBACK));
    }

    protected abstract void updateEvaluatorValue(ExpressionType expression, List<String> values);

    protected abstract List<String> getEvaluatorValues();
}
