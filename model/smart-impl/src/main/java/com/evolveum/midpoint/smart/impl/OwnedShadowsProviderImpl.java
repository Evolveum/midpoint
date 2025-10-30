/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Default implementation that fetches owned shadows via ModelService from the resource.
 */
@Component
public class OwnedShadowsProviderImpl implements OwnedShadowsProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OwnedShadowsProviderImpl.class);

    @Override
    public Collection<OwnedShadow> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            int maxExamples)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        // Maybe we should search the repository instead. The argument for going to the resource is to get some data even
        // if they are not in the repository yet. But this is not a good argument, because if we get an account from the resource,
        // it won't have the owner anyway.
        var ownedShadows = new ArrayList<OwnedShadow>(maxExamples);
        ctx.b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.typeDefinition.getTypeIdentification())
                        .build(),
                (object, lResult) -> {
                    try {
                        var owner = ctx.b.modelService.searchShadowOwner(object.getOid(), null, ctx.task, lResult);
                        if (owner != null) {
                            ownedShadows.add(new OwnedShadow(object.asObjectable(), owner.asObjectable()));
                            state.incrementProgress(result);
                        }
                    } catch (Exception e) {
                        LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
                    }
                    return ctx.canRun() && ownedShadows.size() < maxExamples;
                },
                null, ctx.task, result);
        return ownedShadows;
    }
}
