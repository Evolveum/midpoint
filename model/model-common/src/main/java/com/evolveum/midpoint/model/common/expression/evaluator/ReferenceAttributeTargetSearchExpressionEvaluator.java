/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Creates {@link ShadowReferenceAttributeValue} (or more or them) based on specified condition for the associated object.
 *
 * It is based on `associationTargetSearch` evaluator that is normally mapped to {@link AssociationTargetSearchExpressionEvaluator}
 * object. But if used in the context of reference attributes, this class is instantiated instead.
 *
 * @author Radovan Semancik
 *
 * @see AssociationTargetSearchExpressionEvaluator
 */
class ReferenceAttributeTargetSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<ShadowReferenceAttributeValue, ShadowType, ShadowReferenceAttributeDefinition, SearchObjectExpressionEvaluatorType> {

    ReferenceAttributeTargetSearchExpressionEvaluator(
            QName elementName,
            SearchObjectExpressionEvaluatorType expressionEvaluatorBean,
            ShadowReferenceAttributeDefinition outputDefinition,
            Protector protector,
            ObjectResolver objectResolver,
            LocalizationService localizationService) {
        super(
                elementName,
                expressionEvaluatorBean,
                outputDefinition,
                protector,
                objectResolver,
                localizationService);
    }

    @Override
    Evaluation createEvaluation(
            @NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws SchemaException {
        return new Evaluation(vtCtx, result) {

            @Override
            protected QName getDefaultTargetType() {
                return ShadowType.COMPLEX_TYPE;
            }

            @Override
            protected @NotNull ShadowReferenceAttributeValue createResultValue(
                    String targetOid,
                    @NotNull QName targetTypeName,
                    @Nullable PrismObject<ShadowType> target,
                    List<ItemDelta<ShadowReferenceAttributeValue, ShadowReferenceAttributeDefinition>> newValueDeltasIgnored)
                    throws SchemaException {

                return ShadowReferenceAttributeValue.fromReferencable(
                        ObjectTypeUtil.createObjectRef(targetOid, ObjectTypes.SHADOW, target));
            }

            @Override
            protected ObjectQuery extendQuery(ObjectQuery query) {

                query.setFilter(
                        prismContext.queryFactory()
                                .createAnd(
                                        outputDefinition.createTargetObjectsFilter(true),
                                        query.getFilter()));
                return query;
            }

            @Override
            protected ObjectQuery createRawQuery(SearchFilterType filter) throws SchemaException, ExpressionEvaluationException {
                var concreteShadowDef =
                        outputDefinition
                                .getGeneralizedObjectSideObjectDefinition()
                                .getPrismObjectDefinition();
                var objFilter = prismContext.getQueryConverter().createObjectFilter(concreteShadowDef, filter);
                return prismContext.queryFactory().createQuery(objFilter);
            }

            @Override
            protected void extendOptions(GetOperationOptionsBuilder builder, boolean searchOnResource) {
                super.extendOptions(builder, searchOnResource);
                // We do not need to worry about associations of associations here
                // (nested associations). Avoiding that will make the query faster.
                builder.item(ShadowType.F_ASSOCIATIONS).dontRetrieve();
            }

            @Override
            protected boolean isAcceptable(@NotNull PrismObject<ShadowType> object) {
                // FIXME do additional filtering for the targets (if there are multiple types for them)
                return ShadowUtil.isNotDead(object.asObjectable());
            }
        };
    }

    @Override
    public String shortDebugDump() {
        return "associationTargetSearch for reference attributes";
    }
}
