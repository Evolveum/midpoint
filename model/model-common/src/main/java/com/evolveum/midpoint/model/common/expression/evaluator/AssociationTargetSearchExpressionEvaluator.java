/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Creates an association (or associations) based on specified condition for the associated object.
 *
 * @author Radovan Semancik
 */
class AssociationTargetSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<
                PrismContainerValue<ShadowAssociationValueType>,
                ShadowType,
                ShadowAssociationDefinition,
                SearchObjectExpressionEvaluatorType> {

    AssociationTargetSearchExpressionEvaluator(
            QName elementName,
            SearchObjectExpressionEvaluatorType expressionEvaluatorBean,
            ShadowAssociationDefinition outputDefinition,
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
            protected @NotNull PrismContainerValue<ShadowAssociationValueType> createResultValue(
                    String oid,
                    @NotNull QName objectTypeName,
                    PrismObject<ShadowType> object,
                    List<ItemDelta<PrismContainerValue<ShadowAssociationValueType>, ShadowAssociationDefinition>> newValueDeltas)
                    throws SchemaException {

                var newAssociation = outputDefinition.instantiate();

                var targetRef = ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SHADOW);
                targetRef.asReferenceValue().setObject(object); // may be null
                var newAssociationValue = newAssociation.createNewValueForTargetRef(targetRef);

                if (newValueDeltas != null) {
                    ItemDeltaCollectionsUtil.applyTo(newValueDeltas, newAssociationValue);
                }

                if (InternalsConfig.consistencyChecks) {
                    newAssociationValue.assertDefinitions(() -> "associationCVal in assignment expression in " + vtCtx);
                }
                return newAssociationValue.clone(); // It needs to be parent-less when included in the output triple
            }

            private ResourceObjectTypeDefinition associationTargetDef()
                    throws ExpressionEvaluationException {
                @SuppressWarnings("unchecked")
                var rAssocTargetDefTypedValue = (TypedValue<ResourceObjectTypeDefinition>)
                        this.vtCtx.getVariablesMap().get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
                if (rAssocTargetDefTypedValue == null || rAssocTargetDefTypedValue.getValue() == null) {
                    throw new ExpressionEvaluationException(
                            String.format("No association target object definition variable in %s; the expression may be used in"
                                            + " a wrong place. It is only supposed to create an association.",
                                    vtCtx));
                }
                return (ResourceObjectTypeDefinition) rAssocTargetDefTypedValue.getValue();
            }

            @Override
            protected ObjectQuery extendQuery(ObjectQuery query) throws ExpressionEvaluationException {
                var rAssocTargetDef = associationTargetDef();
                ObjectFilter coordinatesFilter = prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(rAssocTargetDef.getResourceOid())
                        .and().item(ShadowType.F_KIND).eq(rAssocTargetDef.getKind())
                        .and().item(ShadowType.F_INTENT).eq(rAssocTargetDef.getIntent())
                        .buildFilter();
                query.setFilter(
                        prismContext.queryFactory()
                                .createAnd(coordinatesFilter, query.getFilter()));
                return query;
            }

            @Override
            protected ObjectQuery createRawQuery(SearchFilterType filter) throws SchemaException, ExpressionEvaluationException {
                var concreteShadowDef = associationTargetDef().getPrismObjectDefinition();
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
                return ShadowUtil.isNotDead(object.asObjectable());
            }

            /**
             * Create on demand used in AssociationTargetSearch would fail
             * @return false
             */
            @Override
            protected boolean isCreateOnDemandSafe() {
                return false;
            }

            @Override
            protected CacheInfo getCacheInfo() {
                return new CacheInfo(
                        AssociationSearchExpressionEvaluatorCache.getCache(),
                        AssociationSearchExpressionEvaluatorCache.class,
                        CacheType.LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE,
                        ShadowType.class);
            }

        };
    }

    @Override
    public String shortDebugDump() {
        return "associationExpression";
    }
}
