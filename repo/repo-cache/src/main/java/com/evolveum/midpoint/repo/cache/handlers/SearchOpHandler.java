/*
 * Copyright (C) 2020-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.RepositoryCache.CLASS_NAME_WITH_DOT;
import static com.evolveum.midpoint.repo.cache.handlers.CacheUpdater.toImmutableOidList;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalObjectCache;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpEnd;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpStart;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.repo.cache.values.CachedQueryValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.cache.other.MonitoringUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Handler for `searchObjects` and `searchObjectsIterative` operations.
 */
@Component
public class SearchOpHandler extends CachedOpHandler {

    private static final String SEARCH_OBJECTS = "searchObjects";
    private static final String SEARCH_OBJECTS_ITERATIVE = "searchObjectsIterative";

    private static final String OP_SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + "searchContainers";
    private static final String OP_COUNT_CONTAINERS = CLASS_NAME_WITH_DOT + "countContainers";
    private static final String OP_COUNT_OBJECTS = CLASS_NAME_WITH_DOT + "countObjects";
    private static final String OP_SEARCH_REFERENCES = CLASS_NAME_WITH_DOT + "searchReferences";
    private static final String OP_COUNT_REFERENCES = CLASS_NAME_WITH_DOT + "countReferences";
    private static final String OP_SEARCH_REFERENCES_ITERATIVE = CLASS_NAME_WITH_DOT + "searchReferencesIterative";

    private static final String OP_SEARCH_CONTAINERS_ITERATIVE = CLASS_NAME_WITH_DOT + "searchContainersIterative";

    /**
     * Queries resulting in more objects will not be cached "as such" - although individual objects/versions can be cached.
     */
    public static final int QUERY_RESULT_SIZE_LIMIT = 100;

    private static final String OP_ITERATE_OVER_QUERY_RESULT = RepositoryCache.class.getName() + ".iterateOverQueryResult";

    public <T extends ObjectType> @NotNull SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException {
        var exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS);
        SearchResultList<PrismObject<T>> returnValue = null;
        try {
            returnValue = doSearchObjects(exec); // it is a separate method to allow recording the result in the "finally" block
        } catch (Throwable t) {
            exec.result.recordException(t);
            throw t;
        } finally {
            exec.recordSearchResult(returnValue);
            exec.result.close();
        }
        return returnValue;
    }

    /** Returns directly returnable value (mutable vs immutable). */
    private <T extends ObjectType> SearchResultList<PrismObject<T>> doSearchObjects(SearchOpExecution<T> exec)
            throws SchemaException {
        if (exec.cacheUseMode.canNeverUseCachedData()) {
            exec.reportLocalAndGlobalPass();
        } else {
            var fromCache = tryCaches(exec);
            if (fromCache != null) {
                return exec.toReturnValueFromImmutable(fromCache);
            }
        }
        return executeAndCacheSearch(exec);
    }

    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type,
            ObjectQuery query,
            ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential,
            OperationResult parentResult) throws SchemaException {
        var exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS_ITERATIVE);
        var reportingHandler = new RecordingResultHandler<>(handler, exec);
        try {
            return doSearchObjectsIterative(exec, reportingHandler, strictlySequential);
        } catch (Throwable t) {
            exec.result.recordException(t);
            throw t;
        } finally {
            reportingHandler.recordResult();
            exec.result.close();
        }
    }

    private <T extends ObjectType> @Nullable SearchResultMetadata doSearchObjectsIterative(
            SearchOpExecution<T> exec, RecordingResultHandler<T> reportingHandler, boolean strictlySequential)
            throws SchemaException {
        if (exec.cacheUseMode.canNeverUseCachedData()) {
            exec.reportLocalAndGlobalPass();
        } else {
            var fromCache = tryCaches(exec);
            if (fromCache != null) {
                return iterateOverImmutableQueryResult(exec, fromCache, reportingHandler);
            }
        }
        return executeAndCacheSearchIterative(exec, reportingHandler, strictlySequential);
    }

    /** Returned values are immutable. */
    private <T extends ObjectType> SearchResultList<PrismObject<T>> tryCaches(SearchOpExecution<T> exec) {
        var fromLocalCache = tryLocalCache(exec);
        if (fromLocalCache != null) {
            return fromLocalCache;
        } else {
            return tryGlobalCache(exec);
        }
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> tryLocalCache(SearchOpExecution<T> exec) {

        if (!exec.localInfo.available) {
            exec.reportLocalNotAvailable();
            return null;
        }

        if (!exec.localInfo.supports) {
            exec.reportLocalPass();
            return null;
        }

        var cachedValue = exec.localInfo.getCache().get(exec.queryKey);
        var cachedResult = expandCachedOidList(exec, cachedValue);
        if (cachedResult == null) {
            exec.reportLocalMiss();
            return null;
        }

        exec.reportLocalHit();
        return cachedResult;
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> tryGlobalCache(SearchOpExecution<T> exec) {

        if (!exec.globalInfo.available) {
            exec.reportGlobalNotAvailable();
            return null;
        }

        if (!exec.globalInfo.supports) {
            exec.reportGlobalPass();
            return null;
        }

        var cachedValue = globalQueryCache.get(exec.queryKey);
        var cachedResult = expandCachedOidList(exec, cachedValue);
        if (cachedResult == null) {
            exec.reportGlobalMiss();
            return null;
        }

        exec.reportGlobalHit();
        cacheUpdater.storeImmutableSearchResultToAllLocal(exec, cachedResult, toImmutableOidList(cachedResult));
        return cachedResult;
    }

    /**
     * Expanding OIDs to full objects (from local/global object cache). Ignoring version checks for global object cache (for now).
     *
     * Returns `null` if the result cannot be constructed, e.g. because some objects are not in the cache.
     */
    private <T extends ObjectType> SearchResultList<PrismObject<T>> expandCachedOidList(
            SearchOpExecution<T> exec, CachedQueryValue cachedValue) {
        if (cachedValue == null) {
            return null;
        }
        var objects = new ArrayList<PrismObject<T>>();
        SearchResultList<String> oidOnlyResult = cachedValue.getOidOnlyResult();
        for (String oid : oidOnlyResult) {
            CachedObject<T> object = getFromObjectCaches(oid);
            if (object == null) {
                return null;
            }
            if (exec.cacheUseMode.canUseCachedDataOnlyIfComplete() && !object.complete) {
                return null;
            }
            objects.add(object.object);
        }
        var list = new SearchResultList<>(objects, oidOnlyResult.getMetadata());
        list.freeze(); // objects are already frozen, so this should be relatively cheap (just the list itself and the metadata)
        return list;
    }

    private <T extends ObjectType> CachedObject<T> getFromObjectCaches(@NotNull String oid) {
        var local = getLocalObjectCache();
        if (local != null && local.isAvailable()) {
            var entry = local.get(oid);
            if (entry != null) {
                return CachedObject.of(entry.getObject(), entry.isComplete());
            }
        }
        if (globalObjectCache.isAvailable()) {
            var entry = globalObjectCache.get(oid);
            if (entry != null) {
                return CachedObject.of(entry.getObject(), entry.isComplete());
            }
        }
        return null;
    }

    private <T extends ObjectType> SearchOpExecution<T> initializeExecution(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult, String opName)
            throws SchemaException {
        OperationResult result = parentResult.subresult(CLASS_NAME_WITH_DOT + opName)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();

        TracingLevelType level = result.getTracingLevel(RepositorySearchObjectsTraceType.class);
        RepositorySearchObjectsTraceType trace;
        if (isAtLeastMinimal(level)) {
            trace = new RepositorySearchObjectsTraceType()
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .query(prismContext.getQueryConverter().createQueryType(query))
                    .options(String.valueOf(options));
            result.addTrace(trace);
        } else {
            trace = null;
        }
        CacheSetAccessInfo<T> caches = cacheSetAccessInfoFactory.determine(type);
        CacheUseMode cacheUseMode = CacheUseMode.determine(options, type);
        return new SearchOpExecution<>(type, options, result, query, trace, level, caches, cacheUseMode, opName);
    }

    /** Returns directly returnable value (mutable vs immutable). */
    private <T extends ObjectType> SearchResultList<PrismObject<T>> executeAndCacheSearch(SearchOpExecution<T> exec)
            throws SchemaException {
        var key = exec.queryKey;
        try {
            var objects = searchObjectsInternal(exec);
            return cacheUpdater.storeSearchResultToAll(exec, objects);
        } catch (Exception ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    private <T extends ObjectType> SearchResultMetadata executeAndCacheSearchIterative(
            SearchOpExecution<T> exec, RecordingResultHandler<T> recordingHandler, boolean strictlySequential)
            throws SchemaException {
        var key = exec.queryKey;
        try {
            boolean canUpdateCaches = exec.cacheUseMode.canUpdateCache();
            var canUpdateQueryCache =
                    canUpdateCaches && (exec.globalInfo.effectivelySupports() || exec.localInfo.effectivelySupports());

            var innerHandler = canUpdateCaches ?
                    new CachingResultHandler<>(exec, recordingHandler, canUpdateQueryCache, cacheUpdater) :
                    recordingHandler;

            SearchResultMetadata metadata;
            try {
                if (canUpdateQueryCache) {
                    invalidator.registerInvalidationEventsListener((CachingResultHandler<T>) innerHandler);
                }
                metadata = searchObjectsIterativeInternal(exec, innerHandler, strictlySequential);
            } finally {
                if (canUpdateQueryCache) {
                    invalidator.unregisterInvalidationEventsListener((CachingResultHandler<T>) innerHandler);
                }
            }

            if (canUpdateQueryCache) {
                var cachingResultsHandler = (CachingResultHandler<T>) innerHandler;
                var searchResultOidList = cachingResultsHandler.getCacheableSearchResult(metadata);
                if (searchResultOidList != null
                        && invalidator.isSearchResultValid(key, searchResultOidList, cachingResultsHandler.getInvalidationEvents())) {
                    cacheUpdater.storeImmutableSearchResultToQueryLocal(key, searchResultOidList, exec.cachesInfo);
                    cacheUpdater.storeImmutableSearchResultToQueryGlobal(key, searchResultOidList, exec.cachesInfo);
                }
            }
            return metadata;
        } catch (SchemaException ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    @NotNull
    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsInternal(SearchOpExecution<T> exec)
            throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchObjects(exec.type, exec.query, exec.options, exec.result);
        } catch (Exception e) {
            globalQueryCache.remove(exec.queryKey);
            throw e;
        } finally {
            repoOpEnd(startTime);
        }
    }

    private <T extends ObjectType> SearchResultMetadata searchObjectsIterativeInternal(
            SearchOpExecution<T> exec, ResultHandler<T> handler, boolean strictlySequential) throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchObjectsIterative(
                    exec.type, exec.query, handler, exec.options, strictlySequential, exec.result);
        } finally {
            repoOpEnd(startTime);
        }
    }

    private <T extends ObjectType> SearchResultMetadata iterateOverImmutableQueryResult(
            SearchOpExecution<T> exec, SearchResultList<PrismObject<T>> immutableList, ResultHandler<T> handler) {
        OperationResult result = exec.result.subresult(OP_ITERATE_OVER_QUERY_RESULT)
                .setMinor()
                .addParam("objects", immutableList.size())
                .addArbitraryObjectAsParam("handler", handler)
                .build();
        try {
            for (PrismObject<T> immutableObject : immutableList) {
                immutableObject.checkImmutable();
                PrismObject<T> objectToHandle = exec.readOnly ? immutableObject : immutableObject.clone();
                if (!handler.handle(objectToHandle, result)) {
                    break;
                }
            }
            // todo Should be metadata influenced by the number of handler executions?
            //   ...and is it correct to return cached metadata at all?
            return immutableList.getMetadata() != null ? immutableList.getMetadata().clone() : null;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public @NotNull <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.subresult(OP_SEARCH_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectAsParam("options", options)
                .build();
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchContainers(type, query, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_COUNT_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();
        MonitoringUtil.log("Cache: PASS countContainers ({})", false, type.getSimpleName());
        Long startTime = repoOpStart();
        try {
            return repositoryService.countContainers(type, query, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public SearchResultList<ObjectReferenceType> searchReferences(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_SEARCH_REFERENCES)
                .addParam("query", query)
                .addArbitraryObjectAsParam("options", options)
                .build();
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchReferences(query, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public int countReferences(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_COUNT_REFERENCES)
                .addParam("query", query)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();
        MonitoringUtil.log("Cache: PASS countReferences ({})", false, ObjectReferenceType.class.getSimpleName());
        Long startTime = repoOpStart();
        try {
            return repositoryService.countReferences(query, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public SearchResultMetadata searchReferencesIterative(
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_SEARCH_REFERENCES_ITERATIVE)
                .addParam("query", query)
                .addArbitraryObjectAsParam("options", options)
                .build();
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchReferencesIterative(query, handler, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public <T extends Containerable> SearchResultMetadata searchContainersIterative(
            @NotNull  Class<T> type,
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<T> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_SEARCH_CONTAINERS_ITERATIVE)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectAsParam("options", options)
                .build();
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchContainersIterative(type, query, handler, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        // TODO use cached query result if applicable
        OperationResult result = parentResult.subresult(OP_COUNT_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();
        MonitoringUtil.log("Cache: PASS countObjects ({})", false, type.getSimpleName());
        Long startTime = repoOpStart();
        try {
            return repositoryService.countObjects(type, query, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            repoOpEnd(startTime);
            result.computeStatusIfUnknown();
        }
    }

    private record CachedObject<T extends ObjectType>(PrismObject<T> object, boolean complete) {

        static <T extends ObjectType> CachedObject<T> of(@NotNull PrismObject<? extends ObjectType> object, boolean complete) {
            //noinspection unchecked
            return new CachedObject<>((PrismObject<T>) object, complete);
        }
    }
}
