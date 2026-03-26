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
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.impl.correlation.ResourceCorrelationDefinitionProvider;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Default implementation that fetches owned shadows via ModelService from the resource.
 */
@Component
public class ShadowsWithOwnersCorrelatingProvider implements ShadowsWithOwnersProvider {
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

    private @NonNull ResultHandler<ShadowType> addOwnerOrOwnerCandidate(TypeOperationContext ctx,
            OperationContext.StateHolder state, OperationResult result, int maxExamples,
            ArrayList<ShadowWithOwner> shadowWithOwners) {
        return (shadow, lResult) -> {
            try {
                findLinkedOwner(ctx, shadow, lResult)
                        .or(() -> findOwnerCandidate(shadow.asObjectable(), ctx, lResult))
                        .or(() -> correlateShadow(ctx, shadow, result))
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

    private static Optional<FocusType> findLinkedOwner(TypeOperationContext ctx, PrismObject<ShadowType> object,
            OperationResult lResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException {
        return Optional.ofNullable(ctx.b.modelService.searchShadowOwner(object.getOid(), null, ctx.task, lResult))
                .map(owner -> owner.asObjectable());
    }

    private Optional<FocusType> findOwnerCandidate(ShadowType shadow, TypeOperationContext ctx,
            OperationResult result) {
        final String ownerCandidateOid = Optional.ofNullable(shadow.getCorrelation())
                .map(ShadowCorrelationStateType::getResultingOwner)
                .map(AbstractReferencable::getOid)
                .orElse(null);
        if (ownerCandidateOid == null) {
            return Optional.empty();
        }

        try {
            final PrismObject<FocusType> correlatedFocus = ctx.b.modelService.getObject(FocusType.class,
                    ownerCandidateOid, null, ctx.task, result);
            return Optional.of(correlatedFocus.asObjectable());
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Shadow" + shadow + " is correlated with focus with oid " + ownerCandidateOid
                    + ", which does not exist.");
        } catch (SchemaException | ExpressionEvaluationException | SecurityViolationException | CommunicationException |
                 ConfigurationException e) {
            throw new TunnelException(e);
        }
    }

    private Optional<FocusType> correlateShadow(TypeOperationContext ctx, PrismObject<ShadowType> shadow,
            OperationResult result) {
        try {
            final CorrelationDefinitionType correlationDef =
                    new ResourceCorrelationDefinitionProvider(ctx.resource, ctx.getTypeIdentification()).get();
            final CompleteCorrelationResult correlationResult = this.correlationService.correlate(shadow.asObjectable(),
                    ctx.resource, ctx.typeDefinition, correlationDef, ctx.task, result);
            return Optional.ofNullable(correlationResult.getOwner())
                    .filter(owner -> owner instanceof FocusType)
                    .map(owner -> (FocusType) owner);
        } catch (SchemaException | ExpressionEvaluationException | CommunicationException | SecurityViolationException |
                 ConfigurationException | ObjectNotFoundException e) {
            throw new TunnelException(e);
        }
    }

}
