/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.invalidation;

import static com.evolveum.midpoint.repo.cache.RepositoryCache.CLASS_NAME_WITH_DOT;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.RepositoryOperationResult;
import com.evolveum.midpoint.repo.cache.global.GlobalCacheQueryValue;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import javax.xml.namespace.QName;

/**
 * Contains functionality related to cache entry invalidation.
 */
@Component
public class Invalidator {

    public static final Trace LOGGER = TraceManager.getTrace(Invalidator.class);

    private static final List<Class<?>> TYPES_ALWAYS_INVALIDATED_CLUSTERWIDE = Arrays.asList(
            SystemConfigurationType.class,
            FunctionLibraryType.class);

    @Autowired private GlobalQueryCache globalQueryCache;
    @Autowired private GlobalObjectCache globalObjectCache;
    @Autowired private GlobalVersionCache globalVersionCache;
    @Autowired CacheDispatcher cacheDispatcher;
    @Autowired MatchingRuleRegistry matchingRuleRegistry;

    private static final int MAX_LISTENERS = 1000;

    @NotNull private final Set<InvalidationEventListener> listeners = ConcurrentHashMap.newKeySet();

    // This is what is called from cache dispatcher (on local node with the full context; on remote nodes with reduced context)
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null) {
            globalObjectCache.clear();
            globalVersionCache.clear();
            globalQueryCache.clear();
        } else {
            globalObjectCache.remove(type, oid);
            globalVersionCache.remove(type, oid);
            if (ObjectType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                clearQueryResultsGlobally((Class<? extends ObjectType>) type, oid, context);
            }
        }
        if (!listeners.isEmpty()) {
            InvalidationEvent event = new InvalidationEvent(type, oid, context);
            listeners.forEach(listener -> listener.onInvalidationEvent(event));
        }
    }

    public <T extends ObjectType> void invalidateCacheEntries(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable RepositoryOperationResult additionalInfo,
            @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.subresult(CLASS_NAME_WITH_DOT + "invalidateCacheEntries")
                .setMinor()
                .addParam("type", type)
                .addParam("oid", oid)
                .addParam("additionalInfo", additionalInfo != null ? additionalInfo.getClass().getSimpleName() : "none")
                .build();
        try {
            LocalObjectCache localObjectCache = getLocalObjectCache();
            if (localObjectCache != null) {
                localObjectCache.remove(oid);
            }
            LocalVersionCache localVersionCache = getLocalVersionCache();
            if (localVersionCache != null) {
                localVersionCache.remove(oid);
            }
            LocalQueryCache localQueryCache = getLocalQueryCache();
            if (localQueryCache != null) {
                clearQueryResultsLocally(localQueryCache, type, oid, additionalInfo, matchingRuleRegistry);
            }
            var objectClassName = additionalInfo != null ? additionalInfo.getShadowObjectClassName() : null;
            boolean clusterwide = TYPES_ALWAYS_INVALIDATED_CLUSTERWIDE.contains(type)
                    || globalObjectCache.hasClusterwideInvalidationFor(type, objectClassName)
                    || globalVersionCache.hasClusterwideInvalidationFor(type, objectClassName)
                    || globalQueryCache.hasClusterwideInvalidationFor(type, objectClassName);
            cacheDispatcher.dispatchInvalidation(type, oid, clusterwide,
                    new CacheInvalidationContext(false, new RepositoryCacheInvalidationDetails(additionalInfo)));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t; // Really? We want the operation to proceed anyway. But OTOH we want to be sure devel team gets notified about this.
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> void clearQueryResultsLocally(LocalQueryCache cache, Class<T> type, String oid,
            Object additionalInfo, MatchingRuleRegistry matchingRuleRegistry) {
        // TODO implement more efficiently

        ChangeDescription change = ChangeDescription.getFrom(type, oid, additionalInfo, true);

        long start = System.currentTimeMillis();
        int all = 0;
        int removed = 0;
        var iterator = cache.getEntryIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            QueryKey<?> queryKey = entry.getKey();
            all++;
            if (change.mayAffect(queryKey, entry.getValue().getOidOnlyResult(), matchingRuleRegistry)) {
                LOGGER.trace("Removing (from local cache) query for type={}, change={}: {}", type, change, queryKey.getQuery());
                iterator.remove();
                removed++;
            }
        }
        LOGGER.trace("Removed (from local cache) {} (of {}) query result entries of type {} in {} ms", removed, all, type, System.currentTimeMillis() - start);
    }

    private <T extends ObjectType> void clearQueryResultsGlobally(
            @NotNull Class<T> type, String oid, CacheInvalidationContext context) {
        // TODO implement more efficiently

        // Safe invalidation means we evict queries without looking at details of the change.
        // TODO object class name
        boolean safeIfUnknown =
                context != null && !context.isFromRemoteNode()
                        || globalQueryCache.shouldDoSafeRemoteInvalidationFor(type, null);
        ChangeDescription change = ChangeDescription.getFrom(type, oid, context, safeIfUnknown);

        long start = System.currentTimeMillis();
        AtomicInteger all = new AtomicInteger(0);
        AtomicInteger removed = new AtomicInteger(0);

        var config = globalQueryCache.getConfiguration(type, getObjectClassName(context));
        if (config == null || !config.supportsCaching()) {
            return; // TODO what about different settings per task?
        }

        // All ancestors, descendants, and the type itself. We have to check & remove queries issued against all of them.
        var relevantTypes = getAllRelevantTypes(type);

        relevantTypes.forEach(relevantType ->
                globalQueryCache.deleteMatching(relevantType, entry -> {
                    var singleTypeQueryKey = entry.getKey();
                    var queryKey = singleTypeQueryKey.toQueryKey(relevantType);
                    GlobalCacheQueryValue value = entry.getValue();
                    all.incrementAndGet();
                    if (change.mayAffect(queryKey, value.getOidOnlyResult(), matchingRuleRegistry)) {
                        LOGGER.trace("Removing (from global cache) query for type={}, change={}: {}",
                                relevantType, change, singleTypeQueryKey.getQuery());
                        removed.incrementAndGet();
                        return true;
                    } else {
                        return false;
                    }
                }));

        LOGGER.trace("Removed (from global cache) {} (of {}) query result entries of type {} in {} ms",
                removed, all, type, System.currentTimeMillis() - start);
    }

    private @Nullable QName getObjectClassName(CacheInvalidationContext context) {
        if (context == null) {
            return null;
        }
        if (!(context.getDetails() instanceof RepositoryCacheInvalidationDetails repositoryCacheInvalidationDetails)) {
            return null;
        }
        var result = repositoryCacheInvalidationDetails.getResult();
        return result != null ? result.getShadowObjectClassName() : null;
    }

    private static <T extends ObjectType> @NotNull List<Class<ObjectType>> getAllRelevantTypes(Class<T> type) {
        var typeRecord = ObjectTypes.getObjectType(type);
        var relevantTypeRecords = new HashSet<>(typeRecord.thisAndSupertypes());
        relevantTypeRecords.addAll(typeRecord.subtypes());
        return relevantTypeRecords.stream()
                .map(ObjectTypes::getClassDefinition)
                .toList();
    }

    public void registerInvalidationEventsListener(InvalidationEventListener listener) {
        if (listeners.size() >= MAX_LISTENERS) {
            throw new IllegalStateException("Maximum number of invalidation events listeners was reached: " + MAX_LISTENERS);
        }
        boolean added = listeners.add(listener);
        assert added;
    }

    public void unregisterInvalidationEventsListener(InvalidationEventListener listener) {
        boolean removed = listeners.remove(listener);
        assert removed;
    }

    /**
     * Checks if the search result is still valid, even after specified invalidation events came.
     */
    public <T extends ObjectType> boolean isSearchResultValid(
            QueryKey<T> key, SearchResultList<String> list, List<InvalidationEvent> invalidationEvents) {
        for (InvalidationEvent event : invalidationEvents) {
            ChangeDescription change = ChangeDescription.getFrom(event);
            if (change != null && change.mayAffect(key, list, matchingRuleRegistry)) {
                LOGGER.debug("Search result was invalidated by change: {}", change);
                return false;
            }
        }
        return true;
    }
}
