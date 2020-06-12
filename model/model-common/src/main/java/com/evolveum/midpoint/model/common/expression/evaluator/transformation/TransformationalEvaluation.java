/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

/**
 *
 */
class TransformationalEvaluation<V extends PrismValue, D extends ItemDefinition, E extends TransformExpressionEvaluatorType> {

    @NotNull final ExpressionEvaluationContext context;
    @NotNull final OperationResult parentResult;
    @NotNull final AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator;
    @NotNull final PrismContext prismContext;

    TransformationalEvaluation(@NotNull ExpressionEvaluationContext context, @NotNull OperationResult parentResult,
            @NotNull AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator) {
        this.context = context;
        this.parentResult = parentResult;
        this.evaluator = evaluator;
        this.prismContext = evaluator.getPrismContext();
    }
}
