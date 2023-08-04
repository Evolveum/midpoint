/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jetbrains.annotations.NotNull;

public class SimpleValueExpressionPanel extends EvaluatorExpressionPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleValueExpressionPanel.class);

    private static final String ID_VALUE_INPUT = "valueInput";
    private static final String ID_VALUE_CONTAINER_LABEL = "valueContainerLabel";
    private static final String ID_ADD_BUTTON = "add";
    private static final String ID_REMOVE_BUTTON = "remove";

    public SimpleValueExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
        if (getEvaluatorValues().isEmpty()) {
            updateEvaluatorValue(List.of());
        }
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("AssociationExpressionValuePanel.literalValue");
    }

    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        StringBuilder sb = new StringBuilder();
        List<String> values = getEvaluatorValues(expression);
        if (values.isEmpty()) {
            return null;
        }
        values.forEach(value -> {
            if (!sb.isEmpty()){
                sb.append(", ");
            }
            sb.append(value);
        });
        return pageBase.getString("SimpleValueExpressionPanel.values", sb.toString());
    }

    protected void initLayout(MarkupContainer parent) {


        parent.add(new Label(ID_VALUE_CONTAINER_LABEL, getValueContainerLabelModel()));

        AjaxLink<Void> addButton = new AjaxLink<>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        };
        parent.add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeItem(target);
            }
        };
        parent.add(removeButton);

        parent.add(createDefaultInputPanel());
    }

    private WebMarkupContainer createDefaultInputPanel() {

        IModel<List<String>> model = new IModel<>() {
            @Override
            public List<String> getObject() {
                return getEvaluatorValues();
            }

            @Override
            public void setObject(List<String> values) {
                updateEvaluatorValue(values);
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
        MultiValueTextPanel<String> valueInput = new MultiValueTextPanel<>(ID_VALUE_INPUT, model, readOnlyModel, false) {

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
                return new TextPanel<>(id, model);
            }

            @Override
            protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<String> strings) {
                updateEvaluatorValue(strings);
                target.add(getFeedback());
            }
        };
        valueInput.setOutputMarkupId(true);
        return valueInput;
    }

    private void removeItem(AjaxRequestTarget target) {
        getModelObject().getExpressionEvaluator().clear();
        target.add(getValueContainer());
        target.add(getFeedback());
    }

    private void addValue(AjaxRequestTarget target) {
        List<String> values = getEvaluatorValues();
        values.add("");
        updateEvaluatorValue(values);
        target.add(getValueContainer());
        target.add(getFeedback());
    }

    private void updateEvaluatorValue(List<String> values) {
        ExpressionUtil.updateLiteralExpressionValue(getModelObject(), values, PrismContext.get());
    }

    private List<String> getEvaluatorValues() {
        return getEvaluatorValues(getModelObject());
    }

    private static List<String> getEvaluatorValues(ExpressionType expression) {
        List<String> literalValueList = new ArrayList<>();
        try {
            return ExpressionUtil.getLiteralExpressionValues(expression);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
        }
        return literalValueList;
    }
}
