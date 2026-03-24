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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
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
public class OwnedShadowsProviderFromResource implements OwnedShadowsProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OwnedShadowsProviderFromResource.class);

    @Override
    public List<OwnedShadow> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            int maxExamples)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        var ownedShadows = new ArrayList<OwnedShadow>(maxExamples);
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
            ArrayList<OwnedShadow> ownedShadows) {
        return (object, lResult) -> {
            try {
                Optional.ofNullable(findLinkedOwner(ctx, object, lResult))
                        .or(() -> findOwnerCandidate(object.asObjectable(), ctx, lResult))
                        .ifPresent(focus -> {
                            ownedShadows.add(new OwnedShadow(object.asObjectable(), focus.asObjectable()));
                            state.incrementProgress(result);
                        });
            } catch (TunnelException e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e.getCause(), object);
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
            }
            return ctx.canRun() && ownedShadows.size() < maxExamples;
        };
    }

    @SuppressWarnings("unchecked")
    private static PrismObject<FocusType> findLinkedOwner(TypeOperationContext ctx, PrismObject<ShadowType> object,
            OperationResult lResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException {
        return (PrismObject<FocusType>) ctx.b.modelService.searchShadowOwner(object.getOid(), null, ctx.task, lResult);
    }

    private Optional<PrismObject<FocusType>> findOwnerCandidate(ShadowType shadow, TypeOperationContext ctx,
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
            return Optional.of(correlatedFocus);
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Shadow" + shadow + " is correlated with focus with oid " + ownerCandidateOid
                    + ", which does not exist.");
        } catch (SchemaException | ExpressionEvaluationException | SecurityViolationException | CommunicationException |
                 ConfigurationException e) {
            throw new TunnelException(e);
        }
    }

}
