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
 * This is an abstract class that has two subclasses:
 *
 * - {@link FullInboundsPreparation} - the "full" inbounds preparation that takes place during clockwork execution
 * (namely, within the projector)
 * - {@link LimitedInboundsPreparation} - the "pre-inbounds" where we construct preliminary focus object to be
 * used during correlation
 *
 * There are currently rather strong _limitations_ related to pre-inbounds stage, e.g.
 *
 *  * only attribute mappings are evaluated there (no associations, credentials, activation, nor auxiliary object class ones);
 *  * value metadata are not added in the pre-inbounds stage.
 *
 * To reduce complexity, the majority of the work is delegated to smaller classes:
 *
 *  * {@link MSource}, {@link Target}, {@link Context} describing the environment in which mappings are to be evaluated
 *  * {@link MappedItems} containing {@link MappedItem} instances - intermediate structures helping with the mapping preparation
 *
 * FIXME Special mappings i.e. password and activation ones, are evaluated immediately and using different code path.
 *  This should be unified.
 *
 * TODO should the {@link Context} be both included in {@link MSource} and here?!
 */
abstract class InboundsPreparation<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(InboundsPreparation.class);

    private static final String OP_COLLECT_OR_EVALUATE = InboundsPreparation.class.getName() + ".collectOrEvaluate";

    /** Place where the prepared mappings are gathered. */
    @NotNull final MappingEvaluationRequests evaluationRequestsBeingCollected;

    /** Source - i.e. the resource object along with the whole context (like lens context for Clockwork execution). */
    @NotNull final MSource source;

    /** Target - the focus including supporting data. */
    @NotNull final Target<T> target;

    /** Context of the execution (mapping evaluation environment, operation result). */
    @NotNull final Context context;

    InboundsPreparation(
            @NotNull MappingEvaluationRequests evaluationRequestsBeingCollected,
            @NotNull MSource source,
            @NotNull Target<T> target,
            @NotNull Context context) {
        this.evaluationRequestsBeingCollected = evaluationRequestsBeingCollected;
        this.source = source;
        this.target = target;
        this.context = context;
    }

    /**
     * Attribute, association, and aux OC mappings are collected. Special (password, activation) ones are evaluated.
     *
     * TODO collect all the mappings (using the same mechanism), not evaluate anything here
     */
    public void collectOrEvaluate(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, StopProcessingProjectionException {

        OperationResult result = parentResult.subresult(OP_COLLECT_OR_EVALUATE)
                .addArbitraryObjectAsParam("source", source)
                .build();
        try {
            LOGGER.trace("Going to collect/evaluate of inbound mappings for:\n{}", source.debugDumpLazily(1));

            // Preliminary checks
            if (!source.isEligibleForInboundProcessing(result)) {
                return;
            }
            source.checkResourceObjectDefinitionPresent();

            // Collecting information about all source items that are to be mapped
            MappedItems<T> mappedItems = new MappedItems<>(source, target, context);
            mappedItems.collectMappedItems();

            // Now we load the full shadow, if we need to. We no longer do that at other places.
            //source.loadFullShadowIfNeeded(mappedItems.isFullStateRequired(), context, result);

            // Let's create the mappings and put them to mappingsMap
            for (var mappedItem : mappedItems.getMappedItems()) {
                mappedItem.createMappings(evaluationRequestsBeingCollected, result);
            }

            // Evaluation of special mappings. This part will be transformed to the same style as the other mappings (eventually).
            if (!source.isProjectionBeingDeleted()) {
                evaluateSpecialInbounds(result);
            } else {
                // TODO why we are skipping this evaluation?
                LOGGER.trace("Skipping evaluation of special inbounds because of projection DELETE delta");
            }

            executeComplexProcessing(result);

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Evaluates special inbounds (password, activation). Temporary. Implemented only for the full processing case.
     */
    abstract void evaluateSpecialInbounds(OperationResult result) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    /** Complex processing for shadow attributes. Only for the full processing case. Currently limited to reference ones. */
    abstract void executeComplexProcessing(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, StopProcessingProjectionException;
}
