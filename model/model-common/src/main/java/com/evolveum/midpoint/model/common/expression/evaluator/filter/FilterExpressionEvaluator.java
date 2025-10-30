/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.filter;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Filter expression evaluator.
 * The expression evaluates whether a provided object matches the filter.
 * This is a boolean expression, only able to return true or false.
 *
 * It is a part of {@link Expression} and {@link ExpressionEvaluator} framework.
 *
 * @author Radovan Semancik
 */
public class FilterExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
                extends AbstractValueTransformationExpressionEvaluator<V, D, FilterExpressionEvaluatorType> {

    FilterExpressionEvaluator(
            QName elementName,
            FilterExpressionEvaluatorType evaluatorBean,
            D outputDefinition,
            Protector protector,
            LocalizationService localizationService) {
        super(elementName, evaluatorBean, outputDefinition, protector, localizationService);
    }

    @Override
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) {
        // TODO!
        // Do nothing here. The profile will be checked inside ScriptExpression.
    }

    @Override
    protected @NotNull List<V> transformSingleValue(
            @NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        return new FilterExpressionEvaluation<>(this, vtCtx)
                .evaluate(result);
    }

    @Override
    public String shortDebugDump() {
        return "filter: "+getExpressionEvaluatorBean().toString();
    }
}
