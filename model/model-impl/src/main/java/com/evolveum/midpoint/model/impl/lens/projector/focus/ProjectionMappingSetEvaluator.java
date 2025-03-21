/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.ActivationProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.ProjectionCredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.FullInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forModelContext;

/**
 * Evaluates a set of mappings related to a projection.
 *
 * Currently used for:
 *
 * - outbound password mappings ({@link ProjectionCredentialsProcessor}),
 * - outbound existence and activation mappings ({@link ActivationProcessor}),
 * - inbound password and activation mappings ({@link FullInboundsProcessing}).
 *
 * TODO Consider merging evaluation of these special mappings with the evaluation of standard attribute/association mappings.
 *
 * Special responsibilities:
 *
 * 1. Creation of {@link Mapping} objects from beans ({@link AbstractMappingType}.
 *
 * Exclusions:
 *
 * 1. Not doing chaining. This may change in the future.
 * 2. Consolidation of output triples into deltas.
 *
 * NOTE: This functionality was originally present in `evaluateMappingSetProjection` method in {@link MappingEvaluator} class.
 *
 * TODO: Change from singleton to instance-per-operation. Then rename {@link MappingEvaluatorParams} to a builder.
 *  Just like {@link FocalMappingSetEvaluation} + {@link FocalMappingSetEvaluationBuilder}.
 *
 * @see FocalMappingSetEvaluation
 */
@Component
public class ProjectionMappingSetEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionMappingSetEvaluator.class);

    @Autowired private MappingFactory mappingFactory;
    @Autowired private PrismContext prismContext;
    @Autowired private MappingEvaluator mappingEvaluator;

    public <V extends PrismValue,
            D extends ItemDefinition<?>,
            T extends ObjectType,
            F extends FocusType>
    Map<UniformItemPath, MappingOutputStruct<V>> evaluateMappingsToTriples(
            MappingEvaluatorParams<V, D, T, F> params,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException,
            MappingLoader.NotLoadedException {

        String mappingDesc = params.getMappingDesc();
        LensElementContext<T> targetContext = params.getTargetContext();
        PrismObjectDefinition<T> targetObjectDefinition = targetContext.getObjectDefinition();
        ItemPath defaultTargetItemPath = params.getDefaultTargetItemPath();

        Map<UniformItemPath, MappingOutputStruct<V>> outputTripleMap = new HashMap<>();
        XMLGregorianCalendar nextRecomputeTime = null;
        String triggerOriginDescription = null;
        Collection<MappingConfigItem> mappingConfigItems = params.getMappingConfigItems(); // [EP:M:OM] [EP:M:IM] DONE
        Collection<MappingImpl<V, D>> mappings = new ArrayList<>(mappingConfigItems.size());

        for (MappingConfigItem mappingConfigItem : mappingConfigItems) {

            MappingType mappingBean = mappingConfigItem.value();
            // [EP:M:OM] [EP:M:IM] DONE
            MappingBuilder<V, D> mappingBuilder = mappingFactory.createMappingBuilder(mappingConfigItem, mappingDesc);
            String mappingName = mappingBean.getName();

            if (!mappingBuilder.isApplicableToChannel(params.getContext().getChannel())) {
                LOGGER.trace("Mapping {} not applicable to channel {}, skipping", mappingName, params.getContext().getChannel());
                continue;
            }
            if (!task.canSee(mappingBean)) {
                LOGGER.trace("Mapping {} not applicable to the execution mode, skipping", mappingName);
                continue;
            }

            if (targetContext instanceof LensProjectionContext) {
                mappingBuilder.ignoreValueMetadata(); // no value metadata for outbound mappings
            }

            mappingBuilder.now(params.getNow());
            if (defaultTargetItemPath != null) {
                mappingBuilder.defaultTargetDefinition(
                        targetObjectDefinition.findItemDefinition(defaultTargetItemPath));
            } else {
                mappingBuilder.defaultTargetDefinition(params.getTargetItemDefinition());
            }
            mappingBuilder.defaultTargetPath(defaultTargetItemPath);
            mappingBuilder.targetContextDefinition(targetObjectDefinition);
            mappingBuilder.defaultSourceContextIdi(params.getSourceContext());

            // Initialize mapping (using Inversion of Control)
            MappingBuilder<V, D> initializedMappingBuilder = params.getInitializer().initialize(mappingBuilder);

            MappingImpl<V, D> mapping = initializedMappingBuilder.build();

            mapping.evaluateTimeValidity(task, result);
            boolean timeConstraintValid = mapping.isTimeConstraintValid();

            if (params.getEvaluateCurrent() == MappingTimeEval.CURRENT && !timeConstraintValid) {
                LOGGER.trace("Mapping {} is non-current, but evaluating current mappings, skipping", mappingName);
            } else if (params.getEvaluateCurrent() == MappingTimeEval.FUTURE && timeConstraintValid) {
                LOGGER.trace("Mapping {} is current, but evaluating non-current mappings, skipping", mappingName);
            } else {
                mappings.add(mapping);
            }
        }

        boolean targetValueAvailable = params.isTargetValueAvailable();
        PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();

        LOGGER.trace("Going to process {} mappings for {}", mappings.size(), mappingDesc);
        LOGGER.trace("Target context: {}", targetContext);

        for (MappingImpl<V, D> mapping : mappings) {

            LOGGER.trace("Evaluating mapping: {}", mapping.getContextDescription());

            if (mapping.getStrength() == MappingStrengthType.WEAK) {
                LOGGER.trace("-> weak mapping, will be evaluated in the second run");
                continue;
            }

            UniformItemPath mappingOutputPathUniform = prismContext.toUniformPathKeepNull(mapping.getOutputPath());
            if (params.isFixTarget()
                    && mappingOutputPathUniform != null
                    && defaultTargetItemPath != null
                    && !mappingOutputPathUniform.equivalent(defaultTargetItemPath)) {
                throw new ExpressionEvaluationException("Target cannot be overridden in " + mappingDesc);
            }

            if (params.getAPrioriTargetDelta() != null && mappingOutputPathUniform != null) {
                ItemDelta<?, ?> aPrioriItemDelta = params.getAPrioriTargetDelta().findItemDelta(mappingOutputPathUniform);
                if (mapping.getStrength() != MappingStrengthType.STRONG) {
                    if (aPrioriItemDelta != null && !aPrioriItemDelta.isEmpty()) {
                        LOGGER.trace("-> a-priori delta for non-strong mapping, ignoring the mapping");
                        continue;
                    }
                }
            }

            mappingEvaluator.evaluateMapping(mapping, forModelContext(params.getContext()), task, result);

            PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
            LOGGER.trace("Output triple:\n{}", mappingOutputTriple == null ? null : mappingOutputTriple.debugDumpLazily(1));

            if (isMeaningful(mappingOutputTriple)) {

                MappingOutputStruct<V> mappingOutputStruct =
                        outputTripleMap.computeIfAbsent(mappingOutputPathUniform, k -> new MappingOutputStruct<>());

                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    mappingOutputStruct.setStrongMappingWasUsed(true);

                    if (!targetValueAvailable
                            && params.getTargetLoader() != null
                            && aPrioriTargetObject != null
                            && aPrioriTargetObject.getOid() != null
                            && !params.getTargetLoader().isLoaded()) {
                        aPrioriTargetObject = params.getTargetLoader().load("strong mapping", task, result);
                        LOGGER.trace("Loaded object because of strong mapping: {}", aPrioriTargetObject);
                        targetValueAvailable = true;
                    }
                }

                // experimental
                if (mapping.isPushChanges()) {
                    mappingOutputStruct.setPushChanges(true);

                    // TODO should we really load the resource object also if we are pushing the changes?
                    //  (but it looks like we have to!)
                    if (!targetValueAvailable
                            && params.getTargetLoader() != null
                            && aPrioriTargetObject != null
                            && aPrioriTargetObject.getOid() != null
                            && !params.getTargetLoader().isLoaded()) {
                        aPrioriTargetObject = params.getTargetLoader().load("pushing changes", task, result);
                        LOGGER.trace("Loaded object because of pushing changes: {}", aPrioriTargetObject);
                        targetValueAvailable = true;
                    }
                }

                PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
                if (outputTriple == null) {
                    mappingOutputStruct.setOutputTriple(mappingOutputTriple);
                } else {
                    outputTriple.merge(mappingOutputTriple);
                }

            } else {
                LOGGER.trace("Output triple of mapping is NOT meaningful, ignoring it");
            }

        }

        if (params.isEvaluateWeak()) {
            // Second pass, evaluate only weak mappings
            for (MappingImpl<V, D> mapping : mappings) {

                if (mapping.getStrength() != MappingStrengthType.WEAK) {
                    continue;
                }

                LOGGER.trace("Evaluating weak mapping: {}", mapping.getContextDescription());

                UniformItemPath mappingOutputPath = prismContext.toUniformPathKeepNull(mapping.getOutputPath());
                if (params.isFixTarget()
                        && mappingOutputPath != null
                        && defaultTargetItemPath != null
                        && !mappingOutputPath.equivalent(defaultTargetItemPath)) {
                    throw new ExpressionEvaluationException("Target cannot be overridden in " + mappingDesc);
                }

                MappingOutputStruct<V> mappingOutputStruct =
                        outputTripleMap.computeIfAbsent(mappingOutputPath, k -> new MappingOutputStruct<>());

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
                    LOGGER.trace("Previous mapping(s) produced zero/positive output, skipping weak mapping");
                    continue;
                }

                Item<V, D> aPrioriTargetItem = null;
                if (aPrioriTargetObject != null && mappingOutputPath != null) {
                    aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                }
                if (hasNoValue(aPrioriTargetItem)) {

                    LOGGER.trace("A priori target item has no value, going to evaluate the mapping; item: {}", aPrioriTargetItem);

                    mappingOutputStruct.setWeakMappingWasUsed(true);

                    mappingEvaluator.evaluateMapping(mapping, forModelContext(params.getContext()), task, result);

                    PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
                    if (mappingOutputTriple != null) {

                        // This may be counter-intuitive to load object after the mapping is executed
                        // But the mapping may not be activated (e.g. condition is false). And in that
                        // case we really do not want to trigger object loading.
                        // This is all not right. See MID-3847
                        if (!targetValueAvailable
                                && params.getTargetLoader() != null
                                && aPrioriTargetObject != null
                                && aPrioriTargetObject.getOid() != null
                                && !params.getTargetLoader().isLoaded()) {
                            aPrioriTargetObject = params.getTargetLoader().load("weak mapping", task, result);
                            LOGGER.trace("Loaded object because of weak mapping: {}", aPrioriTargetObject);
                            targetValueAvailable = true;
                        }
                        if (aPrioriTargetObject != null && mappingOutputPath != null) {
                            aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                            if (targetValueAvailable) {
                                LOGGER.trace("A priori target item after object loaded: {}", aPrioriTargetItem);
                            }
                        }
                        if (!hasNoValue(aPrioriTargetItem)) {
                            LOGGER.trace("A priori target item has a value, ignoring mapping output; item: {}", aPrioriTargetItem);
                            continue;
                        }

                        if (outputTriple == null) {     // this is currently always true (see above)
                            mappingOutputStruct.setOutputTriple(mappingOutputTriple);
                        } else {
                            outputTriple.merge(mappingOutputTriple);
                        }
                    }
                } else {
                    LOGGER.trace("A priori target item has a value, skipping mapping evaluation; item: {}", aPrioriTargetItem);
                }
            }
        }

        LOGGER.trace("Mapping(s) are evaluated, going to process output triple(s)");

        MappingOutputProcessor<V> processor = params.getProcessor();
        for (Map.Entry<UniformItemPath, MappingOutputStruct<V>> outputTripleMapEntry : outputTripleMap.entrySet()) {
            UniformItemPath mappingOutputPath = outputTripleMapEntry.getKey();
            LOGGER.trace("Processing output triple for path '{}'", mappingOutputPath);

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
                    LOGGER.trace("Expression resulted in null triple, skipping");
                    continue;
                }

                D targetItemDefinition;
                if (mappingOutputPath != null) {
                    targetItemDefinition = targetObjectDefinition.findItemDefinition(mappingOutputPath);
                    if (targetItemDefinition == null) {
                        throw new SchemaException("No definition for item " + mappingOutputPath + " in " + targetObjectDefinition);
                    }
                } else {
                    targetItemDefinition = params.getTargetItemDefinition();
                }
                //noinspection unchecked
                ItemDelta<V, D> targetItemDelta = (ItemDelta<V, D>) targetItemDefinition.createEmptyDelta(mappingOutputPath);

                Item<V, D> aPrioriTargetItem;
                if (aPrioriTargetObject != null) {
                    aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
                } else {
                    aPrioriTargetItem = null;
                }
                LOGGER.trace("A priori target item: {}", aPrioriTargetItem);

                // WARNING
                // Following code seems to be wrong. It is not very relativistic. It seems to always
                // go for replace.
                // It seems that it is only used for activation mappings (outbound and inbound). As
                // these are quite special single-value properties then it seems to work fine
                // (with the exception of MID-3418). Todo: make it more relativistic: MID-3419

                if (targetContext.isAdd()) {

                    Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
                    if (nonNegativeValues.isEmpty()) {
                        LOGGER.trace("The output is null or empty for ADD operation, skipping");
                        continue;
                    }
                    LOGGER.trace("Setting up the REPLACE delta because of ADD operation; values: {}", nonNegativeValues);
                    targetItemDelta.setValuesToReplace(PrismValueCollectionsUtil.cloneCollection(nonNegativeValues));

                } else {

                    // if we have fresh information (full shadow) AND the mapping used to derive the information was strong,
                    // we will consider all values (zero & plus sets) -- otherwise, we take only the "plus" (i.e. changed) set

                    // the first case is necessary, because in some situations (e.g. when mapping is changed)
                    // the evaluator sees no differences w.r.t. real state, even if there is a difference
                    // - and we must have a way to push new information onto the resource

                    Collection<V> valuesToReplace;

                    if (targetValueAvailable && (mappingOutputStruct.isStrongMappingWasUsed() || mappingOutputStruct.isPushChanges())) {
                        valuesToReplace = outputTriple.getNonNegativeValues();
                    } else {
                        valuesToReplace = outputTriple.getPlusSet();
                    }

                    LOGGER.trace(
                            "Computed new values when targetValueAvailable={}, isStrongMappingWasUsed={}, pushingChange={}: {}",
                            targetValueAvailable, mappingOutputStruct.isStrongMappingWasUsed(),
                            mappingOutputStruct.isPushChanges(), valuesToReplace);

                    if (!valuesToReplace.isEmpty()) {

                        // if what we want to set is the same as is already in the shadow, we skip that
                        // (we insist on having full shadow, to be sure we work with current data)

                        if (targetValueAvailable && targetContext.isFresh() && aPrioriTargetItem != null) {
                            Collection<V> valuesPresent = aPrioriTargetItem.getValues();
                            if (PrismValueCollectionsUtil.equalsRealValues(valuesPresent, valuesToReplace)) {
                                LOGGER.trace("Computed values are equivalent to existing ones, skipping creation of a delta");
                                continue;
                            } else {
                                LOGGER.trace("Will create a delta, because computed values are NOT equivalent to existing ones, "
                                        + "which are: {}", valuesPresent);
                            }
                        } else {
                            LOGGER.trace("Not checking with the existing values because of no fresh full target object");
                        }

                        targetItemDelta.setValuesToReplace(PrismValueCollectionsUtil.cloneCollection(valuesToReplace));

                        applyEstimatedOldValueInReplaceCaseIfCredentials(params, targetItemDelta, outputTriple);

                    } else if (outputTriple.hasMinusSet()) {
                        LOGGER.trace("The output is null or empty and there is a minus set, clearing the target property");
                        targetItemDelta.setValueToReplace();

                        applyEstimatedOldValueInReplaceCaseIfCredentials(params, targetItemDelta, outputTriple);

                    } else {
                        LOGGER.trace("The output is null or empty, skipping creation of a delta");
                        // implicit "continue" (the delta is empty here)
                    }
                }

                if (targetItemDelta.isEmpty()) {
                    continue;
                }

                LOGGER.trace("Adding new delta: {}", targetItemDelta);
                targetContext.swallowToSecondaryDelta(targetItemDelta);
            }

        }

        // Figure out recompute time

        for (MappingImpl<V, D> mapping : mappings) {
            XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
            if (mappingNextRecomputeTime != null) {
                if (mapping.isConditionSatisfied() && (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER)) {
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

    /**
     * Sets estimated old values - but only for credentials in a projection.
     *
     * Note: Here we intentionally DO NOT set estimated old values in the delta for all the other cases.
     * The reason is that the old values here would be determined by what the mapping THINKS was there.
     * The reality may be different. The {@link LensElementContext#swallowToSecondaryDelta(ItemDelta)} would know better.
     *
     * The only exception is when we are dealing with credentials in a projection. In that case, we probably won't be able
     * to obtain the estimatedOldValue from the projection (as it's usually not possible to fetch the password from the resource),
     * and there is a code that relies on the estimatedOldValue being set for credentials changes
     * `DeltaExecution#isExecuteAsSelf`. We should replace the magic in that method by something more serious.
     * Also here we could probably restrict providing old values only to `credentials/password/value`? Maybe, maybe not.
     */
    private <V extends PrismValue, D extends ItemDefinition<?>> void applyEstimatedOldValueInReplaceCaseIfCredentials(
            MappingEvaluatorParams<?, ?, ?, ?> params,
            ItemDelta<V, D> targetItemDelta,
            PrismValueDeltaSetTriple<V> outputTriple) {
        if (!params.isTargetProjection() || !targetItemDelta.getPath().startsWith(ShadowType.F_CREDENTIALS)) {
            return;
        }
        Collection<V> nonPositiveValues = outputTriple.getNonPositiveValues();
        if (nonPositiveValues.isEmpty()) {
            return;
        }
        targetItemDelta.setEstimatedOldValuesWithCloning(nonPositiveValues);
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
        //noinspection RedundantIfStatement
        if (hasNoOrHashedValuesOnly(mappingOutputTriple.getMinusSet())
                && hasNoOrHashedValuesOnly(mappingOutputTriple.getZeroSet())
                && hasNoOrHashedValuesOnly(mappingOutputTriple.getPlusSet())) {
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
            if (val instanceof ProtectedStringType protectedString) {
                if (!protectedString.isHashed()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoValue(Item<?, ?> aPrioriTargetItem) {
        return aPrioriTargetItem == null
                || (aPrioriTargetItem.isEmpty() && !aPrioriTargetItem.isIncomplete());
    }
}
