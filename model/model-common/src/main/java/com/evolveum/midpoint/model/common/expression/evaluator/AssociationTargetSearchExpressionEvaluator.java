/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.model.common.expression.evaluator.AssociationRelatedEvaluatorUtil.getAssociationDefinition;

/**
 * Creates an association (or associations) based on specified condition for the associated object.
 *
 * @author Radovan Semancik
 */
public class AssociationTargetSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<
                PrismContainerValue<ShadowAssociationType>,
                ShadowType,
                PrismContainerDefinition<ShadowAssociationType>,
                SearchObjectExpressionEvaluatorType> {

    AssociationTargetSearchExpressionEvaluator(
            QName elementName,
            SearchObjectExpressionEvaluatorType expressionEvaluatorBean,
            PrismContainerDefinition<ShadowAssociationType> outputDefinition,
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
            VariablesMap variables,
            PlusMinusZero valueDestination,
            boolean useNew,
            ExpressionEvaluationContext context,
            String contextDescription,
            Task task,
            OperationResult result) throws SchemaException {
        return new Evaluation(variables, valueDestination, useNew, context, contextDescription, task, result) {

            @Override
            protected QName getDefaultTargetType() {
                return ShadowType.COMPLEX_TYPE;
            }

            @Override
            protected PrismContainerValue<ShadowAssociationType> createResultValue(
                    String oid,
                    PrismObject<ShadowType> object,
                    List<ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> newValueDeltas)
                    throws SchemaException {
                ShadowAssociationType association = new ShadowAssociationType()
                        .name(context.getMappingQName())
                        .shadowRef(oid, targetTypeQName);

                association.getShadowRef().asReferenceValue().setObject(object);

                //noinspection unchecked
                PrismContainerValue<ShadowAssociationType> associationCVal = association.asPrismContainerValue();
                if (newValueDeltas != null) {
                    ItemDeltaCollectionsUtil.applyTo(newValueDeltas, associationCVal);
                }
                prismContext.adopt(associationCVal, ShadowType.COMPLEX_TYPE, ShadowType.F_ASSOCIATION);
                if (InternalsConfig.consistencyChecks) {
                    associationCVal.assertDefinitions(
                            () -> "associationCVal in assignment expression in " + context.getContextDescription());
                }
                return associationCVal;
            }

            @Override
            protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext context)
                    throws ExpressionEvaluationException {

                var associationDefinition = getAssociationDefinition(context);

                ObjectFilter coordinatesFilter;
                var coordinatesFilterBuilder = prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(associationDefinition.getResourceOid());

                if (!associationDefinition.hasMultipleIntents()) {
                    var associationTargetDef = associationDefinition.getAssociationTarget(); // there should be exactly one
                    coordinatesFilter = coordinatesFilterBuilder
                            .and().item(ShadowType.F_KIND).eq(associationTargetDef.getKind())
                            .and().item(ShadowType.F_INTENT).eq(associationTargetDef.getIntent())
                            .buildFilter();
                } else {
                    // There are multiple intents here. Unfortunately, we cannot use them as filtering criteria, because of the
                    // provisioning module limitations: only one kind/intent is allowed. So, we'll search for all the objects
                    // of the given object class, as it was done before 4.6 (see commit 473467d303225b207a2f8da820ee08f7cc786791).
                    // We'll filter out unwanted objects later in isAcceptable() method.
                    coordinatesFilter = coordinatesFilterBuilder
                            .and().item(ShadowType.F_OBJECT_CLASS).eq(associationDefinition.getObjectClassName())
                            .buildFilter();
                }
                query.setFilter(
                        prismContext.queryFactory()
                                .createAnd(coordinatesFilter, query.getFilter()));
                return query;
            }

            @Override
            protected void extendOptions(
                    Collection<SelectorOptions<GetOperationOptions>> options, boolean searchOnResource) {
                super.extendOptions(options, searchOnResource);
                // We do not need to worry about associations of associations here
                // (nested associations). Avoiding that will make the query faster.
                options.add(
                        SelectorOptions.create(
                                prismContext.toUniformPath(ShadowType.F_ASSOCIATION),
                                GetOperationOptions.createDontRetrieve()));
            }

            @Override
            protected boolean isAcceptable(@NotNull PrismObject<ShadowType> object, ExpressionEvaluationContext context)
                    throws ExpressionEvaluationException {
                if (ShadowUtil.isDead(object.asObjectable())) {
                    return false;
                }
                var associationDefinition = getAssociationDefinition(context);
                if (!associationDefinition.hasMultipleIntents()) {
                    return true; // we searched for specific kind+intent (see above)
                }
                var shadow = object.asObjectable();
                return shadow.getKind() == associationDefinition.getKind()
                        && associationDefinition.getIntents().contains(shadow.getIntent());
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

            protected ObjectQuery createRawQuery(ExpressionEvaluationContext context)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException {
                SearchFilterType filterBean =
                        MiscUtil.configNonNull(expressionEvaluatorBean.getFilter(), () -> "No filter in " + shortDebugDump());

                var associationTargetDef = getAssociationDefinition(context);
                var concreteShadowDef = associationTargetDef.getAssociationTarget().getPrismObjectDefinition();
                var filter = prismContext.getQueryConverter().createObjectFilter(concreteShadowDef, filterBean);
                return prismContext.queryFactory().createQuery(filter);
            };
        };
    }

    @Override
    public String shortDebugDump() {
        return "associationExpression";
    }
}
