/*
 * Copyright (c) 2020-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.filter;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;

import com.evolveum.midpoint.util.DOMUtil;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Evaluation of the "filter" expression.
 */
class FilterExpressionEvaluation<V extends PrismValue, D extends ItemDefinition<?>> {

    private final FilterExpressionEvaluator<V, D> evaluator;
    private final ValueTransformationContext vtCtx;

    FilterExpressionEvaluation(FilterExpressionEvaluator<V, D> evaluator, ValueTransformationContext vtCtx) {
        this.evaluator = evaluator;
        this.vtCtx = vtCtx;
    }

    List<V> evaluate(OperationResult result) throws ExpressionEvaluationException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {

        PrismObject<?> object = determineObject();
        boolean match = false;
        if (object != null) {
            ObjectFilter objectFilter = PrismContext.get().getQueryConverter().parseFilter(evaluator.getExpressionEvaluatorBean().getFilter(), object.getDefinition());
            // TODO: call repository instead?
            match = objectFilter.match(object.getValue(), PrismContext.get().getMatchingRuleRegistry());
        }
        return prepareOutput(match);
    }

    private PrismObject<?> determineObject() {
        var eeCtx = vtCtx.getExpressionEvaluationContext();
        Object defaultVariableValue = eeCtx.getVariables().getValue(null);
        if (defaultVariableValue == null) {
            throw new IllegalStateException("Filter expression cannot determine object to work on (default variable is null). In " +
                    vtCtx.getContextDescription());
        }
        if (defaultVariableValue instanceof PrismObject<?> obj) {
            return obj;
        }
        if (defaultVariableValue instanceof ObjectDeltaObject<?> odo) {
            if (vtCtx.isEvaluateNew()) {
                return odo.getNewObject();
            } else {
                return odo.getOldObject();
            }
        } else {
            throw new IllegalStateException("Filter expression cannot determine object to work on (default variable contains " +
                    defaultVariableValue.getClass().getSimpleName() + "). In " +
                    vtCtx.getContextDescription());
        }
    }

    private List<V> prepareOutput(boolean match) throws SchemaException, ExpressionEvaluationException {
        D outputDefinition = evaluator.getOutputDefinition();
        if (!(outputDefinition instanceof PrismPropertyDefinition<?>)) {
            throw new IllegalStateException("Result of filter expression can only be a property. In " +
                    vtCtx.getContextDescription());
        }
        if (!DOMUtil.XSD_BOOLEAN.equals(((PrismPropertyDefinition<?>)outputDefinition).getTypeName())) {
            throw new IllegalStateException("Result of filter expression can only be boolean property, not " +
                    ((PrismPropertyDefinition<?>)outputDefinition).getTypeName().getLocalPart() +
                    ". In " + vtCtx.getContextDescription());
        }
        List<V> list = new ArrayList<>();
        list.add(ExpressionUtil.convertToPrismValue(
                match, outputDefinition, vtCtx.getContextDescription()));
        return list;
    }

}
