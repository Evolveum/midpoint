/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import org.jetbrains.annotations.NotNull;

/**
 * Context for a transformation of a single value (or values tuple) during expression evaluation.
 *
 * It exists to avoid passing multiple variables back and forth, and also to make explicit the distinction
 * regarding {@link ExpressionEvaluationContext}, which provides context for the _whole_ expression evaluation.
 */
public class ValueTransformationContext {

    /** The context of the whole expression evaluation. */
    @NotNull private final ExpressionEvaluationContext expressionEvaluationContext;

    /** Variables available in the value transformation. They _MUST NOT_ be relativistic: no deltas there. */
    @NotNull private final VariablesMap variablesMap;

    /** Are we using "new" state of the values to compute the expected "new" state of the result? */
    private final boolean evaluateNew;

    /** The full description of the context of this value transformation operation. */
    @NotNull private final String contextDescription;

    ValueTransformationContext(
            @NotNull ExpressionEvaluationContext expressionEvaluationContext,
            @NotNull VariablesMap variablesMap,
            boolean evaluateNew,
            @NotNull String contextDescription) {
        this.expressionEvaluationContext = expressionEvaluationContext;
        this.variablesMap = variablesMap;
        this.evaluateNew = evaluateNew;
        this.contextDescription = contextDescription;

        assert !variablesMap.haveDeltas();
    }

    public @NotNull ExpressionEvaluationContext getExpressionEvaluationContext() {
        return expressionEvaluationContext;
    }

    public @NotNull VariablesMap getVariablesMap() {
        return variablesMap;
    }

    public boolean isEvaluateNew() {
        return evaluateNew;
    }

    public @NotNull String getContextDescription() {
        return contextDescription;
    }

    /** @see ExpressionEvaluationContext#toString() */
    @Override
    public String toString() {
        return contextDescription;
    }
}
