/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReferenceSearchExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

/**
 * Creates a reference pointing to the owner of the provided shadow (if it has an owner).
 *
 * Currently, the input is expected to be named `input` with a type of {@link ShadowAssociationValueType};
 * this is true if this evaluator is be used as an inbound mapping for an association.
 *
 * TODO generalize / document this
 */
class ShadowOwnerReferenceSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<
        PrismReferenceValue,
        ObjectType,
        PrismReferenceDefinition,
        ReferenceSearchExpressionEvaluatorType> {

    ShadowOwnerReferenceSearchExpressionEvaluator(
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

        //noinspection DuplicatedCode
        return new Evaluation(vtCtx, result) {

            @Override
            protected QName getDefaultTargetType() {
                return FocusType.COMPLEX_TYPE;
            }

            @Override
            protected @NotNull List<ObjectQuery> createQueries() throws ConfigurationException, SchemaException {
                configCheck(expressionEvaluatorBean.getFilter().isEmpty(),
                        "Filter is not supported in shadow owner reference search expression: %s",
                        lazy(() -> shortDebugDump()));
                var input = vtCtx.getVariablesMap().getValue(ExpressionConstants.VAR_INPUT, ShadowReferenceAttributeValue.class);
                if (input == null) {
                    return List.of();
                }
                var shadowRef = input.asObjectReferenceType();
                return List.of(
                        prismContext.queryFor(targetTypeClass)
                                .item(FocusType.F_LINK_REF)
                                .ref(shadowRef.asReferenceValue().clone())
                                .build());
            }

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
        return "shadowOwnerReferenceSearchExpression";
    }
}
