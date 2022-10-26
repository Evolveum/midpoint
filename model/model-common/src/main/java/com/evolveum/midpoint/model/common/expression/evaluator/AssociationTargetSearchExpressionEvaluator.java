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

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

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
            PrismContext prismContext,
            ObjectResolver objectResolver,
            ModelService modelService,
            ModelInteractionService modelInteractionService,
            SecurityContextManager securityContextManager,
            LocalizationService localizationService,
            CacheConfigurationManager cacheConfigurationManager) {
        super(
                elementName,
                expressionEvaluatorBean,
                outputDefinition,
                protector,
                prismContext,
                objectResolver,
                modelService,
                modelInteractionService,
                securityContextManager,
                localizationService,
                cacheConfigurationManager);
    }

    @Override
    protected AbstractSearchExpressionEvaluatorCache<PrismContainerValue<ShadowAssociationType>, ShadowType, ?, ?> getCache() {
        return AssociationSearchExpressionEvaluatorCache.getCache();
    }

    @Override
    protected Class<?> getCacheClass() {
        return AssociationSearchExpressionEvaluatorCache.class;
    }

    @Override
    protected CacheType getCacheType() {
        return CacheType.LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE;
    }

    @Override
    protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext params) throws ExpressionEvaluationException {
        @SuppressWarnings("unchecked")
        TypedValue<ResourceObjectTypeDefinition> rAssocTargetDefTypedValue =
                params.getVariables().get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
        if (rAssocTargetDefTypedValue == null || rAssocTargetDefTypedValue.getValue() == null) {
            throw new ExpressionEvaluationException("No association target object definition variable in "+
                    params.getContextDescription()+"; the expression may be used in a wrong place. It is only supposed to create an association.");
        }
        ResourceObjectTypeDefinition rAssocTargetDef = (ResourceObjectTypeDefinition) rAssocTargetDefTypedValue.getValue();
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
    protected void extendOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean searchOnResource) {
        super.extendOptions(options, searchOnResource);
        // We do not need to worry about associations of associations here
        // (nested associations). Avoiding that will make the query faster.
        options.add(
                SelectorOptions.create(
                        prismContext.toUniformPath(ShadowType.F_ASSOCIATION),
                        GetOperationOptions.createDontRetrieve()));
    }

    @Override
    protected boolean isAcceptable(@NotNull PrismObject<ShadowType> object) {
        return ShadowUtil.isNotDead(object.asObjectable());
    }

    protected PrismContainerValue<ShadowAssociationType> createPrismValue(
            String oid,
            PrismObject object,
            QName targetTypeQName,
            List<ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> additionalAttributeDeltas,
            ExpressionEvaluationContext params) {
        ShadowAssociationType association = new ShadowAssociationType()
                .name(params.getMappingQName())
                .shadowRef(oid, targetTypeQName);

        association.getShadowRef().asReferenceValue().setObject(object);

        //noinspection unchecked
        PrismContainerValue<ShadowAssociationType> associationCVal = association.asPrismContainerValue();

        try {

            if (additionalAttributeDeltas != null) {
                ItemDeltaCollectionsUtil.applyTo(additionalAttributeDeltas, associationCVal);
            }

            prismContext.adopt(associationCVal, ShadowType.COMPLEX_TYPE, ShadowType.F_ASSOCIATION);
            if (InternalsConfig.consistencyChecks) {
                associationCVal.assertDefinitions(
                        () -> "associationCVal in assignment expression in " + params.getContextDescription());
            }
        } catch (SchemaException e) {
            // Should not happen
            throw new SystemException(e);
        }

        return associationCVal;
    }

    @Override
    protected QName getDefaultTargetType() {
        return ShadowType.COMPLEX_TYPE;
    }

    @Override
    public String shortDebugDump() {
        return "associationExpression";
    }
}
