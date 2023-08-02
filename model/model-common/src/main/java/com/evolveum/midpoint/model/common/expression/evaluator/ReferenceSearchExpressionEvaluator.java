/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReferenceSearchExpressionEvaluatorType;

/**
 * Creates a generic reference (or references) based on specified condition for the referenced object.
 * (It seems to be not much used.)
 *
 * @author Radovan Semancik
 */
public class ReferenceSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<
        PrismReferenceValue,
        ObjectType,
        PrismReferenceDefinition,
        ReferenceSearchExpressionEvaluatorType> {

    ReferenceSearchExpressionEvaluator(
            QName elementName,
            ReferenceSearchExpressionEvaluatorType expressionEvaluatorType,
            PrismReferenceDefinition outputDefinition,
            Protector protector,
            ObjectResolver objectResolver,
            LocalizationService localizationService) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, objectResolver, localizationService);
    }

    @Override
    Evaluation createEvaluation(
            VariablesMap variables,
            PlusMinusZero valueDestination,
            boolean useNew,
            ExpressionEvaluationContext context,
            String contextDescription,
            Task task,
            OperationResult result) throws SchemaException {
        return new Evaluation(variables, valueDestination, useNew, context, contextDescription, task, result) {
            @Override
            protected PrismReferenceValue createResultValue(
                    String oid,
                    PrismObject<ObjectType> object,
                    List<ItemDelta<PrismReferenceValue, PrismReferenceDefinition>> newValueDeltas) {
                // Value deltas are ignored here (they cannot be applied to a reference, anyway).
                PrismReferenceValue refVal = prismContext.itemFactory().createReferenceValue();
                refVal.setOid(oid);
                refVal.setTargetType(targetTypeQName);
                refVal.setRelation(expressionEvaluatorBean.getRelation());
                refVal.setObject(object);
                return refVal;
            }
        };
    }

    @Override
    public String shortDebugDump() {
        return "referenceSearchExpression";
    }
}
