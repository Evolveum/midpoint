/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.impl.correlation.ResourceCorrelationDefinitionProvider;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.mappings.ShadowWithOwner;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Default implementation that fetches owned shadows via ModelService from the resource.
 */
@Component
class ShadowsWithOwnersCorrelatingProvider implements ShadowsWithOwnersProvider {
    private static final Trace LOGGER = TraceManager.getTrace(ShadowsWithOwnersCorrelatingProvider.class);
    private final CorrelationService correlationService;

    public ShadowsWithOwnersCorrelatingProvider(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @Override
    public List<ShadowWithOwner> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            int maxExamples)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        var ownedShadows = new ArrayList<ShadowWithOwner>(maxExamples);
        ctx.b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.typeDefinition.getTypeIdentification())
                        .build(),
                addOwnerOrOwnerCandidate(ctx, state, result, maxExamples, ownedShadows),
                null, ctx.task, result);
        return ownedShadows;
    }

    private ResultHandler<ShadowType> addOwnerOrOwnerCandidate(TypeOperationContext ctx,
            OperationContext.StateHolder state, OperationResult result, int maxExamples,
            ArrayList<ShadowWithOwner> shadowWithOwners) {
        return (shadow, lResult) -> {
            try {
                final CorrelationDefinitionType correlationDef =
                        new ResourceCorrelationDefinitionProvider(ctx.resource, ctx.getTypeIdentification()).get();
                this.correlationService
                        .findLinkedOrCorrelatedFocus(shadow.asObjectable(), ctx.resource, ctx.typeDefinition,
                                correlationDef, ctx.task, result)
                        .ifPresent(focus -> {
                            shadowWithOwners.add(new ShadowWithOwner(shadow.asObjectable(), focus));
                            state.incrementProgress(result);
                        });
            } catch (TunnelException e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e.getCause(), shadow);
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, shadow);
            }
            return ctx.canRun() && shadowWithOwners.size() < maxExamples;
        };
    }

}
