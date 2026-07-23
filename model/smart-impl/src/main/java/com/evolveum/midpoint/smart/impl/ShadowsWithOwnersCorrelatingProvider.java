/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.impl.correlation.ResourceCorrelationDefinitionProvider;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.ShadowWithOwner;
import com.evolveum.midpoint.smart.impl.mappings.ShadowsWithOwnerSampleResult;
import com.evolveum.midpoint.smart.impl.shadowsampling.MappingSampleResult;
import com.evolveum.midpoint.smart.impl.shadowsampling.ObjectsSamplerProvider;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implementation that fetches owned shadows using random sampling strategy.
 * First samples shadows randomly, then finds owners for the sampled shadows.
 */
@Component
class ShadowsWithOwnersCorrelatingProvider implements ShadowsWithOwnersProvider {
    private static final Trace LOGGER = TraceManager.getTrace(ShadowsWithOwnersCorrelatingProvider.class);
    private final CorrelationService correlationService;
    private final ObjectsSamplerProvider samplerProvider;

    public ShadowsWithOwnersCorrelatingProvider(
            CorrelationService correlationService,
            ObjectsSamplerProvider samplerProvider) {
        this.correlationService = correlationService;
        this.samplerProvider = samplerProvider;
    }

    @Override
    public ShadowsWithOwnerSampleResult fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, SubscriptionComplianceException {

        final CorrelationDefinitionType correlationDef =
                new ResourceCorrelationDefinitionProvider(ctx.resource).definitionFor(ctx.getTypeIdentification());

        // Set expected progress based on total sample size (llm + validation)
        int expectedSampleSize = samplerProvider.getExpectedMappingSampleSize(ctx.typeDefinition);
        state.setExpectedProgress(expectedSampleSize);

        // Predicate only checks if shadow has owner (doesn't cache)
        MappingSampleResult sampledResult = samplerProvider.getMappingSampler(
                ctx.typeDefinition, ctx.resource).sample(
                shadow -> hasOwner(shadow, ctx, correlationDef, state, result),
                ctx.task,
                result);

        // Process LLM samples
        final ArrayList<ShadowWithOwner> llmOwnedShadows = new ArrayList<>();
        for (PrismObject<ShadowType> shadow : sampledResult.llmSamples()) {
            OperationResult subResult = result.createSubresult("findOwnerForShadow");
            Optional<FocusType> ownerOptional = findOwner(shadow, ctx, correlationDef, subResult);
            ownerOptional.ifPresent(focusType -> llmOwnedShadows.add(new ShadowWithOwner(shadow.asObjectable(), focusType)));
        }

        // Process validation samples
        final ArrayList<ShadowWithOwner> validationOwnedShadows = new ArrayList<>();
        for (PrismObject<ShadowType> shadow : sampledResult.validationSamples()) {
            OperationResult subResult = result.createSubresult("findOwnerForShadow");
            Optional<FocusType> ownerOptional = findOwner(shadow, ctx, correlationDef, subResult);
            ownerOptional.ifPresent(focusType -> validationOwnedShadows.add(new ShadowWithOwner(shadow.asObjectable(), focusType)));
        }

        LOGGER.info("Sampled {} shadows with owners ({} for LLM, {} for validation)",
                llmOwnedShadows.size() + validationOwnedShadows.size(), llmOwnedShadows.size(), validationOwnedShadows.size());

        return new ShadowsWithOwnerSampleResult(llmOwnedShadows, validationOwnedShadows);
    }

    /**
     * Predicate method that checks if shadow has an owner.
     * Returns true if owner exists, false otherwise.
     * Does not fetch or cache the actual owner object.
     */
    private boolean hasOwner(
            PrismObject<ShadowType> shadow,
            TypeOperationContext ctx,
            CorrelationDefinitionType correlationDef,
            OperationContext.StateHolder state,
            OperationResult parentResult) {

        state.flushIfNeeded(parentResult);

        OperationResult subResult = parentResult.createSubresult("checkIfShadowHasOwner");
        Optional<FocusType> ownerOptional = findOwner(shadow, ctx, correlationDef, subResult);
        boolean hasOwner = ownerOptional.isPresent();

        // Increment progress here where the actual expensive work happens
        if (hasOwner) {
            state.incrementProgress(subResult);
        }

        return hasOwner;
    }

    /**
     * Finds owner for the given shadow using correlation.
     * Returns Optional with owner if found, empty Optional otherwise.
     */
    private Optional<FocusType> findOwner(
            PrismObject<ShadowType> shadow,
            TypeOperationContext ctx,
            CorrelationDefinitionType correlationDef,
            OperationResult result) {
        if (!ctx.canRun()) {
            return Optional.empty();
        }
        result.addParam("shadow", shadow.getOid());
        try {
            return correlationService.findLinkedOrCorrelatedFocus(
                    shadow.asObjectable(),
                    ctx.resource,
                    ctx.typeDefinition,
                    correlationDef,
                    ctx.task,
                    result);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, shadow);
            return Optional.empty();
        } finally {
            result.computeStatusIfUnknown();
            result.setSummarizeSuccesses(true);
            result.summarize();
        }
    }
}
