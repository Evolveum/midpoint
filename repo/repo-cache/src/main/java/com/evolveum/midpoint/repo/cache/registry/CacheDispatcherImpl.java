/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheInvalidationDetails;
import com.evolveum.midpoint.repo.api.CacheInvalidationEventSpecification;
import com.evolveum.midpoint.repo.api.CacheInvalidationListener;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.RepositoryOperationResult;
import com.evolveum.midpoint.repo.cache.invalidation.RepositoryCacheInvalidationDetails;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Dispatches cache-related events - mainly invalidation ones - to all relevant listeners:
 * {@link CacheRegistry} (grouping local caches) and ClusterCacheListener (for
 * inter-node distribution).
 * <p>
 * Could be reworked in the future.
 * <p>
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 */
@Component
public class CacheDispatcherImpl implements CacheDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(CacheDispatcherImpl.class);

    private List<CacheInvalidationListener> cacheListeners = new ArrayList<>();



    @Override
    public synchronized void registerCacheInvalidationListener(CacheInvalidationListener cacheListener) {
        LOGGER.debug("Registering listener {}", cacheListener);
        if (cacheListeners.contains(cacheListener)) {
            LOGGER.warn("Registering listener {} which was already registered.", cacheListener);
            return;
        }
        cacheListeners.add(cacheListener);
    }

    @Override
    public synchronized void unregisterCacheInvalidationListener(CacheInvalidationListener cacheListener) {
        if (!cacheListeners.contains(cacheListener)) {
            LOGGER.warn("Unregistering listener {} which was already unregistered.", cacheListener);
            return;
        }
        cacheListeners.remove(cacheListener);
    }

    @Override
    public <O extends ObjectType> void dispatchInvalidation(Class<O> type, String oid, boolean clusterwide,
            @Nullable CacheInvalidationContext context) {
        RepositoryOperationResult repoResult = null;
        if (context != null) {
            CacheInvalidationDetails details = context.getDetails();
            if (details instanceof RepositoryCacheInvalidationDetails repositoryCacheInvalidationDetails) {
                repoResult = repositoryCacheInvalidationDetails.getResult();
            }
        }

        for (CacheInvalidationListener listener : cacheListeners) {
            if (isInterested(listener.getEventSpecifications(), type, context, repoResult)) {
                listener.invalidate(type, oid, clusterwide, context);
            }
        }
    }

    private boolean isInterested(Collection<CacheInvalidationEventSpecification> eventSpecs, Class<? extends ObjectType> type,
            @Nullable CacheInvalidationContext context, @Nullable RepositoryOperationResult result) {
        if (CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS == eventSpecs) {
            // Fast path for cache listeners interested in all events
            return true;
        }
        if (type == null) {
            // Type was null, means invalidate everything
            return true;
        }

        for(CacheInvalidationEventSpecification eventSpec : eventSpecs) {
            if (eventSpec.getObjectType().isAssignableFrom(type)) {
                LOGGER.trace("Listener interested in {}, repository result is {}", type, result);
                // Listener is interested in this type
                if (result == null) {
                    // FIXME: What to do here? this is caused by addDiagnosticInformation
                    // or when we received non Repository event
                    return true;
                }
                if (eventSpec.getChangeTypes().contains(result.getChangeType())) {
                    if (result instanceof ModifyObjectResult) {
                        return isAnyPathAffected(eventSpec, (ModifyObjectResult<?>) result);
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
