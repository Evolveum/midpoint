/*
 * Copyright (C) 2020-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.RepositoryCache.CLASS_NAME_WITH_DOT;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpEnd;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpStart;
import static com.evolveum.midpoint.schema.GetOperationOptions.isReadOnly;
import static com.evolveum.midpoint.schema.SelectorOptions.findRootOptions;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.repo.cache.other.MonitoringUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Handler for searchObjects/searchObjectsIterative operations.
 */
@Component
public class SearchOpHandler extends CachedOpHandler {

    private static final String SEARCH_OBJECTS = "searchObjects";
    private static final String SEARCH_OBJECTS_ITERATIVE = "searchObjectsIterative";

    private static final String OP_SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + "searchContainers";
    private static final String OP_SEARCH_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "searchShadowOwner";
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

    @NotNull
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {

        SearchOpExecution<T> exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS);

        try {
            QueryKey<T> key = new QueryKey<>(type, query);

            // Checks related to both caches
            PassReason passReason = PassReason.determineForSearch(options, type);
            if (passReason != null) {
                exec.reportLocalAndGlobalPass(passReason);
                SearchResultList<PrismObject<T>> objects;
                if (passReason.isSoft()) {
                    // Soft = execute the search but remember the result
                    objects = executeAndCacheSearch(exec, key);
                } else {
                    // Hard = pass the cache altogether - most probably because the objects differ from "standard" ones
                    objects = searchObjectsInternal(type, query, options, exec.result);
                }
                return exec.prepareReturnValueAsIs(objects);
            }

            // Let's try local cache
            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                SearchResultList<PrismObject<T>> cachedResult = exec.local.getCache().get(key);
                if (cachedResult != null) {
                    exec.reportLocalHit();
                    return exec.prepareReturnValueWhenImmutable(cachedResult);
                } else {
                    exec.reportLocalMiss();
                }
            }

            // Then try global cache
            if (!exec.global.available) {
                exec.reportGlobalNotAvailable();
                SearchResultList<PrismObject<T>> objects = executeAndCacheSearch(exec, key);
                return exec.prepareReturnValueAsIs(objects);
            } else if (!exec.global.supports) {
                exec.reportGlobalPass();
                SearchResultList<PrismObject<T>> objects = executeAndCacheSearch(exec, key);
                return exec.prepareReturnValueAsIs(objects);
            }

            SearchResultList<PrismObject<T>> cachedResult = globalQueryCache.get(key);
            if (cachedResult != null) {
                exec.reportGlobalHit();
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, cachedResult, exec.caches);
                return exec.prepareReturnValueWhenImmutable(cachedResult);
            } else {
                exec.reportGlobalMiss();
                SearchResultList<PrismObject<T>> objects = executeAndCacheSearch(exec, key);
                return exec.prepareReturnValueAsIs(objects);
            }
        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            exec.result.computeStatusIfUnknown();
        }
    }

    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential, OperationResult parentResult) throws SchemaException {

        SearchOpExecution<T> exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS_ITERATIVE);
        ReportingResultHandler<T> reportingHandler = new ReportingResultHandler<>(handler, exec);

        try {
            // Checks related to both caches
            PassReason passReason = PassReason.determineForSearch(options, type);
            if (passReason != null) {
                exec.reportLocalAndGlobalPass(passReason);
                return searchObjectsIterativeInternal(type, query, reportingHandler, options, strictlySequential, exec.result);
            }
            QueryKey<T> key = new QueryKey<>(type, query);

            // Let's try local cache
            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                SearchResultList<PrismObject<T>> cachedResult = exec.local.getCache().get(key);
                if (cachedResult != null) {
                    exec.reportLocalHit();
                    return iterateOverImmutableQueryResult(exec, cachedResult, reportingHandler);
                } else {
                    exec.reportLocalMiss();
                }
            }

            // Then try global cache
            if (!exec.global.available) {
                exec.reportGlobalNotAvailable();
                return executeAndCacheSearchIterative(exec, key, reportingHandler, strictlySequential);
            } else if (!exec.global.supports) {
                exec.reportGlobalPass();
                return executeAndCacheSearchIterative(exec, key, reportingHandler, strictlySequential);
            }

            SearchResultList<PrismObject<T>> cachedResult = globalQueryCache.get(key);
            if (cachedResult != null) {
                exec.reportGlobalHit();
                cachedResult.checkImmutable();
                // What if objects from the result are modified during iteration? Nothing wrong happens: As they are cached
                // before the execution, the usual invalidation will take place.
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, cachedResult, exec.caches);
                iterateOverImmutableQueryResult(exec, cachedResult, reportingHandler);
                return cachedResult.getMetadata();
            } else {
                exec.reportGlobalMiss();
                return executeAndCacheSearchIterative(exec, key, reportingHandler, strictlySequential);
            }
        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            reportingHandler.recordResult();
            exec.result.computeStatusIfUnknown();
        }
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
        return new SearchOpExecution<>(type, options, result, query, trace, level, prismContext, caches, opName);
    }

    // returns directly returnable list (frozen if readonly, mutable if not readonly)
    private <T extends ObjectType> SearchResultList<PrismObject<T>> executeAndCacheSearch(SearchOpExecution<T> exec, QueryKey<T> key)
            throws SchemaException {
        try {
            SearchResultList<PrismObject<T>> objects = searchObjectsInternal(key.getType(), key.getQuery(), exec.options, exec.result);
            return cacheUpdater.storeSearchResultToAll(key, objects, exec.caches, exec.readOnly, exec.started);
        } catch (SchemaException ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    private <T extends ObjectType> SearchResultMetadata executeAndCacheSearchIterative(SearchOpExecution<T> exec, QueryKey<T> key,
            ReportingResultHandler<T> recordingHandler, boolean strictlySequential) throws SchemaException {
        try {
            boolean queryCacheable = exec.global.effectivelySupports() || exec.local.effectivelySupports();
            CachingResultHandler<T> cachingHandler = new CachingResultHandler<>(recordingHandler, queryCacheable,
                    exec.readOnly, exec.started, exec.type, cacheUpdater);

            SearchResultMetadata metadata;
            try {
                if (queryCacheable) {
                    invalidator.registerInvalidationEventsListener(cachingHandler);
                }
                metadata = searchObjectsIterativeInternal(exec.type, exec.query, cachingHandler, exec.options,
                        strictlySequential, exec.result);
            } finally {
                if (queryCacheable) {
                    invalidator.unregisterInvalidationEventsListener(cachingHandler);
                }
            }

            SearchResultList<PrismObject<T>> searchResultList = cachingHandler.getCacheableSearchResult(metadata);
            if (searchResultList != null &&
                    invalidator.isSearchResultValid(key, searchResultList, cachingHandler.getInvalidationEvents())) {
                SearchResultList<PrismObject<T>> immutableList = searchResultList.toDeeplyFrozenList();
                cacheUpdater.storeImmutableSearchResultToQueryLocal(key, immutableList, exec.caches);
                cacheUpdater.storeImmutableSearchResultToQueryGlobal(key, immutableList, exec.caches);
            }
            return metadata;
        } catch (SchemaException ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    @NotNull
    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsInternal(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchObjects(type, query, options, parentResult);
        } finally {
            repoOpEnd(startTime);
        }
    }

    private <T extends ObjectType> SearchResultMetadata searchObjectsIterativeInternal(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential, OperationResult parentResult) throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.searchObjectsIterative(type, query, handler, options, strictlySequential, parentResult);
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

    public <F extends FocusType> PrismObject<F> searchShadowOwner(
            String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_SEARCH_SHADOW_OWNER)
                .addParam("shadowOid", shadowOid)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();
        try {
            // TODO cache the search operation?
            PrismObject<F> ownerObject;
            Long startTime = repoOpStart();
            try {
                ownerObject = repositoryService.searchShadowOwner(shadowOid, options, result);
            } finally {
                repoOpEnd(startTime);
            }
            if (ownerObject != null) {
                Class<F> type = ownerObject.getCompileTimeClass();
                if (type != null && PassReason.determineForSearch(options, type) == null) {
                    boolean readOnly = isReadOnly(findRootOptions(options));
                    cacheUpdater.storeLoadedObjectToAll(ownerObject, readOnly, 0);
                }
            }

            return ownerObject;
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


}
