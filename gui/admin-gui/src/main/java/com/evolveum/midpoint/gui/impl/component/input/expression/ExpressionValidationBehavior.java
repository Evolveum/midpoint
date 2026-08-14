/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import static com.evolveum.midpoint.web.util.ExpressionUtil.getScriptExpressionValue;
import static com.evolveum.midpoint.web.util.ExpressionUtil.usesIterationVariables;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Behavior that dynamically applies CSS validation classes
 * based on the {@link ExpressionPanel} evaluator state.
 *
 * <p>Adds:
 * <ul>
 *     <li>{@code is-empty no-valid-border} when the SCRIPT evaluator has no content</li>
 *     <li>{@code is-iteration-applied no-valid-border} when the script uses iteration variables</li>
 *     <li>{@code is-invalid} when the component has validation errors</li>
 * </ul>
 */
public class ExpressionValidationBehavior extends Behavior {

    private static final String CLASS_EMPTY = "is-empty";
    private static final String CLASS_ITERATION_APPLIED = "is-iteration-applied";
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
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);

        String classes = removeValidationClasses(tag.getAttribute("class"));
        String validationClasses = computeCssClass(component);

        if (validationClasses != null) {
            classes = classes.isEmpty()
                    ? validationClasses
                    : classes + " " + validationClasses;
        }

        if (classes.isEmpty()) {
            tag.remove("class");
        } else {
            tag.put("class", classes);
        }
    }

    private @NotNull String removeValidationClasses(@Nullable String classes) {
        if (classes == null || classes.isBlank()) {
            return "";
        }

        return Arrays.stream(classes.split("\\s+"))
                .filter(css -> !isValidationClass(css))
                .collect(Collectors.joining(" "));
    }

    private boolean isValidationClass(String css) {
        return CLASS_EMPTY.equals(css)
                || CLASS_ITERATION_APPLIED.equals(css)
                || CLASS_INVALID.equals(css)
                || CLASS_NO_VALID_BORDER.equals(css);
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
            ScriptExpressionEvaluatorType script =
                    getScriptExpressionValue(expression);

            return script == null
                    || script.getCode() == null
                    || script.getCode().isBlank();

        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Couldn't parse script expression.", e);
        }
    }

    @Override
    public void detach(Component component) {
        super.detach(component);

        typeModel.detach();
        expressionModel.detach();
    }
}
