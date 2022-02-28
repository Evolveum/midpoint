/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsProcessing;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * This tiny class serves as a bridge between the world of synchronization and the world of mappings.
 */
class PreMappingsEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(PreMappingsEvaluation.class);

    private static final String OP_EVALUATE = PreMappingsEvaluation.class.getName() + ".evaluate";

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final F preFocus;
    @NotNull private final ModelBeans beans;

    PreMappingsEvaluation(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans) throws SchemaException {
        this.syncCtx = syncCtx;
        this.preFocus = syncCtx.getPreFocus();
        this.beans = beans;
    }

    /**
     * We simply copy matching attributes from the resource object to the focus.
     */
    public void evaluate(OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addParam("shadow", syncCtx.getShadowedResourceObject())
                .build();
        try {
            MappingEvaluationEnvironment env =
                    new MappingEvaluationEnvironment(
                            "pre-inbounds", beans.clock.currentTimeXMLGregorianCalendar(), syncCtx.getTask());
            new PreInboundsProcessing<>(syncCtx, beans, env, result)
                    .collectAndEvaluateMappings();

            LOGGER.debug("Pre-focus:\n{}", preFocus.debugDumpLazily(1));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }
}
