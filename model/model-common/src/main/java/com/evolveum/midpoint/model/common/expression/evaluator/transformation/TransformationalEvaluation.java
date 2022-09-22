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
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;

class TransformationalEvaluation<V extends PrismValue, D extends ItemDefinition, E extends TransformExpressionEvaluatorType> {

    @NotNull final ExpressionEvaluationContext context;
    @NotNull final OperationResult parentResult;
    @NotNull final AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator;
    @NotNull final PrismContext prismContext;

    @Nullable final ValueTransformationExpressionEvaluationTraceType trace;

    TransformationalEvaluation(@NotNull ExpressionEvaluationContext context, @NotNull OperationResult parentResult,
            @NotNull AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator) {
        this.context = context;
        this.parentResult = parentResult;
        this.evaluator = evaluator;
        this.prismContext = evaluator.getPrismContext();
        if (parentResult.isTraced() && parentResult.isTracingNormal(ValueTransformationExpressionEvaluationTraceType.class)) {
            trace = new ValueTransformationExpressionEvaluationTraceType(prismContext);
            parentResult.addTrace(trace);
        } else {
            trace = null;
        }
    }

    void recordEvaluationStart(ValueTransformationEvaluationModeType mode) throws SchemaException {
        if (trace != null) {
            E expressionEvaluatorBean = evaluator.getExpressionEvaluatorBean();
            if (expressionEvaluatorBean != null) {
                ExpressionEvaluatorWrapperType evaluatorWrapper = new ExpressionEvaluatorWrapperType();
                //noinspection unchecked
                evaluatorWrapper.setExpressionEvaluator(new JAXBElement<>(
                        evaluator.getElementName(), (Class<E>) expressionEvaluatorBean.getClass(), CloneUtil.clone(expressionEvaluatorBean)));
                trace.setExpressionEvaluator(evaluatorWrapper);
            }
            for (Source<?, ?> source : context.getSources()) {
                trace.beginSource()
                        .name(source.getName())
                        .itemDeltaItem(source.toItemDeltaItemType());
            }
            trace.skipEvaluationPlus(context.isSkipEvaluationPlus())
                    .skipEvaluationMinus(context.isSkipEvaluationMinus())
                    .contextDescription(context.getContextDescription())
                    .localContextDescription(context.getLocalContextDescription())
                    .evaluationMode(mode);
        }
    }

    void recordEvaluationEnd(PrismValueDeltaSetTriple<V> outputTriple) {
        if (trace != null && outputTriple != null) {
            trace.setOutput(DeltaSetTripleType.fromDeltaSetTriple(outputTriple));
        }
    }
}
