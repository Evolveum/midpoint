/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import java.util.*;
import java.util.Map.Entry;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@Component
public class MappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluator.class);

    @Autowired private MappingFactory mappingFactory;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private ContextLoader contextLoader;
    @Autowired private PrismContext prismContext;
    @Autowired private ObjectResolver objectResolver;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    static final List<String> FOCUS_VARIABLE_NAMES = Arrays.asList(ExpressionConstants.VAR_FOCUS, ExpressionConstants.VAR_USER);

    public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(MappingImpl<V, D> mapping,
            LensContext<F> lensContext, Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        evaluateMapping(mapping, lensContext, null, task, parentResult);
    }

    public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(MappingImpl<V, D> mapping,
            LensContext<F> lensContext, LensProjectionContext projContext, Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        ExpressionEnvironment<F, V, D> env = new ExpressionEnvironment<>();
        env.setLensContext(lensContext);
        env.setProjectionContext(projContext);
        env.setMapping(mapping);
        env.setCurrentResult(parentResult);
        env.setCurrentTask(task);
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);

        ObjectType originObject = mapping.getOriginObject();
        String objectOid, objectName, objectTypeName;
        if (originObject != null) {
            objectOid = originObject.getOid();
            objectName = String.valueOf(originObject.getName());
            objectTypeName = originObject.getClass().getSimpleName();
        } else {
            objectOid = objectName = objectTypeName = null;
        }
        String mappingName = mapping.getItemName() != null ? mapping.getItemName().getLocalPart() : null;

        long start = System.currentTimeMillis();
        try {
            task.recordState("Started evaluation of mapping " + mapping.getMappingContextDescription() + ".");
            mapping.evaluate(task, parentResult);
            task.recordState("Successfully finished evaluation of mapping " + mapping.getMappingContextDescription() + " in " + (System.currentTimeMillis() - start) + " ms.");
        } catch (IllegalArgumentException e) {
            task.recordState("Evaluation of mapping " + mapping.getMappingContextDescription() + " finished with error in " + (System.currentTimeMillis() - start) + " ms.");
            throw new IllegalArgumentException(e.getMessage() + " in " + mapping.getContextDescription(), e);
        } finally {
            task.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, System.currentTimeMillis() - start);
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
            if (lensContext.getInspector() != null) {
                lensContext.getInspector().afterMappingEvaluation(lensContext, mapping);
            }
        }
    }

    // TODO: unify OutboundProcessor.evaluateMapping() with MappingEvaluator.evaluateOutboundMapping(...)
    public <T, F extends FocusType> void evaluateOutboundMapping(final LensContext<F> context,
            final LensProjectionContext projCtx, List<MappingType> outboundMappings,
            final ItemPath projectionPropertyPath, final MappingInitializer<PrismPropertyValue<T>,
            PrismPropertyDefinition<T>> initializer, MappingOutputProcessor<PrismPropertyValue<T>> processor,
            XMLGregorianCalendar now, final MappingTimeEval evaluateCurrent, boolean evaluateWeak,
            String desc, final Task task, final OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        String projCtxDesc = projCtx.toHumanReadableString();
        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> internalInitializer =
                builder -> {

                    builder.addVariableDefinitions(ModelImplUtils.getDefaultExpressionVariables(context, projCtx));

                    builder.mappingKind(MappingKindType.OUTBOUND);
                    builder.originType(OriginType.OUTBOUND);
                    builder.implicitTargetPath(projectionPropertyPath);
                    builder.originObject(projCtx.getResource());

                    initializer.initialize(builder);

                    return builder;
                };

        MappingEvaluatorParams<PrismPropertyValue<T>, PrismPropertyDefinition<T>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(outboundMappings);
        params.setMappingDesc(desc + " in projection " + projCtxDesc);
        params.setNow(now);
        params.setInitializer(internalInitializer);
        params.setProcessor(processor);
        params.setTargetLoader(new ProjectionMappingLoader<>(context, projCtx, contextLoader));
        params.setAPrioriTargetObject(shadowNew);
        params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
        params.setTargetContext(projCtx);
        params.setDefaultTargetItemPath(projectionPropertyPath);
        if (context.getFocusContext() != null) {
            params.setSourceContext(context.getFocusContext().getObjectDeltaObject());
        }
        params.setEvaluateCurrent(evaluateCurrent);
        params.setEvaluateWeak(evaluateWeak);
        params.setContext(context);
        params.setHasFullTargetObject(projCtx.hasFullShadow());
        evaluateMappingSetProjection(params, task, result);
    }

    public <V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> Map<UniformItemPath, MappingOutputStruct<V>> evaluateMappingSetProjection(
            MappingEvaluatorParams<V, D, T, F> params, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        String mappingDesc = params.getMappingDesc();
        LensElementContext<T> targetContext = params.getTargetContext();
        PrismObjectDefinition<T> targetObjectDefinition = targetContext.getObjectDefinition();
        ItemPath defaultTargetItemPath = params.getDefaultTargetItemPath();

        Map<UniformItemPath, MappingOutputStruct<V>> outputTripleMap = new HashMap<>();
        XMLGregorianCalendar nextRecomputeTime = null;
        String triggerOriginDescription = null;
        Collection<MappingType> mappingTypes = params.getMappingTypes();
        Collection<MappingImpl<V, D>> mappings = new ArrayList<>(mappingTypes.size());

        for (MappingType mappingType : mappingTypes) {

            MappingImpl.Builder<V, D> mappingBuilder = mappingFactory.createMappingBuilder(mappingType, mappingDesc);
            String mappingName = null;
            if (mappingType.getName() != null) {
                mappingName = mappingType.getName();
            }

            if (!mappingBuilder.isApplicableToChannel(params.getContext().getChannel())) {
                LOGGER.trace("Mapping {} not applicable to channel, skipping {}", mappingName, params.getContext().getChannel());
                continue;
            }

            mappingBuilder.now(params.getNow());
            if (defaultTargetItemPath != null && targetObjectDefinition != null) {
                D defaultTargetItemDef = targetObjectDefinition.findItemDefinition(defaultTargetItemPath);
                mappingBuilder.defaultTargetDefinition(defaultTargetItemDef);
            } else {
                mappingBuilder.defaultTargetDefinition(params.getTargetItemDefinition());
            }
            mappingBuilder.defaultTargetPath(defaultTargetItemPath);
            mappingBuilder.targetContext(targetObjectDefinition);

            if (params.getSourceContext() != null) {
                mappingBuilder.sourceContext(params.getSourceContext());
            }

            // Initialize mapping (using Inversion of Control)
            MappingImpl.Builder<V, D> initializedMappingBuilder = params.getInitializer().initialize(mappingBuilder);

            MappingImpl<V, D> mapping = initializedMappingBuilder.build();
            boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(task, result);

            if (params.getEvaluateCurrent() == MappingTimeEval.CURRENT && !timeConstraintValid) {
                LOGGER.trace("Mapping {} is non-current, but evaluating current mappings, skipping {}", mappingName, params.getContext().getChannel());
            } else if (params.getEvaluateCurrent() == MappingTimeEval.FUTURE && timeConstraintValid) {
                LOGGER.trace("Mapping {} is current, but evaluating non-current mappings, skipping {}", mappingName, params.getContext().getChannel());
            } else {
                mappings.add(mapping);
            }
        }

        boolean hasFullTargetObject = params.hasFullTargetObject();
        PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();

        LOGGER.trace("Going to process {} mappings for {}", mappings.size(), mappingDesc);

        for (MappingImpl<V, D> mapping : mappings) {

            if (mapping.getStrength() == MappingStrengthType.WEAK) {
                // Evaluate weak mappings in a second run.
                continue;
            }

            UniformItemPath mappingOutputPathUniform = prismContext.toUniformPathKeepNull(mapping.getOutputPath());
            if (params.isFixTarget() && mappingOutputPathUniform != null && defaultTargetItemPath != null && !mappingOutputPathUniform.equivalent(defaultTargetItemPath)) {
                throw new ExpressionEvaluationException("Target cannot be overridden in " + mappingDesc);
            }

            if (params.getAPrioriTargetDelta() != null && mappingOutputPathUniform != null) {
                ItemDelta<?, ?> aPrioriItemDelta = params.getAPrioriTargetDelta().findItemDelta(mappingOutputPathUniform);
                if (mapping.getStrength() != MappingStrengthType.STRONG) {
                    if (aPrioriItemDelta != null && !aPrioriItemDelta.isEmpty()) {
                        continue;
                    }
                }
            }

            evaluateMapping(mapping, params.getContext(), task, result);

            PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Output triple of mapping {}\n{}", mapping.getContextDescription(),
                        mappingOutputTriple == null ? null : mappingOutputTriple.debugDump(1));
            }

            if (isMeaningful(mappingOutputTriple)) {

                MappingOutputStruct<V> mappingOutputStruct = outputTripleMap.get(mappingOutputPathUniform);
                if (mappingOutputStruct == null) {
                    mappingOutputStruct = new MappingOutputStruct<>();
                    outputTripleMap.put(mappingOutputPathUniform, mappingOutputStruct);
                }

                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    mappingOutputStruct.setStrongMappingWasUsed(true);

                    if (!hasFullTargetObject && params.getTargetLoader() != null && aPrioriTargetObject != null && aPrioriTargetObject.getOid() != null) {
                        if (!params.getTargetLoader().isLoaded()) {
                            aPrioriTargetObject = params.getTargetLoader().load("strong mapping", task, result);
                            LOGGER.trace("Loaded object because of strong mapping: {}", aPrioriTargetObject);
                            hasFullTargetObject = true;
                        }
                    }
                }

                PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
                if (outputTriple == null) {
                    mappingOutputStruct.setOutputTriple(mappingOutputTriple);
                } else {
                    outputTriple.merge(mappingOutputTriple);
                }

            } else {
                LOGGER.trace("Output triple of mapping {} is NOT meaningful", mapping.getContextDescription());
            }

        }

        if (params.isEvaluateWeak()) {
            // Second pass, evaluate only weak mappings
            for (MappingImpl<V, D> mapping : mappings) {

                if (mapping.getStrength() != MappingStrengthType.WEAK) {
                    continue;
                }

                UniformItemPath mappingOutputPath = prismContext.toUniformPathKeepNull(mapping.getOutputPath());
                if (params.isFixTarget() && mappingOutputPath != null && defaultTargetItemPath != null && !mappingOutputPath.equivalent(defaultTargetItemPath)) {
                    throw new ExpressionEvaluationException("Target cannot be overridden in " + mappingDesc);
                }

                MappingOutputStruct<V> mappingOutputStruct = outputTripleMap.get(mappingOutputPath);
                if (mappingOutputStruct == null) {
                    mappingOutputStruct = new MappingOutputStruct<>();
                    outputTripleMap.put(mappingOutputPath, mappingOutputStruct);
                }

                PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
                if (outputTriple != null && !outputTriple.getNonNegativeValues().isEmpty()) {
                    // Previous mapping produced zero/positive output. We do not need to evaluate weak mapping.
                    //
                    // Note: this might or might not be correct. The idea is that if previous mapping produced no positive values
                    //  (e.g. because of condition switched from true to false) we might apply the weak mapping.
                    //
                    // TODO (original) this is not entirely correct. Previous mapping might have deleted all
                    //  values. Also we may need the output of the weak mapping to correctly process
                    //  non-tolerant values (to avoid removing the value that weak mapping produces).
                    //  MID-3847
                    continue;
                }

                Item<V, D> aPrioriTargetItem = null;
                if (aPrioriTargetObject != null && mappingOutputPath != null) {
                    aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                }
                if (hasNoValue(aPrioriTargetItem)) {

                    mappingOutputStruct.setWeakMappingWasUsed(true);

                    evaluateMapping(mapping, params.getContext(), task, result);

                    PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
                    if (mappingOutputTriple != null) {

                        // This may be counter-intuitive to load object after the mapping is executed
                        // But the mapping may not be activated (e.g. condition is false). And in that
                        // case we really do not want to trigger object loading.
                        // This is all not right. See MID-3847
                        if (!hasFullTargetObject && params.getTargetLoader() != null && aPrioriTargetObject != null && aPrioriTargetObject.getOid() != null) {
                            if (!params.getTargetLoader().isLoaded()) {
                                aPrioriTargetObject = params.getTargetLoader().load("weak mapping", task, result);
                                LOGGER.trace("Loaded object because of weak mapping: {}", aPrioriTargetObject);
                                hasFullTargetObject = true;
                            }
                        }
                        if (aPrioriTargetObject != null && mappingOutputPath != null) {
                            aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                        }
                        if (!hasNoValue(aPrioriTargetItem)) {
                            continue;
                        }

                        //noinspection ConstantConditions
                        if (outputTriple == null) {     // this is currently always true (see above)
                            mappingOutputStruct.setOutputTriple(mappingOutputTriple);
                        } else {
                            outputTriple.merge(mappingOutputTriple);
                        }
                    }

                }
            }
        }

        MappingOutputProcessor<V> processor = params.getProcessor();
        for (Entry<UniformItemPath, MappingOutputStruct<V>> outputTripleMapEntry : outputTripleMap.entrySet()) {
            UniformItemPath mappingOutputPath = outputTripleMapEntry.getKey();
            MappingOutputStruct<V> mappingOutputStruct = outputTripleMapEntry.getValue();
            PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();

            boolean defaultProcessing;
            if (processor != null) {
                LOGGER.trace("Executing processor to process mapping evaluation results: {}", processor);
                defaultProcessing = processor.process(mappingOutputPath, mappingOutputStruct);
            } else {
                defaultProcessing = true;
            }

            if (defaultProcessing) {

                if (outputTriple == null) {
                    LOGGER.trace("{} expression resulted in null triple for {}, skipping", mappingDesc, targetContext);
                    continue;
                }

                ItemDefinition targetItemDefinition;
                if (mappingOutputPath != null) {
                    targetItemDefinition = targetObjectDefinition.findItemDefinition(mappingOutputPath);
                    if (targetItemDefinition == null) {
                        throw new SchemaException("No definition for item " + mappingOutputPath + " in " + targetObjectDefinition);
                    }
                } else {
                    targetItemDefinition = params.getTargetItemDefinition();
                }
                //noinspection unchecked
                ItemDelta<V, D> targetItemDelta = targetItemDefinition.createEmptyDelta(mappingOutputPath);

                Item<V, D> aPrioriTargetItem;
                if (aPrioriTargetObject != null) {
                    aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                } else {
                    aPrioriTargetItem = null;
                }

                // WARNING
                // Following code seems to be wrong. It is not very relativistic. It seems to always
                // go for replace.
                // It seems that it is only used for activation mappings (outbound and inbound). As
                // these are quite special single-value properties then it seems to work fine
                // (with the exception of MID-3418). Todo: make it more relativistic: MID-3419

                if (targetContext.isAdd()) {

                    Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
                    if (nonNegativeValues.isEmpty()) {
                        LOGGER.trace("{} resulted in null or empty value for {}, skipping", mappingDesc, targetContext);
                        continue;
                    }
                    targetItemDelta.setValuesToReplace(PrismValueCollectionsUtil.cloneCollection(nonNegativeValues));

                } else {

                    // if we have fresh information (full shadow) AND the mapping used to derive the information was strong,
                    // we will consider all values (zero & plus sets) -- otherwise, we take only the "plus" (i.e. changed) set

                    // the first case is necessary, because in some situations (e.g. when mapping is changed)
                    // the evaluator sees no differences w.r.t. real state, even if there is a difference
                    // - and we must have a way to push new information onto the resource

                    Collection<V> valuesToReplace;

                    if (hasFullTargetObject && mappingOutputStruct.isStrongMappingWasUsed()) {
                        valuesToReplace = outputTriple.getNonNegativeValues();
                    } else {
                        valuesToReplace = outputTriple.getPlusSet();
                    }

                    LOGGER.trace("{}: hasFullTargetObject={}, isStrongMappingWasUsed={}, valuesToReplace={}",
                            mappingDesc, hasFullTargetObject, mappingOutputStruct.isStrongMappingWasUsed(), valuesToReplace);

                    if (!valuesToReplace.isEmpty()) {

                        // if what we want to set is the same as is already in the shadow, we skip that
                        // (we insist on having full shadow, to be sure we work with current data)

                        if (hasFullTargetObject && targetContext.isFresh() && aPrioriTargetItem != null) {
                            Collection<V> valuesPresent = aPrioriTargetItem.getValues();
                            if (PrismValueCollectionsUtil.equalsRealValues(valuesPresent, valuesToReplace)) {
                                LOGGER.trace("{} resulted in existing values for {}, skipping creation of a delta", mappingDesc, targetContext);
                                continue;
                            }
                        }
                        targetItemDelta.setValuesToReplace(PrismValueCollectionsUtil.cloneCollection(valuesToReplace));

                        applyEstematedOldValueInReplaceCase(targetItemDelta, outputTriple);

                    } else if (outputTriple.hasMinusSet()) {
                        LOGGER.trace("{} resulted in null or empty value for {} and there is a minus set, resetting it (replace with empty)", mappingDesc, targetContext);
                        targetItemDelta.setValueToReplace();
                        applyEstematedOldValueInReplaceCase(targetItemDelta, outputTriple);

                    } else {
                        LOGGER.trace("{} resulted in null or empty value for {}, skipping", mappingDesc, targetContext);
                    }

                }

                if (targetItemDelta.isEmpty()) {
                    continue;
                }

                LOGGER.trace("{} adding new delta for {}: {}", mappingDesc, targetContext, targetItemDelta);
                targetContext.swallowToSecondaryDelta(targetItemDelta);
            }

        }

        // Figure out recompute time

        for (MappingImpl<V, D> mapping : mappings) {
            XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
            if (mappingNextRecomputeTime != null) {
                if (mapping.isSatisfyCondition() && (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER)) {
                    nextRecomputeTime = mappingNextRecomputeTime;
                    // TODO: maybe better description? But consider storage requirements. We do not want to store too much.
                    triggerOriginDescription = mapping.getIdentifier();
                }
            }
        }

        if (nextRecomputeTime != null) {
            NextRecompute nextRecompute = new NextRecompute(nextRecomputeTime, triggerOriginDescription);
            nextRecompute.createTrigger(params.getAPrioriTargetObject(), targetObjectDefinition, targetContext);
        }

        return outputTripleMap;
    }

    private <V extends PrismValue, D extends ItemDefinition> void applyEstematedOldValueInReplaceCase(ItemDelta<V, D> targetItemDelta,
            PrismValueDeltaSetTriple<V> outputTriple) {
        Collection<V> nonPositiveValues = outputTriple.getNonPositiveValues();
        if (nonPositiveValues.isEmpty()) {
            return;
        }
        targetItemDelta.setEstimatedOldValues(PrismValueCollectionsUtil.cloneCollection(nonPositiveValues));
    }

    private <V extends PrismValue> boolean isMeaningful(PrismValueDeltaSetTriple<V> mappingOutputTriple) {
        if (mappingOutputTriple == null) {
            // this means: mapping not applicable
            return false;
        }
        if (mappingOutputTriple.isEmpty()) {
            // this means: no value produced
            return true;
        }
        if (mappingOutputTriple.getZeroSet().isEmpty() && mappingOutputTriple.getPlusSet().isEmpty()) {
            // Minus deltas are always meaningful, even with hashing (see below)
            // This may be used e.g. to remove existing password.
            return true;
        }
        if (hasNoOrHashedValuesOnly(mappingOutputTriple.getMinusSet()) && hasNoOrHashedValuesOnly(mappingOutputTriple.getZeroSet()) && hasNoOrHashedValuesOnly(mappingOutputTriple.getPlusSet())) {
            // Used to skip application of mapping that produces only hashed protected values.
            // Those values are useless, e.g. to set new password. If we would consider them as
            // meaningful then a normal mapping with such values may prohibit application of
            // a weak mapping. We want weak mapping in this case, e.g. to set a randomly-generated password.
            // Not entirely correct. Maybe we need to filter this out in some other way?
            return false;
        }
        return true;
    }

    // Not entirely correct. Maybe we need to filter this out in some other way?
    private <V extends PrismValue> boolean hasNoOrHashedValuesOnly(Collection<V> set) {
        if (set == null) {
            return true;
        }
        for (V pval : set) {
            Object val = pval.getRealValue();
            if (val instanceof ProtectedStringType) {
                if (!((ProtectedStringType) val).isHashed()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoValue(Item aPrioriTargetItem) {
        return aPrioriTargetItem == null
                || (aPrioriTargetItem.isEmpty() && !aPrioriTargetItem.isIncomplete());
    }

    <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType, T extends AssignmentHolderType> MappingImpl<V, D> createFocusMapping(
            final MappingFactory mappingFactory, final LensContext<AH> context, final MappingType mappingType, MappingKindType mappingKind, ObjectType originObject,
            ObjectDeltaObject<AH> focusOdo, Source<V, D> defaultSource, PrismObject<T> defaultTargetObject, AssignmentPathVariables assignmentPathVariables,
            Integer iteration, String iterationToken, PrismObject<SystemConfigurationType> configuration,
            XMLGregorianCalendar now, String contextDesc, final Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (!MappingImpl.isApplicableToChannel(mappingType, context.getChannel())) {
            LOGGER.trace("Mapping {} not applicable to channel {}, skipping.", mappingType, context.getChannel());
            return null;
        }

        ValuePolicyResolver stringPolicyResolver = new ValuePolicyResolver() {
            private ItemPath outputPath;
            private ItemDefinition outputDefinition;

            @Override
            public void setOutputPath(ItemPath outputPath) {
                this.outputPath = outputPath;
            }

            @Override
            public void setOutputDefinition(ItemDefinition outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType resolve() {
                // TODO need to switch to ObjectValuePolicyEvaluator
                if (outputDefinition.getItemName().equals(PasswordType.F_VALUE)) {
                    return credentialsProcessor.determinePasswordPolicy(context.getFocusContext());
                }
                if (mappingType.getExpression() != null) {
                    List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
                    if (evaluators != null) {
                        for (JAXBElement jaxbEvaluator : evaluators) {
                            Object object = jaxbEvaluator.getValue();
                            if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
                                ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
                                try {
                                    ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class,
                                            null, "resolving value policy for generate attribute " + outputDefinition.getItemName() + " value", task, new OperationResult("Resolving value policy"));
                                    if (valuePolicyType != null) {
                                        return valuePolicyType;
                                    }
                                } catch (CommonException ex) {
                                    throw new SystemException(ex.getMessage(), ex);
                                }
                            }
                        }

                    }
                }
                return null;

            }
        };

        ExpressionVariables variables = new ExpressionVariables();
        FOCUS_VARIABLE_NAMES.forEach(name -> variables.addVariableDefinition(name, focusOdo, focusOdo.getDefinition()));
        variables.put(ExpressionConstants.VAR_ITERATION, iteration, Integer.class);
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken, String.class);
        variables.put(ExpressionConstants.VAR_CONFIGURATION, configuration, SystemConfigurationType.class);
        variables.put(ExpressionConstants.VAR_OPERATION, context.getFocusContext().getOperation().getValue(), String.class);
        variables.put(ExpressionConstants.VAR_SOURCE, originObject, ObjectType.class);

        TypedValue<PrismObject<T>> defaultTargetContext = new TypedValue<>(defaultTargetObject);
        Collection<V> targetValues = ExpressionUtil.computeTargetValues(mappingType.getTarget(), defaultTargetContext, variables, mappingFactory.getObjectResolver(), contextDesc, prismContext, task, result);

        MappingImpl.Builder<V, D> mappingBuilder = mappingFactory.<V, D>createMappingBuilder(mappingType, contextDesc)
                .sourceContext(focusOdo)
                .defaultSource(defaultSource)
                .targetContext(defaultTargetObject.getDefinition())
                .variables(variables)
                .originalTargetValues(targetValues)
                .mappingKind(mappingKind)
                .originType(OriginType.USER_POLICY)
                .originObject(originObject)
                .objectResolver(objectResolver)
                .valuePolicyResolver(stringPolicyResolver)
                .rootNode(focusOdo)
                .now(now);

        mappingBuilder = LensUtil.addAssignmentPathVariables(mappingBuilder, assignmentPathVariables, prismContext);

        MappingImpl<V, D> mapping = mappingBuilder.build();

        ItemPath itemPath = mapping.getOutputPath();
        if (itemPath == null) {
            // no output element, i.e. this is a "validation mapping"
            return mapping;
        }

        if (defaultTargetObject != null) {
            Item<V, D> existingTargetItem = (Item<V, D>) defaultTargetObject.findItem(itemPath);
            if (existingTargetItem != null && !existingTargetItem.isEmpty()
                    && mapping.getStrength() == MappingStrengthType.WEAK) {
                LOGGER.trace("Mapping {} is weak and target already has a value {}, skipping.", mapping, existingTargetItem);
                return null;
            }
        }

        return mapping;
    }
}
