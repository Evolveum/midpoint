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
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReferenceSearchExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

/**
 * Creates a generic reference (or references) based on specified condition for the referenced object.
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
            @NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws SchemaException {
        return new Evaluation(vtCtx, result) {
            @Override
            protected @NotNull PrismReferenceValue createResultValue(
                    String oid,
                    @NotNull QName objectTypeName,
                    PrismObject<ObjectType> object,
                    List<ItemDelta<PrismReferenceValue, PrismReferenceDefinition>> newValueDeltas)
                    throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                    ConfigurationException, ObjectNotFoundException {
                var relation =
                        determineRelation(expressionEvaluatorBean.getRelation(), expressionEvaluatorBean.getRelationExpression());
                return createReferenceValue(oid, objectTypeName, object, relation);
            }
        };
    }

    @Override
    public String shortDebugDump() {
        return "referenceSearchExpression";
    }
}
