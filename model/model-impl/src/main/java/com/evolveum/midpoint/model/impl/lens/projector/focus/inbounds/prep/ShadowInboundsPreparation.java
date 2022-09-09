/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.List;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prepares (collects) inbound mappings for specified shadow.
 *
 * This is an abstract class that has two subclasses:
 *
 * - {@link ClockworkShadowInboundsPreparation} - the "full" inbounds preparation that takes place during clockwork execution
 * (namely, within the projector)
 * - {@link PreShadowInboundsPreparation} - the "pre-inbounds" where we construct preliminary focus object to be
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
abstract class ShadowInboundsPreparation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowInboundsPreparation.class);

    /** Place where the prepared mappings are gathered. */
    @NotNull private final PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap;

    /** Source - i.e. the resource object along with the whole context (like lens context for Clockwork execution). */
    @NotNull final MSource source;

    /** Target - the focus including supporting data. */
    @NotNull final Target<F> target;

    /** Context of the execution (mapping evaluation environment, operation result). */
    @NotNull final Context context;

    ShadowInboundsPreparation(
            @NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap,
            @NotNull MSource source,
            @NotNull Target<F> target,
            @NotNull Context context) {
        this.mappingsMap = mappingsMap;
        this.source = source;
        this.target = target;
        this.context = context;
    }

    /**
     * Attribute, association, and aux OC mappings are collected. Special (password, activation) ones are evaluated.
     * TODO collect all the mappings (using the same mechanism), not evaluate anything here
     */
    public void collectOrEvaluate()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, StopProcessingProjectionException {

        LOGGER.trace("Going to collect/evaluate of inbound mappings for:\n{}", source.debugDumpLazily(1));

        // Preliminary checks

        if (!source.isEligibleForInboundProcessing()) {
            return;
        }

        source.checkResourceObjectDefinitionPresent();

        MappedItems<F> mappedItems = new MappedItems<>(source, target, context);
        mappedItems.createMappedItems();

        // Now we load the full shadow, if we need to. We no longer do that at other places.
        source.loadFullShadowIfNeeded(mappedItems.isFullStateRequired(), context);

        // Let's create the mappings and put them to mappingsMap
        for (MappedItem<?, ?, F> mappedItem : mappedItems.getMappedItems()) {
            mappedItem.createMappings(mappingsMap);
        }

        // Evaluation of special mappings. This part will be transformed to the same style as the other mappings (eventually).
        if (!source.isProjectionBeingDeleted()) {
            evaluateSpecialInbounds();
        } else {
            // TODO why we are skipping this evaluation?
            LOGGER.trace("Skipping evaluation of special inbounds because of projection DELETE delta");
        }
    }

    /**
     * Evaluates special inbounds (password, activation). Temporary. Implemented only for clockwork case.
     */
    abstract void evaluateSpecialInbounds() throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;
}
