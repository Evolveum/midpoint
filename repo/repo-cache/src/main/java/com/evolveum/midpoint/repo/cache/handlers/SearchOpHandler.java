/*
 * Copyright (C) 2020-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.api.RepositoryService.*;
import static com.evolveum.midpoint.repo.cache.RepositoryCache.OP_SEARCH_OBJECTS_IMPL;
import static com.evolveum.midpoint.repo.cache.RepositoryCache.OP_SEARCH_OBJECTS_ITERATIVE_IMPL;
import static com.evolveum.midpoint.repo.cache.handlers.CacheUpdater.toImmutableOidList;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalObjectCache;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpEnd;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpStart;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.repo.cache.values.CachedQueryValue;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

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

import javax.xml.namespace.QName;

/**
 * Handler for `searchObjects` and `searchObjectsIterative` operations.
 */
@Component
public class SearchOpHandler extends CachedOpHandler {

    /**
     * Queries resulting in more objects will not be cached "as such" - although individual objects/versions can be cached.
     */
    public static final int QUERY_RESULT_SIZE_LIMIT = 100;

    public <T extends ObjectType> @NotNull SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException {
        var exec = initializeExecution(
                type, query, options, parentResult, OP_SEARCH_OBJECTS, OP_SEARCH_OBJECTS_IMPL);
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
        var exec = initializeExecution(
                type, query, options, parentResult,
                OP_SEARCH_OBJECTS_ITERATIVE, OP_SEARCH_OBJECTS_ITERATIVE_IMPL);
        var recordingHandler = new RecordingResultHandler<>(handler, exec);
        try {
            return doSearchObjectsIterative(exec, recordingHandler, strictlySequential);
        } catch (Throwable t) {
            exec.result.recordException(t);
            throw t;
        } finally {
            recordingHandler.recordResult();
            exec.result.close();
        }
    }

    private <T extends ObjectType> @Nullable SearchResultMetadata doSearchObjectsIterative(
            SearchOpExecution<T> exec, RecordingResultHandler<T> recordingHandler, boolean strictlySequential)
            throws SchemaException {
        if (exec.cacheUseMode.canNeverUseCachedData()) {
            exec.reportLocalAndGlobalPass();
        } else {
            var fromCache = tryCaches(exec);
            if (fromCache != null) {
                return iterateOverImmutableQueryResult(exec, fromCache, recordingHandler);
            }
        }
        return executeAndCacheSearchIterative(exec, recordingHandler, strictlySequential);
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
        cacheUpdater.storeImmutableSearchResult(exec, cachedResult, toImmutableOidList(cachedResult), false);
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

    private <T extends ObjectType> SearchOpExecution<T> initializeExecution(
            Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult,
            String localOpName,
            String fullOpName)
            throws SchemaException {
        OperationResult result = parentResult.subresult(fullOpName)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("query", query)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();
        try {
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
            QName objectClassName = getObjectClassNameFromQuery(query);
            CacheSetAccessInfo<T> caches = cacheSetAccessInfoFactory.determine(type, objectClassName);
            CacheUseMode cacheUseMode = CacheUseMode.determine(options, type);
            return new SearchOpExecution<>(type, options, result, query, trace, level, caches, cacheUseMode, localOpName);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        }
    }

    private QName getObjectClassNameFromQuery(ObjectQuery query) throws SchemaException {
        return query != null ? ObjectQueryUtil.getObjectClassNameFromFilter(query.getFilter()) : null;
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
            boolean canUpdateAtLeastOneCache = exec.cacheUseMode.canUpdateAtLeastOneCache();
            var canUpdateQueryCache =
                    canUpdateAtLeastOneCache // this is just for static compile-time checks (regarding handler casting below)
                            && exec.cacheUseMode.canUpdateQueryCache()
                            && exec.cachesInfo.isEffectivelySupportedByAnyQueryCache();

            var innerHandler = canUpdateAtLeastOneCache ?
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
            // Not providing own operation result here, as there's no processing in this module.
            return repositoryService.searchObjectsIterative(
                    exec.type, exec.query, handler, exec.options, strictlySequential, exec.result);
        } finally {
            repoOpEnd(startTime);
        }
    }

    private <T extends ObjectType> SearchResultMetadata iterateOverImmutableQueryResult(
            SearchOpExecution<T> exec,
            SearchResultList<PrismObject<T>> immutableList,
            RecordingResultHandler<T> recordingHandler) {
        // We provide our own handler here so that the result will be correctly aggregated, even for naive clients
        var resultProvidingHandler = recordingHandler.providingOwnOperationResult(RepositoryCache.OP_HANDLE_OBJECT_FOUND_IMPL);
        OperationResult result = exec.result.subresult(RepositoryCache.OP_ITERATE_OVER_QUERY_RESULT)
                .setMinor()
                .addParam("objects", immutableList.size())
                .build();
        try {
            for (PrismObject<T> immutableObject : immutableList) {
                immutableObject.checkImmutable();
                PrismObject<T> objectToHandle = exec.readOnly ? immutableObject : immutableObject.clone();
                if (!resultProvidingHandler.handle(objectToHandle, result)) {
                    break;
                }
            }
            // todo Should be metadata influenced by the number of handler executions?
            //   ...and is it correct to return cached metadata at all?
            return immutableList.getMetadata() != null ? immutableList.getMetadata().clone() : null;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public @NotNull <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.subresult(RepositoryCache.OP_SEARCH_CONTAINERS_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_COUNT_CONTAINERS_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_SEARCH_REFERENCES_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_COUNT_REFERENCES_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_SEARCH_REFERENCES_ITERATIVE_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_SEARCH_CONTAINERS_ITERATIVE_IMPL)
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
        OperationResult result = parentResult.subresult(RepositoryCache.OP_COUNT_OBJECTS_IMPL)
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
