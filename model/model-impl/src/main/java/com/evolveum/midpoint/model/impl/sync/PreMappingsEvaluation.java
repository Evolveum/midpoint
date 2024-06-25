/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.LimitedInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SimplePreInboundsContextImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * This tiny class serves as a bridge between the world of correlation and the world of mappings.
 */
public class PreMappingsEvaluation<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(PreMappingsEvaluation.class);

    private static final String OP_EVALUATE = PreMappingsEvaluation.class.getName() + ".evaluate";

    @NotNull private final PreInboundsContext<T> ctx;
    @NotNull private final T preFocus;
    @NotNull private final ModelBeans beans = ModelBeans.get();

    PreMappingsEvaluation(@NotNull PreInboundsContext<T> ctx) {
        this.ctx = ctx;
        this.preFocus = ctx.getPreFocus();
    }

    @VisibleForTesting
    public static <F extends FocusType> @NotNull F computePreFocus(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            @NotNull ResourceType resource,
            @NotNull Class<F> focusClass,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        SimplePreInboundsContextImpl<F> preInboundsContext = new SimplePreInboundsContextImpl<>(
                shadowedResourceObject,
                resource,
                PrismContext.get().createObjectable(focusClass),
                ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                task,
                objectTypeDefinition,
                objectTypeDefinition,
                null);
        new PreMappingsEvaluation<>(preInboundsContext)
                .evaluate(result);
        return preInboundsContext.getPreFocus();
    }

    // FIXME TEMPORARY
    public static <C extends Containerable> void computePreFocusForAssociationValue(
            @NotNull ShadowReferenceAttributeValue associationValue,
            @NotNull ResourceObjectInboundDefinition inboundDefinition,
            @NotNull ResourceType resource,
            @NotNull C targetObject,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        SimplePreInboundsContextImpl<C> preInboundsContext = new SimplePreInboundsContextImpl<>(
                associationValue.getShadowBean(),
                resource,
                targetObject,
                ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                task,
                associationValue.getTargetObjectDefinition(),
                inboundDefinition,
                (ShadowReferenceAttributeDefinition) associationValue.getDefinition());
        new PreMappingsEvaluation<>(preInboundsContext)
                .evaluate(result);
    }

    /**
     * We simply copy matching attributes from the resource object to the focus.
     */
    public void evaluate(OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addParam("shadow", ctx.getShadowedResourceObject())
                .build();
        try {
            MappingEvaluationEnvironment env =
                    new MappingEvaluationEnvironment(
                            "pre-inbounds", beans.clock.currentTimeXMLGregorianCalendar(), ctx.getTask());
            new LimitedInboundsProcessing<>(ctx, env)
                    .collectAndEvaluateMappings(result);

            LOGGER.debug("Pre-focus:\n{}", preFocus.debugDumpLazily(1));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }
}
