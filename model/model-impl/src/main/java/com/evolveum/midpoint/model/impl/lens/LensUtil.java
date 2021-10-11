/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.VirtualAssignmenetSpecification;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.getSingleValue;
import static java.util.Collections.emptySet;

/**
 * @author semancik
 *
 */
public class LensUtil {

    private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);

    public static <F extends ObjectType> ResourceType getResourceReadOnly(LensContext<F> context,
                                                                  String resourceOid, ProvisioningService provisioningService, Task task, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ResourceType resourceType = context.getResource(resourceOid);
        if (resourceType == null) {
            // Fetching from provisioning to take advantage of caching and
            // pre-parsed schema
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
            resourceType = provisioningService.getObject(ResourceType.class, resourceOid, options, task, result)
                    .asObjectable();
            context.rememberResource(resourceType);
        }
        return resourceType;
    }

    @NotNull
    static <F extends ObjectType> ResourceType getResourceReadOnly(LensContext<F> context, String resourceOid, ObjectResolver objectResolver,
            Task task, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ResourceType resourceFromContext = context.getResource(resourceOid);
        if (resourceFromContext != null) {
            return resourceFromContext;
        } else {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
            ResourceType resourceFetched = objectResolver.getObject(ResourceType.class, resourceOid, options, task, result);
            context.rememberResource(resourceFetched);
            return resourceFetched;
        }
    }

    public static String refineProjectionIntent(ShadowKindType kind, String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        RefinedObjectClassDefinition rObjClassDef = refinedSchema.getRefinedDefinition(kind, intent);
        if (rObjClassDef == null) {
            LOGGER.error("No projection definition for kind={}, intent={} in {}", kind, intent, resource);
            LOGGER.error("Diagnostic output follows:\n\nResource:\n{}\n\nRefined resource schema:\n{}",
                    resource.asPrismObject().debugDump(), refinedSchema.debugDump());
            throw new SchemaException("No projection definition for kind="+kind+" intent="+intent+" in "+resource);
        }
        return rObjClassDef.getIntent();
    }

    public static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context, PrismObject<ShadowType> equivalentAccount,
                                                                                   ProvisioningService provisioningService, PrismContext prismContext,
                                                                                   Task task, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ShadowType equivalentAccountType = equivalentAccount.asObjectable();
        ShadowKindType kind = ShadowUtil.getKind(equivalentAccountType);
        return getProjectionContext(context, ShadowUtil.getResourceOid(equivalentAccountType),
                kind, equivalentAccountType.getIntent(), equivalentAccountType.getTag(), provisioningService,
                prismContext, task, result);
    }

    private static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context, String resourceOid,
                                                                                   ShadowKindType kind, String intent, String tag,
                                                                                   ProvisioningService provisioningService, PrismContext prismContext,
                                                                                   Task task, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ResourceType resource = getResourceReadOnly(context, resourceOid, provisioningService, task, result);
        String refinedIntent = refineProjectionIntent(kind, intent, resource, prismContext);
        ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, refinedIntent, tag, false);
        return context.findProjectionContext(rsd);
    }

    public static <F extends ObjectType> LensProjectionContext getOrCreateProjectionContext(LensContext<F> context,
            ResourceShadowDiscriminator rsd) {
        LensProjectionContext accountSyncContext = context.findProjectionContext(rsd);
        if (accountSyncContext == null) {
            accountSyncContext = context.createProjectionContext(rsd);
            ResourceType resource = context.getResource(rsd.getResourceOid());
            accountSyncContext.setResource(resource);
        }
        accountSyncContext.setDoReconciliation(context.isDoReconciliationForAllProjections());
        return accountSyncContext;
    }

    public static <F extends ObjectType> LensProjectionContext createAccountContext(LensContext<F> context, ResourceShadowDiscriminator rsd){
        return new LensProjectionContext(context, rsd);
    }

    public static <V extends PrismValue, D extends ItemDefinition> V cloneAndApplyMetadata(V value, boolean isAssignment,
            Collection<ItemValueWithOrigin<V, D>> origins) throws SchemaException {
        return cloneAndApplyMetadata(value, isAssignment, () -> getAutoCreationIdentifier(origins));
    }

//    public static <V extends PrismValue> Collection<V> cloneAndApplyMetadata(Collection<V> values, boolean isAssignment,
//            MappingType mapping) throws SchemaException {
//        List<V> rv = new ArrayList<>();
//        for (V value : values) {
//            rv.add(cloneAndApplyMetadata(value, isAssignment, mapping::getName));
//        }
//        return rv;
//    }

    public static <V extends PrismValue> V cloneAndApplyMetadata(V value, boolean isAssignment,
            PrismValueDeltaSetTripleProducer<?, ?> mapping) throws SchemaException {
        return cloneAndApplyMetadata(value, isAssignment, mapping::getIdentifier);
    }

    public static <V extends PrismValue> V cloneAndApplyMetadata(V value, boolean isAssignment,
            MappingType mapping) throws SchemaException {
        return cloneAndApplyMetadata(value, isAssignment, mapping::getName);
    }

    private static <V extends PrismValue> V cloneAndApplyMetadata(V value, boolean isAssignment,
            Supplier<String> originMappingNameSupplier) throws SchemaException {
        //noinspection unchecked
        V cloned = (V) value.clone();
        if (isAssignment && cloned instanceof PrismContainerValue) {
            ((PrismContainerValue) cloned).setId(null);
            String originMappingName = originMappingNameSupplier.get();
            LOGGER.trace("cloneAndApplyMetadata: originMappingName = {}", originMappingName);
            if (originMappingName != null) {
                //noinspection unchecked
                PrismContainer<MetadataType> metadataContainer = ((PrismContainerValue) cloned).findOrCreateContainer(AssignmentType.F_METADATA);
                metadataContainer.getValue().asContainerable().setOriginMappingName(originMappingName);
            }
        }
        return cloned;
    }

    private static <V extends PrismValue, D extends ItemDefinition> String getAutoCreationIdentifier(Collection<ItemValueWithOrigin<V, D>> origins) {
        // let's ignore conflicts (name1 vs name2, named vs unnamed) for now
        for (ItemValueWithOrigin<V, D> origin : origins) {
            if (origin.getMapping() != null && origin.getMapping().getIdentifier() != null) {
                return origin.getMapping().getIdentifier();
            }
        }
        return null;
    }

    public static PropertyDelta<XMLGregorianCalendar> createActivationTimestampDelta(ActivationStatusType status,
            XMLGregorianCalendar now,
            PrismContainerDefinition<ActivationType> activationDefinition, OriginType origin,
            PrismContext prismContext) {
        ItemName timestampPropertyName;
        if (status == null || status == ActivationStatusType.ENABLED) {
            timestampPropertyName = ActivationType.F_ENABLE_TIMESTAMP;
        } else if (status == ActivationStatusType.DISABLED) {
            timestampPropertyName = ActivationType.F_DISABLE_TIMESTAMP;
        } else if (status == ActivationStatusType.ARCHIVED) {
            timestampPropertyName = ActivationType.F_ARCHIVE_TIMESTAMP;
        } else {
            throw new IllegalArgumentException("Unknown activation status "+status);
        }

        PrismPropertyDefinition<XMLGregorianCalendar> timestampDef = activationDefinition.findPropertyDefinition(timestampPropertyName);
        PropertyDelta<XMLGregorianCalendar> timestampDelta
                = timestampDef.createEmptyDelta(FocusType.F_ACTIVATION.append(timestampPropertyName));
        timestampDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(now, origin, null));
        return timestampDelta;
    }

    public static <F extends ObjectType> void moveTriggers(LensProjectionContext projCtx, LensFocusContext<F> focusCtx) throws SchemaException {
        ObjectDelta<ShadowType> projSecondaryDelta = projCtx.getSecondaryDelta();
        if (projSecondaryDelta == null) {
            return;
        }
        Collection<? extends ItemDelta> modifications = projSecondaryDelta.getModifications();
        Iterator<? extends ItemDelta> iterator = modifications.iterator();
        while (iterator.hasNext()) {
            ItemDelta projModification = iterator.next();
            LOGGER.trace("MOD: {}\n{}", projModification.getPath(), projModification.debugDumpLazily());
            if (projModification.getPath().equivalent(SchemaConstants.PATH_TRIGGER)) {
                focusCtx.swallowToProjectionWaveSecondaryDelta(projModification);
                iterator.remove();
            }
        }
    }

    public static Object getIterationVariableValue(LensProjectionContext accCtx) {
        Integer iterationOld = null;
        PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
        if (shadowCurrent != null) {
            iterationOld = shadowCurrent.asObjectable().getIteration();
        }
        if (iterationOld == null) {
            return accCtx.getIteration();
        }
        PrismPropertyDefinition<Integer> propDef = accCtx.getPrismContext().definitionFactory().createPropertyDefinition(ExpressionConstants.VAR_ITERATION_QNAME,
                DOMUtil.XSD_INT);
        PrismProperty<Integer> propOld = propDef.instantiate();
        propOld.setRealValue(iterationOld);
        PropertyDelta<Integer> propDelta = propDef.createEmptyDelta(ExpressionConstants.VAR_ITERATION_QNAME);
        propDelta.setRealValuesToReplace(accCtx.getIteration());
        PrismProperty<Integer> propNew = propDef.instantiate();
        propNew.setRealValue(accCtx.getIteration());
        ItemDeltaItem<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> idi = new ItemDeltaItem<>(propOld, propDelta, propNew, propDef);
        return idi;
    }

    public static Object getIterationTokenVariableValue(LensProjectionContext accCtx) {
        String iterationTokenOld = null;
        PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
        if (shadowCurrent != null) {
            iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
        }
        if (iterationTokenOld == null) {
            return accCtx.getIterationToken();
        }
        PrismPropertyDefinition<String> propDef = accCtx.getPrismContext().definitionFactory().createPropertyDefinition(
                ExpressionConstants.VAR_ITERATION_TOKEN_QNAME, DOMUtil.XSD_STRING);
        PrismProperty<String> propOld = propDef.instantiate();
        propOld.setRealValue(iterationTokenOld);
        PropertyDelta<String> propDelta = propDef.createEmptyDelta(ExpressionConstants.VAR_ITERATION_TOKEN_QNAME);
        propDelta.setRealValuesToReplace(accCtx.getIterationToken());
        PrismProperty<String> propNew = propDef.instantiate();
        propNew.setRealValue(accCtx.getIterationToken());
        ItemDeltaItem<PrismPropertyValue<String>,PrismPropertyDefinition<String>> idi = new ItemDeltaItem<>(propOld, propDelta, propNew, propDef);
        return idi;
    }

    /**
     * Extracts the delta from this projection context and also from all other projection contexts that have
     * equivalent discriminator.
     */
    public static <F extends ObjectType, T> PropertyDelta<T> findAPrioriDelta(LensContext<F> context,
            LensProjectionContext projCtx, ItemPath projectionPropertyPath) throws SchemaException {
        PropertyDelta<T> aPrioriDelta = null;
        for (LensProjectionContext aProjCtx: findRelatedContexts(context, projCtx)) {
            ObjectDelta<ShadowType> aProjDelta = aProjCtx.getDelta();
            if (aProjDelta != null) {
                PropertyDelta<T> aPropProjDelta = aProjDelta.findPropertyDelta(projectionPropertyPath);
                if (aPropProjDelta != null) {
                    if (aPrioriDelta == null) {
                        aPrioriDelta = aPropProjDelta.clone();
                    } else {
                        aPrioriDelta.merge(aPropProjDelta);
                    }
                }
            }
        }
        return aPrioriDelta;
    }

    /**
     * Extracts the delta from this projection context and also from all other projection contexts that have
     * equivalent discriminator.
     */
    public static <F extends ObjectType, T> ObjectDelta<ShadowType> findAPrioriDelta(LensContext<F> context,
            LensProjectionContext projCtx) throws SchemaException {
        ObjectDelta<ShadowType> aPrioriDelta = null;
        for (LensProjectionContext aProjCtx: findRelatedContexts(context, projCtx)) {
            ObjectDelta<ShadowType> aProjDelta = aProjCtx.getDelta();
            if (aProjDelta != null) {
                if (aPrioriDelta == null) {
                    aPrioriDelta = aProjDelta.clone();
                } else {
                    aPrioriDelta.merge(aProjDelta);
                }
            }
        }
        return aPrioriDelta;
    }

    /**
     * Returns a list of context that have equivalent discriminator with the reference context. Ordered by "order" in the
     * discriminator.
     */
    public static <F extends ObjectType> List<LensProjectionContext> findRelatedContexts(
            LensContext<F> context, LensProjectionContext refProjCtx) {
        List<LensProjectionContext> projCtxs = new ArrayList<>();
        ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
        if (refDiscr == null) {
            return projCtxs;
        }
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
            if (refDiscr.equivalent(aDiscr)) {
                projCtxs.add(aProjCtx);
            }
        }
        Comparator<? super LensProjectionContext> orderComparator = new Comparator<LensProjectionContext>() {
            @Override
            public int compare(LensProjectionContext ctx1, LensProjectionContext ctx2) {
                int order1 = ctx1.getResourceShadowDiscriminator().getOrder();
                int order2 = ctx2.getResourceShadowDiscriminator().getOrder();
                return Integer.compare(order1, order2);
            }
        };
        Collections.sort(projCtxs, orderComparator);
        return projCtxs;
    }

    public static <F extends ObjectType> boolean hasLowerOrderContext(LensContext<F> context,
            LensProjectionContext refProjCtx) {
        ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
            if (refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() > aDiscr.getOrder())) {
                return true;
            }
        }
        return false;
    }

    public static <F extends ObjectType> boolean hasDependentContext(LensContext<F> context,
            LensProjectionContext targetProjectionContext) {
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            for (ResourceObjectTypeDependencyType dependency: projectionContext.getDependencies()) {
                if (isDependencyTargetContext(projectionContext, targetProjectionContext, dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <F extends ObjectType> boolean isDependencyTargetContext(LensProjectionContext sourceProjContext, LensProjectionContext targetProjectionContext, ResourceObjectTypeDependencyType dependency) {
        ResourceShadowDiscriminator refDiscr = new ResourceShadowDiscriminator(dependency,
                sourceProjContext.getResource().getOid(), sourceProjContext.getKind());
        return targetProjectionContext.compareResourceShadowDiscriminator(refDiscr, false);
    }

    public static <F extends ObjectType> LensProjectionContext findLowerOrderContext(LensContext<F> context,
            LensProjectionContext refProjCtx) {
        int minOrder = -1;
        LensProjectionContext foundCtx = null;
        ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
            if (refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() > aDiscr.getOrder())) {
                if (minOrder < 0 || (aDiscr.getOrder() < minOrder)) {
                    minOrder = aDiscr.getOrder();
                    foundCtx = aProjCtx;
                }
            }
        }
        return foundCtx;
    }

    public static <T extends ObjectType, F extends ObjectType> void setContextOid(LensContext<F> context,
            LensElementContext<T> objectContext, String oid) {
        objectContext.setOid(oid);
        // Check if we need to propagate this oid also to higher-order contexts
        if (!(objectContext instanceof LensProjectionContext)) {
            return;
        }
        LensProjectionContext refProjCtx = (LensProjectionContext)objectContext;
        ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
        if (refDiscr == null) {
            return;
        }
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
            if (aDiscr != null && refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() < aDiscr.getOrder())) {
                aProjCtx.setOid(oid);
            }
        }
    }

    public static <F extends FocusType> PrismObjectDefinition<F> getFocusDefinition(LensContext<F> context) {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return null;
        }
        Class<F> typeClass = focusContext.getObjectTypeClass();
        return context.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(typeClass);
    }

    public static IterationSpecificationType getIterationSpecification(ObjectTemplateType objectTemplate) {
        return objectTemplate != null ? objectTemplate.getIterationSpecification() : null;
    }

    public static int determineMaxIterations(IterationSpecificationType iterationSpecType) {
        return iterationSpecType != null ? iterationSpecType.getMaxIterations() : 0;
    }

    public static <F extends ObjectType> String formatIterationToken(LensContext<F> context,
            LensElementContext<?> accountContext, IterationSpecificationType iterationType,
            int iteration, ExpressionFactory expressionFactory, ExpressionVariables variables,
            Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (iterationType == null) {
            return formatIterationTokenDefault(iteration);
        }
        ExpressionType tokenExpressionType = iterationType.getTokenExpression();
        if (tokenExpressionType == null) {
            return formatIterationTokenDefault(iteration);
        }
        PrismContext prismContext = context.getPrismContext();
        PrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.VAR_ITERATION_TOKEN_QNAME,
                DOMUtil.XSD_STRING);
        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(tokenExpressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), "iteration token expression in "+accountContext.getHumanReadableName(), task, result);

        Collection<Source<?,?>> sources = new ArrayList<>();
        MutablePrismPropertyDefinition<Integer> inputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.VAR_ITERATION_QNAME,
                DOMUtil.XSD_INT);
        inputDefinition.setMaxOccurs(1);
        PrismProperty<Integer> input = inputDefinition.instantiate();
        input.addRealValue(iteration);
        ItemDeltaItem<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> idi = new ItemDeltaItem<>(input);
        Source<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> iterationSource = new Source<>(idi, ExpressionConstants.VAR_ITERATION_QNAME);
        sources.add(iterationSource);

        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources , variables,
                "iteration token expression in "+accountContext.getHumanReadableName(), task);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, task, result);
        Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
        if (outputValues.isEmpty()) {
            return "";
        }
        if (outputValues.size() > 1) {
            throw new ExpressionEvaluationException("Iteration token expression in "+accountContext.getHumanReadableName()+" returned more than one value ("+outputValues.size()+" values)");
        }
        String realValue = outputValues.iterator().next().getValue();
        if (realValue == null) {
            return "";
        }
        return realValue;
    }

    public static String formatIterationTokenDefault(int iteration) {
        if (iteration == 0) {
            return "";
        }
        return Integer.toString(iteration);
    }

    public static <F extends ObjectType> boolean evaluateIterationCondition(LensContext<F> context,
            LensElementContext<?> accountContext, IterationSpecificationType iterationType,
            int iteration, String iterationToken, boolean beforeIteration,
            ExpressionFactory expressionFactory, ExpressionVariables variables, Task task, OperationResult result)
                    throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (iterationType == null) {
            return true;
        }
        ExpressionType expressionType;
        String desc;
        if (beforeIteration) {
            expressionType = iterationType.getPreIterationCondition();
            desc = "pre-iteration expression in "+accountContext.getHumanReadableName();
        } else {
            expressionType = iterationType.getPostIterationCondition();
            desc = "post-iteration expression in "+accountContext.getHumanReadableName();
        }
        if (expressionType == null) {
            return true;
        }
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(
                expressionType, ExpressionUtil.createConditionOutputDefinition(context.getPrismContext()), MiscSchemaUtil.getExpressionProfile(),
                desc, task, result);

        variables.put(ExpressionConstants.VAR_ITERATION, iteration, Integer.class);
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken, String.class);

        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, task);
        ExpressionEnvironment<?,?,?> env = new ExpressionEnvironment<>(context, null, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, env, result);
        Collection<PrismPropertyValue<Boolean>> outputValues = outputTriple.getNonNegativeValues();
        if (outputValues.isEmpty()) {
            return false;
        }
        if (outputValues.size() > 1) {
            throw new ExpressionEvaluationException(desc+" returned more than one value ("+outputValues.size()+" values)");
        }
        Boolean realValue = outputValues.iterator().next().getValue();
        if (realValue == null) {
            return false;
        }
        return realValue;

    }

    /**
     * Used for assignments and similar objects that do not have separate lifecycle.
     */
    public static boolean isAssignmentValid(AssignmentHolderType focus, AssignmentType assignment, XMLGregorianCalendar now,
            ActivationComputer activationComputer, LifecycleStateModelType focusStateModel) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef != null && QNameUtil.match(ArchetypeType.COMPLEX_TYPE, targetRef.getType())) {
            // Archetype assignments are always valid, even in non-valid lifecycle states.
            // The object cannot lose its (arche)type.
            return true;
        }
        String focusLifecycleState = focus.getLifecycleState();

        if (!activationComputer.lifecycleHasActiveAssignments(focusLifecycleState, focusStateModel)) {
            return false;
        }
        return isValid(assignment.getLifecycleState(), assignment.getActivation(), now, activationComputer, focusStateModel);
    }

    public static <R extends AbstractRoleType> Collection<AssignmentType> getForcedAssignments(LifecycleStateModelType lifecycleModel, String targetLifecycle,
            ObjectResolver objectResolver, PrismContext prismContext, Task task, OperationResult result) throws SchemaException,
    ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        VirtualAssignmenetSpecification<R> virtualAssignmenetSpecification = LifecycleUtil.getForcedAssignmentSpecification(lifecycleModel, targetLifecycle, prismContext);

        Collection<AssignmentType> forcedAssignments = new HashSet<>();
        if (virtualAssignmenetSpecification != null) {

            ResultHandler<R> handler = (object, parentResult)  -> {
                AssignmentType assignment = ObjectTypeUtil.createAssignmentTo(object, prismContext);
                return forcedAssignments.add(assignment);
            };

            objectResolver.searchIterative(virtualAssignmenetSpecification.getType(),
                   prismContext.queryFactory().createQuery(virtualAssignmenetSpecification.getFilter()), null, handler, task, result);

        }

        return forcedAssignments;
    }

    public static boolean isFocusValid(AssignmentHolderType focus, XMLGregorianCalendar now, ActivationComputer activationComputer, LifecycleStateModelType focusStateModel) {
        if (FocusType.class.isAssignableFrom(focus.getClass())) {
            return isValid(focus.getLifecycleState(),  ((FocusType) focus).getActivation(), now, activationComputer, focusStateModel);
        }
        return isValid(focus.getLifecycleState(),  null, now, activationComputer, focusStateModel);
    }

    private static boolean isValid(String lifecycleState, ActivationType activationType, XMLGregorianCalendar now, ActivationComputer activationComputer, LifecycleStateModelType focusStateModel) {
        TimeIntervalStatusType validityStatus = activationComputer.getValidityStatus(activationType, now);
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(lifecycleState, activationType, validityStatus, focusStateModel);
        return effectiveStatus == ActivationStatusType.ENABLED;
    }

    public static AssignmentPathVariables computeAssignmentPathVariables(AssignmentPathImpl assignmentPath) throws SchemaException {
        if (assignmentPath == null || assignmentPath.isEmpty()) {
            return null;
        }
        AssignmentPathVariables vars = new AssignmentPathVariables();
        vars.setAssignmentPath(assignmentPath.clone());

        Iterator<AssignmentPathSegmentImpl> iterator = assignmentPath.getSegments().iterator();
        while (iterator.hasNext()) {
            AssignmentPathSegmentImpl segment = iterator.next();
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> segmentAssignmentIdi = segment.getAssignmentIdi();

            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> magicAssignmentIdi;
            // Magic assignment
            if (vars.getMagicAssignment() == null) {
                magicAssignmentIdi = segmentAssignmentIdi.clone();
                vars.setMagicAssignment(magicAssignmentIdi);
            } else {
                // Collect extension values from the assignment extension
                magicAssignmentIdi = vars.getMagicAssignment();
                mergeExtension(magicAssignmentIdi, segmentAssignmentIdi);
            }

            // Collect extension values from the source object extension
            ObjectType segmentSource = segment.getSource();
            if (segmentSource != null) {
                mergeExtension(magicAssignmentIdi, segmentSource.asPrismObject());
            }

            // immediate assignment (use assignment from previous iteration)
            vars.setImmediateAssignment(vars.getThisAssignment());

            // this assignment
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> thisAssignment = segmentAssignmentIdi.clone();
            vars.setThisAssignment(thisAssignment);

            if (iterator.hasNext() && segmentSource instanceof AbstractRoleType) {
                vars.setImmediateRole((PrismObject<? extends AbstractRoleType>) segmentSource.asPrismObject());
            }
        }

        AssignmentPathSegmentImpl focusAssignmentSegment = assignmentPath.first();
        vars.setFocusAssignment(focusAssignmentSegment.getAssignmentIdi().clone());

        // a bit of hack -- TODO reconsider in 3.7
        // objects are already cloned
        convertToLegacy(vars.getMagicAssignment());
        convertToLegacy(vars.getThisAssignment());
        convertToLegacy(vars.getFocusAssignment());
        convertToLegacy(vars.getImmediateAssignment());

        return vars;
    }

    private static void convertToLegacy(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi) {
        if (idi == null || idi.getDelta() == null || idi.getSubItemDeltas() != null) {
            return;
        }
        // Legacy approach (when adding/removing assignments) was: itemOld+itemNew = value, delta = null
        // This was recently changed, to provide precise information (add = null->itemNew, delete = itemOld->null).
        // However, to not break scripts before 3.6 release we provide imitation of old behavior here.
        // (Moreover, for magic assignment the delta is not correct anyway.)
        if (idi.getDelta().isAdd() || idi.getDelta().isReplace()) {
            idi.setItemOld(idi.getItemNew().clone());
        } else {
            idi.setItemNew(idi.getItemOld().clone());
        }
        idi.setDelta(null);
    }

    private static void mergeExtension(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi, ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcIdi) throws SchemaException {
        mergeExtension(destIdi.getItemOld(), srcIdi.getItemOld());
        mergeExtension(destIdi.getItemNew(), srcIdi.getItemNew());
        if (srcIdi.getDelta() != null || srcIdi.getSubItemDeltas() != null) {
            throw new UnsupportedOperationException("Merge of IDI with deltas not supported");
        }
    }

    private static void mergeExtension(Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem,
            Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcItem) throws SchemaException {
        if (srcItem == null || dstItem == null) {
            return;
        }
        PrismContainer<Containerable> srcExtension = ((PrismContainer<AssignmentType>)srcItem).findContainer(AssignmentType.F_EXTENSION);
        mergeExtensionContainers(dstItem, srcExtension);
    }

    private static <O extends ObjectType> void mergeExtension(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi,
            PrismObject<O> srcObject) throws SchemaException {
        if (srcObject == null) {
            return;
        }

        PrismContainer<Containerable> srcExtension = srcObject.findContainer(ObjectType.F_EXTENSION);

        mergeExtensionContainers(destIdi.getItemNew(), srcExtension);
        mergeExtensionContainers(destIdi.getItemOld(), srcExtension);
    }

    private static void mergeExtensionContainers(Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem, PrismContainer<Containerable> srcExtension) throws SchemaException {
        if (dstItem == null) {
            return;
        }
        PrismContainer<AssignmentType> dstContainer = (PrismContainer<AssignmentType>) dstItem;
        if (srcExtension != null && !srcExtension.isEmpty()) {
            PrismContainer<?> dstExtensionContainer = dstContainer.findOrCreateContainer(AssignmentType.F_EXTENSION);
            PrismContainerValue<?> dstExtensionContainerValue = dstExtensionContainer.getValues().isEmpty()
                    ? dstExtensionContainer.createNewValue() : dstExtensionContainer.getValue();
            ObjectTypeUtil.mergeExtension(dstExtensionContainerValue, srcExtension.getValue());
        }
    }

    public static <V extends PrismValue,D extends ItemDefinition> MappingImpl.Builder<V,D> addAssignmentPathVariables(MappingImpl.Builder<V,D> builder, AssignmentPathVariables assignmentPathVariables, PrismContext prismContext) {
        ExpressionVariables expressionVariables = new ExpressionVariables();
        ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, expressionVariables, prismContext);
        return builder.addVariableDefinitions(expressionVariables);
    }

    public static <F extends ObjectType> void checkContextSanity(LensContext<F> context, String activityDescription,
            OperationResult result) throws SchemaException, PolicyViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            PrismObject<F> focusObjectNew = focusContext.getObjectNew();
            if (focusObjectNew != null) {
                PolyStringType namePolyType = focusObjectNew.asObjectable().getName();
                if (namePolyType == null) {
                    throw new SchemaException("Focus "+focusObjectNew+" does not have a name after "+activityDescription);
                }
                ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicyType();
                checkArchetypePolicy(focusContext, archetypePolicy);
            }
        }
    }

    private static <F extends ObjectType> void checkArchetypePolicy(LensFocusContext<F> focusContext, ArchetypePolicyType archetypePolicy) throws SchemaException, PolicyViolationException {
        if (archetypePolicy == null) {
            return;
        }
        PrismObject<F> focusObjectNew = focusContext.getObjectNew();
        ObjectDelta<F> focusDelta = focusContext.getDelta();

        for (ItemConstraintType itemConstraintType : archetypePolicy.getItemConstraint()) {
            processItemConstraint(focusContext, focusDelta, focusObjectNew, itemConstraintType);
        }
        // Deprecated
        for (ItemConstraintType itemConstraintType : archetypePolicy.getPropertyConstraint()) {
            processItemConstraint(focusContext, focusDelta, focusObjectNew, itemConstraintType);
        }

    }

    private static <F extends ObjectType> void processItemConstraint(LensFocusContext<F> focusContext, ObjectDelta<F> focusDelta, PrismObject<F> focusObjectNew, ItemConstraintType itemConstraintType) throws PolicyViolationException {
        ItemPath itemPath = itemConstraintType.getPath().getItemPath();
        if (BooleanUtils.isTrue(itemConstraintType.isOidBound())) {
            if (focusDelta != null) {
                if (focusDelta.isAdd()) {
                    PrismProperty<Object> propNew = focusObjectNew.findProperty(itemPath);
                    if (propNew != null) {
                        // prop delta is OK, but it has to match
                        if (focusObjectNew.getOid() != null) {
                            if (!focusObjectNew.getOid().equals(propNew.getRealValue().toString())) {
                                throw new PolicyViolationException("Cannot set "+itemPath+" to a value different than OID in oid bound mode");
                            }
                        }
                    }
                } else {
                    PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(itemPath);
                    if (nameDelta != null) {
                        if (nameDelta.isReplace()) {
                            Collection<PrismPropertyValue<Object>> valuesToReplace = nameDelta.getValuesToReplace();
                            if (valuesToReplace.size() == 1) {
                                String stringValue = valuesToReplace.iterator().next().getValue().toString();
                                if (focusContext.getOid().equals(stringValue)) {
                                    // This is OK. It is most likely a correction made by a recompute.
                                    return;
                                }
                            }
                        }
                        throw new PolicyViolationException("Cannot change "+itemPath+" in oid bound mode");
                    }
                }
            }
        }

    }

    public static PrismContainer<AssignmentType> createAssignmentSingleValueContainer(@NotNull AssignmentType assignmentType) throws SchemaException {
        // Make it appear to be single-value. Therefore paths without segment IDs will work.
        return assignmentType.asPrismContainerValue().asSingleValuedContainer(SchemaConstantsGenerated.C_ASSIGNMENT);
    }

    public static AssignmentType getAssignmentType(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi, boolean old) {
        return PrismContainerValue.asContainerable(assignmentIdi.getSingleValue(old));
    }


    public static <F extends ObjectType> String getChannel(LensContext<F> context, Task task) {
        if (context != null && context.getChannel() != null){
            return context.getChannel();
        } else if (task.getChannel() != null){
            return task.getChannel();
        }
        return null;
    }

    public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ItemDelta<?,?> itemDelta) {
        if (itemDelta.getEstimatedOldValues() != null) {
            return;
        }
        if (ctx.getObjectOld() == null) {
            return;
        }
        Item<PrismValue, ItemDefinition> itemOld = ctx.getObjectOld().findItem(itemDelta.getPath());
        if (itemOld != null) {
            //noinspection unchecked
            itemDelta.setEstimatedOldValues((Collection) PrismValueCollectionsUtil.cloneCollection(itemOld.getValues()));
            return;
        }
        // Here we need to distinguish whether the item is missing because it is not filled in (e.g. familyName in MID-4237)
        // or because it was not loaded (as for attributes or associations).
        if (!isItemLoadable(ctx.getObjectOld(), itemDelta.getPath())) {
            itemDelta.setEstimatedOldValues(emptySet());
            return;
        }
        // get the old data from current object. Still better estimate than nothing
        if (ctx.getObjectCurrent() != null) {
            itemOld = ctx.getObjectCurrent().findItem(itemDelta.getPath());
            if (itemOld != null) {
                //noinspection unchecked
                itemDelta.setEstimatedOldValues((Collection) PrismValueCollectionsUtil.cloneCollection(itemOld.getValues()));
            }
        }
    }

    // a heuristic by now
    private static <O extends ObjectType> boolean isItemLoadable(PrismObject<O> object, ItemPath path) {
        if (!(object.asObjectable() instanceof ShadowType)) {
            return false;
        }
        return path.startsWithName(ShadowType.F_ATTRIBUTES) || path.startsWithName(ShadowType.F_ASSOCIATION);
    }

    public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ObjectDelta<O> objectDelta) {
        if (objectDelta == null) {
            return;
        }
        if (!objectDelta.isModify()) {
            return;
        }
        for (ItemDelta<?, ?> modification: objectDelta.getModifications()) {
            setDeltaOldValue(ctx, modification);
        }
    }

    public static <F extends ObjectType> LensObjectDeltaOperation<F> createObjectDeltaOperation(ObjectDelta<F> focusDelta, OperationResult result,
                                                                                                LensElementContext<F> focusContext, LensProjectionContext projCtx) {
        return createObjectDeltaOperation(focusDelta, result, focusContext, projCtx, null);
    }

    // projCtx may or may not be present (object itself can be focus or projection)
    public static <T extends ObjectType> LensObjectDeltaOperation<T> createObjectDeltaOperation(ObjectDelta<T> objectDelta, OperationResult result,
            LensElementContext<T> objectContext,
            LensProjectionContext projCtx,
            ResourceType resource) {
        LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<>(objectDelta.clone());
        objectDeltaOp.setExecutionResult(result);
        PrismObject<T> object = objectContext.getObjectAny();
        if (object != null) {
            PolyString name = object.getName();
            if (name == null && object.asObjectable() instanceof ShadowType) {
                try {
                    name = ShadowUtil.determineShadowName((PrismObject<ShadowType>) object);
                    if (name == null) {
                        LOGGER.debug("No name for shadow:\n{}", object.debugDump());
                    } else if (name.getNorm() == null) {
                        name.recompute(objectContext.getPrismContext().getDefaultPolyStringNormalizer());
                    }
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine name for shadow -- continuing with no name; shadow:\n{}", e, object.debugDump());
                }
            }
            objectDeltaOp.setObjectName(name);
        }
        if (resource == null && projCtx != null) {
            resource = projCtx.getResource();
        }
        if (resource != null) {
            objectDeltaOp.setResourceOid(resource.getOid());
            objectDeltaOp.setResourceName(PolyString.toPolyString(resource.getName()));
        } else if (objectContext instanceof LensProjectionContext) {
            objectDeltaOp.setResourceOid(((LensProjectionContext) objectContext).getResourceOid());
        }
        return objectDeltaOp;
    }

    public static void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers,
            Collection<String> policySituations) {

        LOGGER.debug("Policy rule {} triggered: {}", rule.getName(), triggers);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Policy rule {} triggered:\n{}", rule.getName(), DebugUtil.debugDump(triggers, 1));
        }

        ((EvaluatedPolicyRuleImpl) rule).addTriggers(triggers);
        CollectionUtils.addIgnoreNull(policySituations, rule.getPolicySituation());
    }

    public static void processRuleWithException(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger <?>> triggers,
             PolicyExceptionType policyException) {

        LOGGER.debug("Policy rule {} would be triggered, but there is an exception for it. Not triggering", rule.getName());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Policy rule {} would be triggered, but there is an exception for it:\nTriggers:\n{}\nException:\n{}",
                    rule.getName(), DebugUtil.debugDump(triggers, 1), policyException);
        }
        ((EvaluatedPolicyRuleImpl)rule).addPolicyException(policyException);
    }


    public static void checkMaxIterations(int iteration, int maxIterations, String conflictMessage, String humanReadableName)
            throws ObjectAlreadyExistsException {
        if (iteration > maxIterations) {
            StringBuilder sb = new StringBuilder();
            if (iteration == 1) {
                sb.append("Error processing ");
            } else {
                sb.append("Too many iterations (").append(iteration).append(") for ");
            }
            sb.append(humanReadableName);
            if (iteration == 1) {
                sb.append(": constraint violation: ");
            } else {
                sb.append(": cannot determine values that satisfy constraints: ");
            }
            if (conflictMessage != null) {
                sb.append(conflictMessage);
            }
            throw new ObjectAlreadyExistsException(sb.toString());
        }
    }

    public static boolean needsFullShadowForCredentialProcessing(LensProjectionContext projCtx) throws SchemaException {
        RefinedObjectClassDefinition refinedProjDef = projCtx.getStructuralObjectClassDefinition();
        if (refinedProjDef == null) {
            return false;
        }

        List<MappingType> outboundMappingType = refinedProjDef.getPasswordOutbound();
        if (outboundMappingType == null) {
            return false;
        }
        for (MappingType mappingType: outboundMappingType) {
            if (mappingType.getStrength() == MappingStrengthType.STRONG || mappingType.getStrength() == MappingStrengthType.WEAK) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPasswordReturnedByDefault(LensProjectionContext projCtx) {
        CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(projCtx.getResource(), CredentialsCapabilityType.class);
        return CapabilityUtil.isPasswordReturnedByDefault(credentialsCapabilityType);
    }

    public static boolean evaluateBoolean(ExpressionType expressionBean, ExpressionVariables expressionVariables,
            String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
                task, result,
                DOMUtil.XSD_BOOLEAN, false, null);
    }

    public static String evaluateString(ExpressionType expressionBean, ExpressionVariables expressionVariables,
            String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
                task, result,
                DOMUtil.XSD_STRING, null, null);
    }

    public static LocalizableMessageType evaluateLocalizableMessageType(ExpressionType expressionBean, ExpressionVariables expressionVariables,
            String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Function<Object, Object> additionalConvertor = (o) -> {
            if (o == null || o instanceof LocalizableMessageType) {
                return o;
            } else if (o instanceof LocalizableMessage) {
                return LocalizationUtil.createLocalizableMessageType((LocalizableMessage) o);
            } else {
                return new SingleLocalizableMessageType().fallbackMessage(String.valueOf(o));
            }
        };
        return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
                task, result, LocalizableMessageType.COMPLEX_TYPE, null, additionalConvertor);
    }

    public static <T> T evaluateExpressionSingle(ExpressionType expressionBean, ExpressionVariables expressionVariables,
            String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
            OperationResult result, QName typeName,
            T defaultValue, Function<Object, Object> additionalConvertor)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismPropertyDefinition<T> resultDef = prismContext.definitionFactory().createPropertyDefinition(
                new QName(SchemaConstants.NS_C, "result"), typeName);
        Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression =
                expressionFactory.makeExpression(expressionBean, resultDef, MiscSchemaUtil.getExpressionProfile(), contextDescription, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, expressionVariables, contextDescription, task);
        eeContext.setAdditionalConvertor(additionalConvertor);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple = ModelExpressionThreadLocalHolder
                .evaluateExpressionInContext(expression, eeContext, task, result);
        List<T> results = exprResultTriple.getZeroSet().stream()
                .map(ppv -> (T) ppv.getRealValue())
                .collect(Collectors.toList());
        return getSingleValue(results, defaultValue, contextDescription);
    }

    @NotNull
    public static SingleLocalizableMessageType interpretLocalizableMessageTemplate(LocalizableMessageTemplateType template,
            ExpressionVariables var, ExpressionFactory expressionFactory, PrismContext prismContext,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        SingleLocalizableMessageType rv = new SingleLocalizableMessageType();
        if (template.getKey() != null) {
            rv.setKey(template.getKey());
        } else if (template.getKeyExpression() != null) {
            rv.setKey(evaluateString(template.getKeyExpression(), var, "localizable message key expression", expressionFactory, prismContext, task, result));
        }
        if (!template.getArgument().isEmpty() && !template.getArgumentExpression().isEmpty()) {
            throw new IllegalArgumentException("Both argument and argumentExpression items are non empty");
        } else if (!template.getArgumentExpression().isEmpty()) {
            for (ExpressionType argumentExpression : template.getArgumentExpression()) {
                LocalizableMessageType argument = evaluateLocalizableMessageType(argumentExpression, var,
                        "localizable message argument expression", expressionFactory, prismContext, task, result);
                rv.getArgument().add(new LocalizableMessageArgumentType().localizable(argument));
            }
        } else {
            // TODO allow localizable messages templates here
            rv.getArgument().addAll(template.getArgument());
        }
        if (template.getFallbackMessage() != null) {
            rv.setFallbackMessage(template.getFallbackMessage());
        } else if (template.getFallbackMessageExpression() != null) {
            rv.setFallbackMessage(evaluateString(template.getFallbackMessageExpression(), var,
                    "localizable message fallback expression", expressionFactory, prismContext, task, result));
        }
        return rv;
    }

    public static <F extends ObjectType> void reclaimSequences(LensContext<F> context, RepositoryService repositoryService, Task task, OperationResult result) throws SchemaException {
        if (context == null) {
            return;
        }

        Map<String, Long> sequenceMap = context.getSequences();
        LOGGER.trace("Context sequence map: {}", sequenceMap);
        for (Map.Entry<String, Long> sequenceMapEntry: sequenceMap.entrySet()) {
            Collection<Long> unusedValues = new ArrayList<>(1);
            unusedValues.add(sequenceMapEntry.getValue());
            try {
                LOGGER.trace("Returning value {} to sequence {}", sequenceMapEntry.getValue(), sequenceMapEntry.getKey());
                repositoryService.returnUnusedValuesToSequence(sequenceMapEntry.getKey(), unusedValues, result);
            } catch (ObjectNotFoundException e) {
                LOGGER.error("Cannot return unused value to sequence {}: it does not exist", sequenceMapEntry.getKey(), e);
                // ... but otherwise ignore it and go on
            }
        }
    }

    public static <AH extends AssignmentHolderType> void applyObjectPolicyConstraints(LensFocusContext<AH> focusContext, ArchetypePolicyType archetypePolicy, PrismContext prismContext) throws SchemaException, ConfigurationException {
        if (archetypePolicy == null) {
            return;
        }

        final PrismObject<AH> focusNew = focusContext.getObjectNew();
        if (focusNew == null) {
            // This is delete. Nothing to do.
            return;
        }

        for (ItemConstraintType itemConstraintType : archetypePolicy.getItemConstraint()) {
            applyObjectPolicyItemConstraint(focusContext, archetypePolicy, prismContext, focusNew, itemConstraintType);
        }
        // Deprecated
        for (ItemConstraintType itemConstraintType : archetypePolicy.getPropertyConstraint()) {
            applyObjectPolicyItemConstraint(focusContext, archetypePolicy, prismContext, focusNew, itemConstraintType);
        }
    }

    private static <AH extends AssignmentHolderType> void applyObjectPolicyItemConstraint(LensFocusContext<AH> focusContext, ArchetypePolicyType archetypePolicy, PrismContext prismContext, PrismObject<AH> focusNew, ItemConstraintType itemConstraintType) throws SchemaException, ConfigurationException {
        if (itemConstraintType.getPath() == null) {
            LOGGER.error("Invalid configuration. Path is mandatory for property constraint definition in {} defined in system configuration", archetypePolicy);
            throw new SchemaException("Invalid configuration. Path is mandatory for property constraint definition in " + archetypePolicy + " defined in system configuration.");
        }
        ItemPath itemPath = itemConstraintType.getPath().getItemPath();
        if (BooleanUtils.isTrue(itemConstraintType.isOidBound())) {
            PrismProperty<Object> prop = focusNew.findProperty(itemPath);
            if (prop == null || prop.isEmpty()) {
                String newValue = focusNew.getOid();
                if (newValue == null) {
                    newValue = OidUtil.generateOid();
                }
                LOGGER.trace("Generating new OID-bound value for {}: {}", itemPath, newValue);
                PrismObjectDefinition<AH> focusDefinition = focusContext.getObjectDefinition();
                PrismPropertyDefinition<Object> propDef = focusDefinition.findPropertyDefinition(itemPath);
                if (propDef == null) {
                    throw new SchemaException("No definition for property "+itemPath+" in "+focusDefinition+" as specified in object policy");
                }
                PropertyDelta<Object> propDelta = propDef.createEmptyDelta(itemPath);
                if (String.class.isAssignableFrom(propDef.getTypeClass())) {
                    propDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(newValue, OriginType.USER_POLICY, null));
                } else if (PolyString.class.isAssignableFrom(propDef.getTypeClass())) {
                    propDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(new PolyString(newValue), OriginType.USER_POLICY, null));
                } else {
                    throw new SchemaException("Unsupported type "+propDef.getTypeName()+" for property "+itemPath+" in "+focusDefinition+" as specified in object policy, only string and polystring properties are supported for OID-bound mode");
                }
                focusContext.swallowToSecondaryDelta(propDelta);
                focusContext.recompute();
            }
        }
    }

    public static LensContext.ExportType getExportType(TraceType trace, OperationResult result) {
        return result.isTracingNormal(trace.getClass()) ? LensContext.ExportType.TRACE : LensContext.ExportType.MINIMAL;
    }

    public static LensContext.ExportType getExportTypeTraceOrReduced(TraceType trace, OperationResult result) {
        return result.isTracingNormal(trace.getClass()) ? LensContext.ExportType.TRACE : LensContext.ExportType.REDUCED;
    }

    public static <AH extends AssignmentHolderType> ItemDelta getAprioriItemDelta(ObjectDelta<AH> focusDelta, ItemPath itemPath) {
        return focusDelta != null ? focusDelta.findItemDelta(itemPath) : null;
    }

    public static <O extends ObjectType> String determineExplicitArchetypeOid(PrismObject<O> object) {
        String explicitArchetypeOid = null;
        // Used in cases where archetype assignment haven't had the change to be processed yet.
        // E.g. in case that we are creating a new object with archetype assignment
        if (object.canRepresent(AssignmentHolderType.class)) {
            AssignmentHolderType assignmentHolderType = (AssignmentHolderType)object.asObjectable();
            List<ObjectReferenceType> archetypeRefs = assignmentHolderType.getArchetypeRef();
            if (archetypeRefs.isEmpty()) {
                explicitArchetypeOid = determineExplicitArchetypeOidFromAssignments(object);
            }
        }
        return explicitArchetypeOid;
    }

    public static <O extends ObjectType> String determineExplicitArchetypeOidFromAssignments(PrismObject<O> object) {
        String explicitArchetypeOid = null;
        if (object.canRepresent(AssignmentHolderType.class)) {
            for (AssignmentType assignment : ((AssignmentHolderType)object.asObjectable()).getAssignment()) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef != null && QNameUtil.match(ArchetypeType.COMPLEX_TYPE, targetRef.getType())) {
                    explicitArchetypeOid = targetRef.getOid();
                }
            }
        }
        return explicitArchetypeOid;
    }
}
