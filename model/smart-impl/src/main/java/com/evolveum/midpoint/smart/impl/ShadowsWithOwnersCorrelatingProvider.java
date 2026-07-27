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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.ShadowWithOwner;
import com.evolveum.midpoint.smart.impl.shadowsampling.SamplingConfiguration;
import com.evolveum.midpoint.smart.impl.shadowsampling.ShadowSamplingService;
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
 * Implementation that fetches owned shadows using random sampling strategy.
 * First samples shadows randomly, then finds owners for the sampled shadows.
 */
@Component
class ShadowsWithOwnersCorrelatingProvider implements ShadowsWithOwnersProvider {
    private static final Trace LOGGER = TraceManager.getTrace(ShadowsWithOwnersCorrelatingProvider.class);
    private final CorrelationService correlationService;
    private final ShadowSamplingService shadowSamplingService;

    public ShadowsWithOwnersCorrelatingProvider(
            CorrelationService correlationService,
            ShadowSamplingService shadowSamplingService) {
        this.correlationService = correlationService;
        this.shadowSamplingService = shadowSamplingService;
    }

    @Override
    public List<ShadowWithOwner> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            SamplingConfiguration config)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        List<PrismObject<ShadowType>> sampledShadows = shadowSamplingService.sampleShadows(
                ctx.resource,
                ctx.typeDefinition,
                config,
                ctx.task,
                result);

        LOGGER.debug("Sampled {} shadows, now finding owners for them", sampledShadows.size());

        final ArrayList<ShadowWithOwner> ownedShadows = new ArrayList<>();
        final CorrelationDefinitionType correlationDef =
                new ResourceCorrelationDefinitionProvider(ctx.resource, ctx.getTypeIdentification()).get();

        for (PrismObject<ShadowType> shadow : sampledShadows) {
            if (!ctx.canRun()) {
                break;
            }

            OperationResult subResult = result.createSubresult("findOwnerForShadow");
            subResult.addParam("shadow", shadow.getOid());

            state.flushIfNeeded(subResult);
            try {
                correlationService
                        .findLinkedOrCorrelatedFocus(shadow.asObjectable(), ctx.resource, ctx.typeDefinition,
                                correlationDef, ctx.task, subResult)
                        .ifPresent(focus -> {
                            ownedShadows.add(new ShadowWithOwner(shadow.asObjectable(), focus));
                            state.incrementProgress(subResult);
                        });
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, shadow);
            } finally {
                subResult.computeStatusIfUnknown();
                subResult.setSummarizeSuccesses(true);
                subResult.summarize();
            }
        }

        LOGGER.info("Found {} shadows with owners out of {} sampled shadows",
                ownedShadows.size(), sampledShadows.size());

        return ownedShadows;
    }

}
