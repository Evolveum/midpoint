/*
 * Copyright (C) 2019-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr.triggerSetter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.cache.invalidation.RepositoryCacheInvalidationDetails;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Global state for optimizing trigger creators for the given midPoint node.
 */
@Component
public class TriggerCreatorGlobalState implements CacheInvalidationListener, CacheDiagnostics {

    private static final Trace LOGGER = TraceManager.getTrace(TriggerCreatorGlobalState.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(TriggerCreatorGlobalState.class.getName() + ".content");

    private final AtomicLong lastExpirationCleanup = new AtomicLong(0L);

    private static final long EXPIRATION_INTERVAL = 10000L;

    @Autowired private CacheDiagnosticsService cacheDiagnosticsService;
    @Autowired private CacheInvalidationDispatcher cacheInvalidationDispatcher;

    private final Map<TriggerHolderSpecification, CreatedTrigger> state = new ConcurrentHashMap<>();

    synchronized CreatedTrigger getLastCreatedTrigger(TriggerHolderSpecification key) {
        return state.get(key);
    }

    synchronized void recordCreatedTrigger(TriggerHolderSpecification key, CreatedTrigger trigger) {
        state.put(key, trigger);
    }

    @Override
    public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS; // TODO narrow the scope
    }

    @Override
    public synchronized <O extends ObjectType> void invalidate(Class<O> type, String oid, CacheInvalidationContext context) {
        if (oid != null) {
            // We are interested in object deletion events; just to take care of situations when an object is deleted and
            // a new object (of the same name) is created immediately.
            boolean cleanupSpecificEntries = context != null
                    && context.getDetails() instanceof RepositoryCacheInvalidationDetails details
                    && details.getResult() instanceof DeleteObjectResult;

            // We want to remove expired entries in regular intervals. Invalidation event arrival is quite good approximation.
            // However, there's EXPIRATION_INTERVAL present to avoid going through the entries at each invalidation event.
            // (But if we scan the entries for another reason, we take care of expired ones regardless of expiration interval.)
            boolean cleanupExpiredEntries = System.currentTimeMillis() - lastExpirationCleanup.get() >= EXPIRATION_INTERVAL;

            if (cleanupSpecificEntries || cleanupExpiredEntries) {
                int removedMatching = 0;
                int removedExpired = 0;
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<TriggerHolderSpecification, CreatedTrigger>> iterator = state.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<TriggerHolderSpecification, CreatedTrigger> entry = iterator.next();
                    String entryOid = entry.getValue().getHolderOid();
                    if (cleanupSpecificEntries && entryOid.equals(oid)) {
                        iterator.remove();
                        removedMatching++;
                    } else if (entry.getValue().getFireTime() < now) {
                        iterator.remove();
                        removedExpired++;
                    }
                }
                LOGGER.trace("Removed {} entries corresponding to OID={} and {} expired entries",
                        removedMatching, oid, removedExpired);
                lastExpirationCleanup.set(System.currentTimeMillis());
            }
        } else {
            // just an approximation
            int size = state.size();
            state.clear();
            LOGGER.trace("Removed the whole state ({} entries)", size);
            lastExpirationCleanup.set(System.currentTimeMillis());
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(
                new SingleCacheStateInformationType()
                        .name(TriggerCreatorGlobalState.class.getName())
                        .size(state.size())
        );
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            state.forEach((k, v) -> LOGGER_CONTENT.info("Cached trigger creation: {}: {}", k, v));
        }
    }

    @PostConstruct
    public void register() {
        cacheDiagnosticsService.registerCache(this);
        cacheInvalidationDispatcher.registerListener(this);
    }

    @PreDestroy
    public void unregister() {
        cacheDiagnosticsService.unregisterCache(this);
        cacheInvalidationDispatcher.unregisterListener(this);
    }
}
