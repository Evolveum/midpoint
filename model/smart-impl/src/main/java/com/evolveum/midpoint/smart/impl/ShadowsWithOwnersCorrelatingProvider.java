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

import org.jetbrains.annotations.NotNull;
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
        final ArrayList<ShadowWithOwner> ownedShadows = new ArrayList<>(maxExamples);
        final CorrelationDefinitionType correlationDef =
                new ResourceCorrelationDefinitionProvider(ctx.resource, ctx.getTypeIdentification()).get();
        ctx.b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.typeDefinition.getTypeIdentification())
                        .build(),
                addOwnerOrOwnerCandidate(correlationDef, ctx, state, maxExamples, ownedShadows),
                null, ctx.task, result);
        return ownedShadows;
    }

    private @NotNull ResultHandler<ShadowType> addOwnerOrOwnerCandidate(CorrelationDefinitionType correlationDef,
            TypeOperationContext ctx, OperationContext.StateHolder state, int maxExamples,
            ArrayList<ShadowWithOwner> shadowWithOwners) {
        return (shadow, lResult) -> {
            state.flushIfNeeded(lResult);
            try {
                this.correlationService
                        .findLinkedOrCorrelatedFocus(shadow.asObjectable(), ctx.resource, ctx.typeDefinition,
                                correlationDef, ctx.task, lResult)
                        .ifPresent(focus -> {
                            shadowWithOwners.add(new ShadowWithOwner(shadow.asObjectable(), focus));
                            state.incrementProgress(lResult);
                        });
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, shadow);
            }
            finally {
                lResult.computeStatusIfUnknown();
                lResult.setSummarizeSuccesses(true);
                lResult.summarize();
            }
            return ctx.canRun() && shadowWithOwners.size() < maxExamples;
        };
    }

}
