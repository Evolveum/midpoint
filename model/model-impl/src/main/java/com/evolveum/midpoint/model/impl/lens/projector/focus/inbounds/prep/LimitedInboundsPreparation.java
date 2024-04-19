/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequests;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public class LimitedInboundsPreparation<T extends Containerable> extends InboundsPreparation<T> {

    private static final Trace LOGGER = TraceManager.getTrace(LimitedInboundsPreparation.class);

    public LimitedInboundsPreparation(
            @NotNull MappingEvaluationRequests evaluationRequestsBeingCollected,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull LimitedContext context,
            @NotNull PrismContainerValue<T> target,
            @NotNull PrismContainerDefinition<T> targetDefinition)
            throws SchemaException, ConfigurationException {
        super(
                evaluationRequestsBeingCollected,
                new LimitedSource(context.ctx),
                new LimitedTarget<>(target, targetDefinition, itemDefinitionMap),
                context);
    }

    @Override
    void evaluateSpecialInbounds(OperationResult result) {
        LOGGER.trace("Special inbounds are currently ignored during limited processing");
    }

    @Override
    void processAssociatedObjects(OperationResult result) {
        LOGGER.trace("Associated objects are currently ignored during limited processing");
    }
}
