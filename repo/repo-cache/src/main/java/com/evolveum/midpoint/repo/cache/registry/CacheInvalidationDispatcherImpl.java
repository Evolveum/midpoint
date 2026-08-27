/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.cache.invalidation.RepositoryCacheInvalidationDetails;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Dispatches cache-related events - mainly invalidation ones - to all relevant local-node listeners.
 *
 * Filters events based on {@link CacheInvalidationEventSpecification}.
 *
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 */
@Component
public class CacheInvalidationDispatcherImpl implements CacheInvalidationDispatcher, ClusterwideCacheInvalidationListener {

    private static final Trace LOGGER = TraceManager.getTrace(CacheInvalidationDispatcherImpl.class);

    @Autowired private ClusterwideCacheInvalidationDispatcher clusterwideDispatcher;

    private final List<CacheInvalidationListener> listeners = new ArrayList<>();

    @PostConstruct
    public void registerMyself() {
        clusterwideDispatcher.registerListener(this);
    }

    @PreDestroy
    public void unregisterMyself() {
        clusterwideDispatcher.unregisterListener(this);
    }

    @Override
    public synchronized void registerListener(CacheInvalidationListener listener) {
        LOGGER.debug("Registering listener {}", listener);
        if (listeners.contains(listener)) {
            LOGGER.warn("Registering listener {} which was already registered.", listener);
            return;
        }
        listeners.add(listener);
    }

    @Override
    public synchronized void unregisterListener(CacheInvalidationListener listener) {
        if (!listeners.contains(listener)) {
            LOGGER.warn("Unregistering listener {} which was already unregistered.", listener);
            return;
        }
        listeners.remove(listener);
    }

    @Override
    public <O extends ObjectType> void invalidate(
            Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context) {
        dispatchInvalidation(type, oid, context);
    }

    /**
     * Dispatches "cache entry/entries invalidation" event to all relevant local caches.
     *
     * @param type Type of object(s) to be invalidated. Null means 'all types' (implies oid is null as well).
     * @param oid Object(s) to be invalidated. Null means 'all objects of given type(s)'.
     * @param context Context of the invalidation request (optional).
     */
    private <O extends ObjectType> void dispatchInvalidation(
            Class<O> type, String oid, @Nullable CacheInvalidationContext context) {
        RepositoryOperationResult repoResult = null;
        if (context != null) {
            CacheInvalidationDetails details = context.getDetails();
            if (details instanceof RepositoryCacheInvalidationDetails repositoryCacheInvalidationDetails) {
                repoResult = repositoryCacheInvalidationDetails.getResult();
            }
        }

        for (var listener : listeners) {
            if (isInterested(listener.getEventSpecifications(), type, repoResult)) {
                listener.invalidate(type, oid, context);
            }
        }
    }

    private boolean isInterested(
            Collection<CacheInvalidationEventSpecification> eventSpecs,
            Class<? extends ObjectType> type,
            @Nullable RepositoryOperationResult result) {
        if (CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS == eventSpecs) {
            // Fast path for cache listeners interested in all events
            return true;
        }
        if (type == null) {
            // Type was null, means invalidate everything
            return true;
        }

        for (CacheInvalidationEventSpecification eventSpec : eventSpecs) {
            if (eventSpec.getObjectType().isAssignableFrom(type)) {
                LOGGER.trace("Listener interested in {}, repository result is {}", type, result);
                // Listener is interested in this type
                if (result == null) {
                    // FIXME: What to do here? this is caused by addDiagnosticInformation
                    // or when we received non Repository event
                    return true;
                }
                if (eventSpec.getChangeTypes().contains(result.getChangeType())) {
                    if (result instanceof ModifyObjectResult<?> modifyObjectResult) {
                        return isAnyPathAffected(eventSpec, modifyObjectResult);
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAnyPathAffected(CacheInvalidationEventSpecification eventSpec, ModifyObjectResult<?> result) {
        if (CacheInvalidationEventSpecification.ALL_PATHS == eventSpec.getPaths()) {
            return true;
        }
        if (result.isOverwrite()) {
            // MID-8167: Object was overwritten - in case of sqale or other repositories this is delete + add
            // so list of modifications paths may be incorrect or empty
            // we would rather assume that paths changed and emit event.
            return true;
        }
        for (ItemPath path : eventSpec.getPaths()) {
            for (ItemDelta<?, ?> modification : result.getModifications()) {
                if (modification.getPath().startsWith(path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
