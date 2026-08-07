/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import static com.evolveum.midpoint.web.util.ExpressionUtil.getScriptExpressionValue;
import static com.evolveum.midpoint.web.util.ExpressionUtil.usesIterationVariables;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Behavior that dynamically appends CSS validation classes
 * based on the {@link ExpressionPanel} evaluator state.
 *
 * <p>Adds:
 *
 * <ul>
 *     <li>{@code is-empty no-valid-border} when the SCRIPT evaluator has no content</li>
 *     <li>{@code is-iteration-applied no-valid-border} when the script uses iteration variables</li>
 *     <li>{@code is-invalid} when the component has validation errors</li>
 * </ul>
 */
public class ExpressionValidationBehavior extends Behavior {

    private static final String CLASS_EMPTY = "is-empty";
    private static final String CLASS_ITERATION_APPLIED = "is-iteration-applied no-";
    private static final String CLASS_INVALID = "is-invalid";
    private static final String CLASS_NO_VALID_BORDER = "no-valid-border";

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

        removeValidationClasses(component);

        String cssClass = computeCssClass(component);
        if (cssClass != null) {
            component.add(AttributeModifier.append("class", cssClass));
        }
    }

    private void removeValidationClasses(@NotNull Component component) {
        String current = (String) component.getMarkupAttributes().get("class");
        if (current == null || current.isBlank()) {
            return;
        }

        String updated = current
                .replace(CLASS_EMPTY, "")
                .replace(CLASS_ITERATION_APPLIED, "")
                .replace(CLASS_INVALID, "")
                .replace(CLASS_NO_VALID_BORDER, "")
                .replaceAll("\\s+", " ")
                .trim();

        component.add(AttributeModifier.replace("class", updated));
    }

    private @Nullable String computeCssClass(@NotNull Component component) {
        if (component.hasErrorMessage()) {
            return CLASS_INVALID;
        }

        if (typeModel.getObject() != ExpressionPanel.RecognizedEvaluator.SCRIPT) {
            return null;
        }

        ExpressionType expression = expressionModel.getObject();

        if (isEmpty(expression)) {
            return CLASS_EMPTY + " " + CLASS_NO_VALID_BORDER;
        }

        if (usesIterationVariables(expression)) {
            return CLASS_ITERATION_APPLIED + " " + CLASS_NO_VALID_BORDER;
        }

        return null;
    }

    private boolean isEmpty(@Nullable ExpressionType expression) {
        if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
            return true;
        }

        try {
            ScriptExpressionEvaluatorType script = getScriptExpressionValue(expression);

            return script == null
                    || script.getCode() == null
                    || script.getCode().isBlank();

        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't parse script expression.", e);
        }
    }

    @Override
    public void detach(Component component) {
        super.detach(component);

        typeModel.detach();
        expressionModel.detach();
    }
}
