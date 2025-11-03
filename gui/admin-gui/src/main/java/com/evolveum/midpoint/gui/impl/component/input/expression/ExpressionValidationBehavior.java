/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Behavior that dynamically appends CSS validation classes
 * based on the ExpressionPanel evaluator state.
 * <p>
 * Adds:
 * - "is-info" when SCRIPT evaluator has no content
 * - "is-valid-all-set no-valid-border" when SCRIPT evaluator has content
 * - (future) "is-invalid" if component has feedback errors
 */
public class ExpressionValidationBehavior extends Behavior {

    private final IModel<ExpressionPanel.RecognizedEvaluator> typeModel;
    private final IModel<ExpressionType> expressionModel;

    public ExpressionValidationBehavior(
            @NotNull IModel<ExpressionPanel.RecognizedEvaluator> typeModel,
            @NotNull IModel<ExpressionType> expressionModel) {
        this.typeModel = typeModel;
        this.expressionModel = expressionModel;
    }

    @Override
    public void onConfigure(Component component) {
        super.onConfigure(component);

        String current = (String) component.getMarkupAttributes().get("class");
        if (current != null) {
            current = current
                    .replace("is-info", "")
                    .replace("is-valid-all-set no-valid-border", "")
                    .replace("is-invalid", "")
                    .trim();

            component.add(AttributeModifier.replace("class", current));
        }

        String cssClass = computeCssClass(component);
        if (cssClass != null) {
            component.add(AttributeModifier.append("class", cssClass));
        }
    }

    private @Nullable String computeCssClass(@NotNull Component component) {
        if (component.hasErrorMessage()) {
            return "is-invalid";
        }

        ExpressionPanel.RecognizedEvaluator evaluator = typeModel.getObject();
        if (evaluator == null) {
            return null;
        }

        if (ExpressionPanel.RecognizedEvaluator.SCRIPT.equals(evaluator)) {
            boolean isEmpty = expressionModel.getObject() == null
                    || expressionModel.getObject().getExpressionEvaluator().isEmpty();
            return isEmpty
                    ? "is-info"
                    : "is-valid-all-set no-valid-border";
        }
        return null;
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        typeModel.detach();
        expressionModel.detach();
    }
}
