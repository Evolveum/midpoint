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
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTripleUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Evaluation of inbound mappings from all available projections.
 *
 * Executes either under the clockwork (as part of the projector), taking all projection contexts into account.
 * Or it executes during correlation - or, generally, before the clockwork - processing only the source shadow.
 *
 * Responsibility of this class:
 *
 * 1. collects inbound mappings to be evaluated
 * 2. evaluates them
 * 3. consolidates the results into deltas
 */
abstract class AbstractInboundsProcessing<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractInboundsProcessing.class);

    @NotNull final ModelBeans beans;
    @NotNull final MappingEvaluationEnvironment env;
    @NotNull final OperationResult result;

    /**
     * Key: target item path, value: InboundMappingInContext(mapping, projectionCtx [nullable])
     */
    final PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap = new PathKeyedMap<>();

    /** Here we cache definitions for regular and identity items. */
    final PathKeyedMap<ItemDefinition<?>> itemDefinitionMap = new PathKeyedMap<>();

    /**
     * Output triples for individual target paths.
     */
    private final PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap = new PathKeyedMap<>();

    AbstractInboundsProcessing(
            @NotNull ModelBeans beans,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        this.beans = beans;
        this.env = env;
        this.result = result;
    }

    public void collectAndEvaluateMappings() throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        collectMappings();
        evaluateMappings();
        consolidateTriples();

        normalizeChangedFocusIdentityData();
    }

    /**
     * Focus identity data produced by inbound mappings need to be normalized.
     * Currently applicable only to clockwork processing.
     */
    abstract void normalizeChangedFocusIdentityData() throws ConfigurationException, SchemaException, ExpressionEvaluationException;

    /**
     * Collects the mappings - either from all projections (for clockwork) or from the input shadow (for pre-mappings).
     *
     * In the former case, special mappings are evaluated here (until fixed).
     */
    abstract void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException;

    /**
     * Evaluate mappings collected from all the projections. There may be mappings from different projections to the same target.
     * We want to merge their values. Otherwise those mappings will overwrite each other.
     */
    private void evaluateMappings() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, CommunicationException {
        for (Map.Entry<ItemPath, List<InboundMappingInContext<?, ?>>> entry : mappingsMap.entrySet()) {
            evaluateMappingsForTargetItem(entry.getKey(), entry.getValue());
        }
    }

    private void evaluateMappingsForTargetItem(ItemPath targetPath, List<InboundMappingInContext<?, ?>> listOfMappingsInContext)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        assert !listOfMappingsInContext.isEmpty();
        for (InboundMappingInContext<?, ?> mappingInCtx : listOfMappingsInContext) {
            MappingImpl<?, ?> mapping = mappingInCtx.getMapping();

            LOGGER.trace("Starting evaluation of {}", mappingInCtx);
            beans.mappingEvaluator.evaluateMapping(
                    mapping,
                    mappingInCtx.getLensContext(), // nullable
                    mappingInCtx.getProjectionContext(), // nullable
                    env.task,
                    result);

            mergeMappingOutput(mapping, targetPath, mappingInCtx.isProjectionBeingDeleted());
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void mergeMappingOutput(MappingImpl<V, D> mapping,
            ItemPath targetPath, boolean allToDelete) {

        DeltaSetTriple<ItemValueWithOrigin<V, D>> ivwoTriple = ItemValueWithOrigin.createOutputTriple(mapping);
        LOGGER.trace("Inbound mapping for {}\nreturned triple:\n{}",
                DebugUtil.shortDumpLazily(mapping.getDefaultSource()), DebugUtil.debugDumpLazily(ivwoTriple, 1));

        if (ivwoTriple != null) {
            if (allToDelete) {
                LOGGER.trace("Projection is going to be deleted, setting values from this projection to minus set");
                DeltaSetTriple<ItemValueWithOrigin<V, D>> convertedTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
                convertedTriple.addAllToMinusSet(ivwoTriple.getPlusSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getZeroSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getMinusSet());
                //noinspection unchecked,rawtypes
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, convertedTriple);
            } else {
                //noinspection unchecked,rawtypes
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, ivwoTriple);
            }
        }
    }

    private void consolidateTriples() throws CommunicationException, ObjectNotFoundException, ConfigurationException,
            SchemaException, SecurityViolationException, ExpressionEvaluationException {

        PrismObject<F> focusNew = getFocusNew();
        ObjectDelta<F> focusAPrioriDelta = getFocusAPrioriDelta();

        //noinspection rawtypes
        Consumer<IvwoConsolidatorBuilder> customizer = builder ->
                builder
                        .deleteExistingValues(
                                builder.getItemDefinition().isSingleValue() && !rangeIsCompletelyDefined(builder.getItemPath()))
                        .skipNormalMappingAPrioriDeltaCheck(true);

        DeltaSetTripleMapConsolidation<F> consolidation = new DeltaSetTripleMapConsolidation<>(
                outputTripleMap,
                focusNew,
                focusAPrioriDelta,
                getFocusPrimaryItemDeltaExistsProvider(),
                true,
                customizer,
                itemDefinitionMap::get,
                env,
                beans,
                getLensContextIfPresent(),
                result);
        consolidation.computeItemDeltas();

        applyComputedDeltas(consolidation.getItemDeltas());
    }

    private boolean rangeIsCompletelyDefined(ItemPath itemPath) {
        return mappingsMap.get(itemPath).stream()
                .allMatch(m -> m.getMapping().hasTargetRange());
    }

    abstract @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus);

    abstract @Nullable PrismObject<F> getFocusNew() throws SchemaException;

    protected abstract @Nullable ObjectDelta<F> getFocusAPrioriDelta();

    abstract @NotNull Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider();

    abstract @Nullable LensContext<?> getLensContextIfPresent();

    abstract void applyComputedDeltas(Collection<ItemDelta<?,?>> itemDeltas) throws SchemaException;
}
