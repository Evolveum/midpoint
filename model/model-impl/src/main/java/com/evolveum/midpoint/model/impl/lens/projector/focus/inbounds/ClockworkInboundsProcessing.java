/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkShadowInboundsPreparation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluation of inbound mappings from all projections in given lens context.
 *
 * Responsibility of this class:
 *
 * 1. collects inbound mappings to be evaluated
 * 2. evaluates them
 * 3. consolidates the results into deltas
 */
public class ClockworkInboundsProcessing<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkInboundsProcessing.class);

    @NotNull private final LensContext<F> context;
    @NotNull private final ModelBeans beans;
    @NotNull private final MappingEvaluationEnvironment env;
    @NotNull private final OperationResult result;

    /**
     * Key: target item path, value: InboundMappingInContext(mapping, projectionCtx)
     */
    private final PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap = new PathKeyedMap<>();

    /**
     * Output triples for individual target paths.
     */
    private final PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap = new PathKeyedMap<>();

    public ClockworkInboundsProcessing(
            @NotNull LensContext<F> context,
            @NotNull ModelBeans beans,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        this.context = context;
        this.beans = beans;
        this.env = env;
        this.result = result;
    }

    public void collectAndEvaluateMappings() throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        collectMappings();
        evaluateMappings();
        consolidateTriples();
    }

    /**
     * Used to collect all the mappings from all the projects, sorted by target property.
     *
     * Motivation: we need to evaluate them together, e.g. in case that there are several mappings
     * from several projections targeting the same property.
     */
    private void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            // Preliminary checks. (Before computing apriori delta and other things.)

            if (projectionContext.isGone()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is gone",
                        lazy(projectionContext::getHumanReadableName));
                continue;
            }
            if (!projectionContext.isCanProject()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {}: "
                                + "there is a limit to propagate changes only from resource {}",
                        lazy(projectionContext::getHumanReadableName), context.getTriggeringResourceOid());
                continue;
            }

            try {
                PrismObject<F> objectCurrentOrNew = context.getFocusContext().getObjectCurrentOrNew();
                new ClockworkShadowInboundsPreparation<>(
                        projectionContext,
                        context,
                        mappingsMap,
                        new ClockworkContext(context, env, result, beans),
                        objectCurrentOrNew,
                        getFocusDefinition(objectCurrentOrNew))
                        .collectOrEvaluate();
            } catch (StopProcessingProjectionException e) {
                LOGGER.debug("Inbound processing on {} interrupted because the projection is broken", projectionContext);
            }
        }
    }

    /**
     * Evaluate mappings consolidated from all the projections. There may be mappings from different projections to the same target.
     * We want to merge their values. Otherwise those mappings will overwrite each other.
     */
    private void evaluateMappings() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, CommunicationException {
        for (Map.Entry<ItemPath, List<InboundMappingInContext<?, ?>>> entry : mappingsMap.entrySet()) {
            evaluateMappingsForTargetItem(entry.getKey(), entry.getValue());
        }
    }

    private void evaluateMappingsForTargetItem(ItemPath targetPath, List<InboundMappingInContext<?, ?>> mappingStructList)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        assert !mappingStructList.isEmpty();
        for (InboundMappingInContext<?, ?> mappingStruct : mappingStructList) {
            MappingImpl<?, ?> mapping = mappingStruct.getMapping();
            LensProjectionContext projectionCtx = mappingStruct.getProjectionContextRequired();

            LOGGER.trace("Starting evaluation of mapping {} in {}", mapping, DebugUtil.lazy(projectionCtx::getHumanReadableName));
            beans.mappingEvaluator.evaluateMapping(mapping, context, projectionCtx, env.task, result);
            mergeMappingOutput(mapping, targetPath, projectionCtx.isDelete());
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void mergeMappingOutput(MappingImpl<V, D> mapping,
            ItemPath targetPath, boolean allToDelete) {

        DeltaSetTriple<ItemValueWithOrigin<V, D>> ivwoTriple = ItemValueWithOrigin.createOutputTriple(mapping, beans.prismContext);
        LOGGER.trace("Inbound mapping for {}\nreturned triple:\n{}",
                DebugUtil.shortDumpLazily(mapping.getDefaultSource()), DebugUtil.debugDumpLazily(ivwoTriple, 1));

        if (ivwoTriple != null) {
            if (allToDelete) {
                LOGGER.trace("Projection is going to be deleted, setting values from this projection to minus set");
                DeltaSetTriple<ItemValueWithOrigin<V, D>> convertedTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
                convertedTriple.addAllToMinusSet(ivwoTriple.getPlusSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getZeroSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getMinusSet());
                //noinspection unchecked
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, convertedTriple);
            } else {
                //noinspection unchecked
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, ivwoTriple);
            }
        }
    }

    private void consolidateTriples() throws CommunicationException, ObjectNotFoundException, ConfigurationException,
            SchemaException, SecurityViolationException, ExpressionEvaluationException {

        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        PrismObjectDefinition<F> focusDefinition = getFocusDefinition(focusNew);
        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        ObjectDelta<F> focusAPrioriDelta = focusContext.getCurrentDelta();

        Consumer<IvwoConsolidatorBuilder> customizer = builder ->
                builder
                        .deleteExistingValues(
                                builder.getItemDefinition().isSingleValue() && !rangeIsCompletelyDefined(builder.getItemPath()))
                        .skipNormalMappingAPrioriDeltaCheck(true);

        DeltaSetTripleMapConsolidation<F> consolidation = new DeltaSetTripleMapConsolidation<>(
                outputTripleMap, focusNew, focusAPrioriDelta, context::primaryFocusItemDeltaExists,
                true, customizer, focusDefinition,
                env, beans, context, result);
        consolidation.computeItemDeltas();

        focusContext.swallowToSecondaryDelta(consolidation.getItemDeltas());
    }

    private boolean rangeIsCompletelyDefined(ItemPath itemPath) {
        return mappingsMap.get(itemPath).stream()
                .allMatch(m -> m.getMapping().hasTargetRange());
    }

    private @NotNull PrismObjectDefinition<F> getFocusDefinition(PrismObject<F> focus) {
        if (focus != null && focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            return context.getFocusContextRequired().getObjectDefinition();
        }
    }
}
