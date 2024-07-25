/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.expr.AssociationSynchronizationResult;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Evaluation of inbound mappings from single projection or all available projections.
 *
 * Has two modes of operation:
 *
 * 1. *Full processing*: takes all the mappings, from all available projections, from all embedded shadows, evaluates them,
 * and consolidates them. Always executed under the clockwork. Always targeted to the focus object (user, role, and so on).
 * Treats issues like loading the full shadow, if necessary. See {@link FullInboundsProcessing}.
 *
 * 2. *Limited processing*: converts just a single shadow into a (fragment of) the target value. This is used for correlation
 * purposes. May execute within or outside the clockwork. May target any object: user, role, but also specific assignment
 * or a custom structure. See {@link SingleShadowInboundsProcessing}.
 *
 * Responsibility of this class:
 *
 * 1. collects inbound mappings to be evaluated
 * 2. evaluates them
 * 3. consolidates the results into deltas
 *
 * @param <T> type of the target object ({@link UserType}, {@link AssignmentType}, etc)
 */
abstract class AbstractInboundsProcessing<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractInboundsProcessing.class);
    private static final String OP_EVALUATE_MAPPINGS = AbstractInboundsProcessing.class.getName() + ".evaluateMappings";

    /** Context description, time, task. */
    @NotNull final MappingEvaluationEnvironment env;

    /** All evaluation requests (i.e., mappings prepared for evaluation), indexed by the target item path. */
    @NotNull final MappingEvaluationRequestsMap evaluationRequestsMap = new MappingEvaluationRequestsMap();

    /** Here we cache definitions for both regular and identity *target* items. */
    @NotNull final PathKeyedMap<ItemDefinition<?>> itemDefinitionMap = new PathKeyedMap<>();

    /**
     * Output triples for individual target paths. This is the actual result of mapping evaluation.
     * They are converted into deltas by consolidation.
     */
    @NotNull private final DeltaSetTripleIvwoMap outputTripleMap = new DeltaSetTripleIvwoMap();

    @NotNull final ModelBeans beans = ModelBeans.get();

    AbstractInboundsProcessing(@NotNull MappingEvaluationEnvironment env) {
        this.env = env;
    }

    /** Full processing, resulting in deltas being applied (to focus context or target object). */
    public void executeToDeltas(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        executeToTriples(result);
        consolidateTriples(result);
    }

    /** Partial processing that stops after triples are computed, i.e. just prepares and evaluates the mappings. */
    void executeToTriples(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        prepareMappings(result);
        evaluateMappings(result);
    }

    /**
     * Collects the mappings - either from all projections (for full processing) or from the input shadow (for pre-mappings).
     *
     * In the former case, special mappings are evaluated here (until fixed).
     */
    abstract void prepareMappings(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException;

    /**
     * Evaluate mappings collected from all the projections. There may be mappings from different projections to the same target.
     * We want to merge their values. Otherwise, those mappings will overwrite each other.
     */
    private void evaluateMappings(OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, CommunicationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE_MAPPINGS)
                .build();
        try {
            for (var entry : List.copyOf(evaluationRequestsMap.entrySet())) {
                List<MappingEvaluationRequest<?, ?>> mappings = entry.getValue();
                assert !mappings.isEmpty();
                for (var mapping : mappings) {
                    evaluateMapping(entry.getKey(), mapping, result);
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void evaluateMapping(
            ItemPath targetPath, MappingEvaluationRequest<V, D> evaluationRequest, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        LOGGER.trace("Starting evaluation of {}", evaluationRequest);

        var mapping = evaluationRequest.getMapping();
        beans.mappingEvaluator.evaluateMapping(
                mapping,
                evaluationRequest.getEvaluationContext(),
                env.task,
                result);

        mergeMappingOutput(mapping, targetPath, evaluationRequest.isSourceBeingDeleted());
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void mergeMappingOutput(
            MappingImpl<V, D> mapping, ItemPath targetPath, boolean allToDelete) {

        var ivwoTriple = ItemValueWithOrigin.createOutputTriple(mapping);
        LOGGER.trace("Inbound mapping for {}\nreturned triple:\n{}",
                DebugUtil.shortDumpLazily(mapping.getDefaultSource()), DebugUtil.debugDumpLazily(ivwoTriple, 1));

        if (ivwoTriple == null) {
            return;
        }
        if (allToDelete) {
            LOGGER.trace("Projection is going to be deleted, setting values from this projection to minus set");
            DeltaSetTriple<ItemValueWithOrigin<V, D>> convertedTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
            convertedTriple.addAllToMinusSet(ivwoTriple.getPlusSet());
            convertedTriple.addAllToMinusSet(ivwoTriple.getZeroSet());
            convertedTriple.addAllToMinusSet(ivwoTriple.getMinusSet());
            outputTripleMap.putOrMerge(targetPath, convertedTriple);
            return; // ignoring embedded triple maps
        }

        outputTripleMap.putOrMerge(targetPath, ivwoTriple);

        // Let's also treat inner triples and additional data, if there are any
        if (mapping.getOutputTriple() instanceof AssociationSynchronizationResult<V> associationSynchronizationResult) {
            outputTripleMap.putOrMergeAll(
                    associationSynchronizationResult.getInnerDeltaSetTriplesMap());
            itemDefinitionMap.putAll(
                    associationSynchronizationResult.getInnerItemDefinitionsMap());
            evaluationRequestsMap.putAll(
                    associationSynchronizationResult.getInnerMappingEvaluationRequestsMap());
        }
    }

    private void consolidateTriples(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ConfigurationException, SchemaException,
            SecurityViolationException, ExpressionEvaluationException {

        Consumer<IvwoConsolidatorBuilder<?, ?, ?>> customizer = builder ->
                builder
                        .deleteExistingValues(
                                builder.getItemDefinition().isSingleValue() && !rangeIsCompletelyDefined(builder.getItemPath()))
                        .skipNormalMappingAPrioriDeltaCheck(true);

        DeltaSetTripleMapConsolidation<T> consolidation = new DeltaSetTripleMapConsolidation<>(
                outputTripleMap,
                getTargetNew(),
                getFocusAPrioriDeltaProvider(),
                getFocusPrimaryItemDeltaExistsProvider(),
                true,
                customizer,
                this::getItemDefinition,
                env,
                getLensContextIfPresent(),
                result);
        consolidation.computeItemDeltas();

        applyComputedDeltas(consolidation.getItemDeltas());
    }

    @NotNull private ItemDefinition<?> getItemDefinition(@NotNull ItemPath itemPath) {
        return Objects.requireNonNull(
                itemDefinitionMap.get(itemPath),
                () -> "No cached definition for " + itemPath + " found. Having definitions for: " + itemDefinitionMap.keySet());
    }

    private boolean rangeIsCompletelyDefined(ItemPath itemPath) {
        return evaluationRequestsMap.getRequired(itemPath).stream()
                .allMatch(m -> m.getMapping().hasTargetRange());
    }

    /** For full clockwork mode, this returns the "new" version of the focus. */
    abstract @Nullable PrismContainerValue<T> getTargetNew() throws SchemaException;

    /** For full clockwork mode, this returns the "current" version of the focus. */
    abstract @Nullable PrismContainerValue<T> getTarget() throws SchemaException;

    protected abstract @NotNull APrioriDeltaProvider getFocusAPrioriDeltaProvider();

    abstract @NotNull Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider();

    abstract @Nullable LensContext<?> getLensContextIfPresent();

    abstract void applyComputedDeltas(Collection<? extends ItemDelta<?,?>> itemDeltas) throws SchemaException;

    public @NotNull DeltaSetTripleIvwoMap getOutputTripleMap() {
        return outputTripleMap;
    }

    public @NotNull PathKeyedMap<ItemDefinition<?>> getItemDefinitionMap() {
        return itemDefinitionMap;
    }

    public @NotNull MappingEvaluationRequestsMap getEvaluationRequestsMap() {
        return evaluationRequestsMap;
    }
}
