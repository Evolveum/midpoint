/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.log;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.invalidation.InvalidationEvent;
import com.evolveum.midpoint.repo.cache.invalidation.InvalidationEventListener;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Caches the objects found within searchObjectsIterative call.
 */
final class CachingResultHandler<T extends ObjectType> implements ResultHandler<T>, InvalidationEventListener {

    private final ResultHandler<T> originalHandler;
    private final boolean queryCacheable;
    private final boolean readOnly;
    private final long started;
    private final Class<T> type;
    private final CacheUpdater cacheUpdater;

    // Guarded by itself
    private final List<InvalidationEvent> invalidationEvents = new ArrayList<>();

    private boolean overflown; // too many objects for the result to be cached
    private boolean wasInterrupted; // interrupted by handler => result cannot be cached
    private final List<PrismObject<T>> objects = new ArrayList<>();

    CachingResultHandler(ResultHandler<T> handler, boolean queryCacheable, boolean readOnly, long started,
            Class<T> type, CacheUpdater cacheUpdater) {
        this.originalHandler = handler;
        this.queryCacheable = queryCacheable;
        this.readOnly = readOnly;
        this.started = started;
        this.type = type;
        this.cacheUpdater = cacheUpdater;
    }

    @Override
    public boolean handle(PrismObject<T> object, OperationResult parentResult) {

        // We have to store loaded object to caches _before_ executing the original handler,
        // not after that. The reason is that the handler can change these objects. (See MID-6250.)
        cacheUpdater.storeLoadedObjectToAll(object, readOnly, System.currentTimeMillis() - started);

        // We also collect loaded objects to store them in query cache later - if possible.
        if (queryCacheable && !overflown) {
            if (objects.size() < SearchOpHandler.QUERY_RESULT_SIZE_LIMIT) {
                objects.add(object.isImmutable() ? object : object.createImmutableClone());
            } else {
                CachePerformanceCollector.INSTANCE.registerOverSizedQuery(type);
                overflown = true;
            }
        }

        boolean cont = originalHandler.handle(object, parentResult);
        if (!cont) {
            wasInterrupted = true;
        }
        return cont;
    }

    /**
     * @return Search result to be stored into the cache; or null if the result is not suitable for caching.
     */
    SearchResultList<PrismObject<T>> getCacheableSearchResult(SearchResultMetadata metadataToImplant) {
        if (queryCacheable && !overflown && !wasInterrupted) {
            long age = System.currentTimeMillis() - started;
            if (age < CacheUpdater.DATA_STALENESS_LIMIT) {
                return new SearchResultList<>(objects, metadataToImplant);
            } else {
                CachePerformanceCollector.INSTANCE.registerSkippedStaleData(type);
                log("Not caching stale search result with {} object(s) (age = {} ms)", false, objects.size(), age);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void onInvalidationEvent(InvalidationEvent event) {
        synchronized (invalidationEvents) {
            if (queryCacheable && !overflown) {
                invalidationEvents.add(event);
            }
        }
    }

    /**
     * @return List of invalidation events gathered during execution of searchObjectsIterative.
     */
    List<InvalidationEvent> getInvalidationEvents() {
        // Note that in theory we could return the list directly, because when called, this object is already
        // unregistered from the Invalidator (so it will not be altered any more). But let's be more careful
        // than strictly needed.
        synchronized (invalidationEvents) {
            return new ArrayList<>(invalidationEvents);
        }
    }
}
