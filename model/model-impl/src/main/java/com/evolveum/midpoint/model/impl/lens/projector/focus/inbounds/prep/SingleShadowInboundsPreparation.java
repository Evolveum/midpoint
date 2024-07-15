/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequests;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;

import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Prepares (collects) inbound mappings for given single shadow.
 *
 * (This is probably the hardest part of the inbound processing.)
 *
 * There are currently rather strong _limitations_ related to pre-inbounds stage, e.g.
 *
 *  * only attribute mappings are evaluated there (no associations, credentials, activation, nor auxiliary object class ones);
 *  * value metadata are not added in the pre-inbounds stage.
 *
 * To reduce complexity, the majority of the work is delegated to smaller classes:
 *
 *  * {@link MappingSource}, {@link MappingTarget}, {@link MappingContext} describing the environment in which mappings are to be evaluated
 *  * {@link MappedItems} containing {@link MappedItem} instances - intermediate structures helping with the mapping preparation
 *
 * FIXME Special mappings i.e. password and activation ones, are evaluated immediately and using different code path.
 *  This should be unified.
 *
 * TODO should the {@link MappingContext} be both included in {@link MappingSource} and here?!
 */
public class SingleShadowInboundsPreparation<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleShadowInboundsPreparation.class);

    private static final String OP_PREPARE_OR_EVALUATE = SingleShadowInboundsPreparation.class.getName() + ".prepareOrEvaluate";

    /** Place where the prepared mappings are gathered. */
    @NotNull private final MappingEvaluationRequests evaluationRequestsBeingCollected;

    /** Source - i.e. the resource object along with the whole context (like lens context for Clockwork execution). */
    @NotNull private final MappingSource source;

    /** Target - the focus including supporting data. */
    @NotNull private final MappingTarget<T> target;

    /** Context of the execution (mapping evaluation environment, operation result). */
    @NotNull private final MappingContext context;

    /** Temporary */
    @NotNull private final SpecialInboundsEvaluator specialInboundsEvaluator;

    public SingleShadowInboundsPreparation(
            @NotNull MappingEvaluationRequests evaluationRequestsBeingCollected,
            @NotNull MappingSource source,
            @NotNull MappingTarget<T> target,
            @NotNull MappingContext context,
            @NotNull SpecialInboundsEvaluator specialInboundsEvaluator) {
        this.evaluationRequestsBeingCollected = evaluationRequestsBeingCollected;
        this.source = source;
        this.target = target;
        this.context = context;
        this.specialInboundsEvaluator = specialInboundsEvaluator;
    }

    /**
     * Attribute, association, and aux OC mappings are collected. Special (password, activation) ones are evaluated.
     *
     * TODO collect all the mappings (using the same mechanism), not evaluate anything here
     */
    public void prepareOrEvaluate(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, StopProcessingProjectionException {

        OperationResult result = parentResult.subresult(OP_PREPARE_OR_EVALUATE)
                .addArbitraryObjectAsParam("source", source)
                .build();
        try {
            LOGGER.trace("Going to prepare/evaluate inbound mappings for:\n{}", source.debugDumpLazily(1));

            // Preliminary checks
            if (!source.isEligibleForInboundProcessing(result)) {
                return;
            }

            // Collecting information about all source items that are to be mapped
            MappedItems<T> mappedItems = new MappedItems<>(source, target, context);
            mappedItems.collectMappedItems();

            // Now we load the full shadow, if we need to. We no longer do that at other places.
            source.loadFullShadowIfNeeded(mappedItems.isFullStateRequired(), context, result);

            // Let's create the mappings and put them to `evaluationRequestsBeingCollected`
            mappedItems.createMappings(evaluationRequestsBeingCollected, result);

            // Evaluation of special mappings. This part will be transformed to the same style as the other mappings (eventually).
            if (!source.isProjectionBeingDeleted()) {
                specialInboundsEvaluator.evaluateSpecialInbounds(result);
            } else {
                // TODO why we are skipping this evaluation?
                LOGGER.trace("Skipping evaluation of special inbounds because of projection DELETE delta");
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** FIXME TEMPORARY */
    public interface SpecialInboundsEvaluator {

        /**
         * Evaluates special inbounds (password, activation). Temporary. Implemented only for the full processing case.
         */
        void evaluateSpecialInbounds(OperationResult result) throws SchemaException, ExpressionEvaluationException,
                CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;
    }
}
