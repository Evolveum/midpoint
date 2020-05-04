/*
 * Copyright (c) 2020 Evolveum and contributors
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

import com.evolveum.midpoint.repo.cache.other.MonitoringUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositorySearchObjectsTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

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

    static final int QUERY_RESULT_SIZE_LIMIT = 100000;

    private static final String OP_ITERATE_OVER_QUERY_RESULT = RepositoryCache.class.getName() + ".iterateOverQueryResult";

    @NotNull
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {

        SearchOpExecution<T> exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS);

        try {
            // Checks related to both caches
            PassReason passReason = PassReason.determine(options, type);
            if (passReason != null) {
                exec.reportLocalAndGlobalPass(passReason);
                SearchResultList<PrismObject<T>> objects = searchObjectsInternal(type, query, options, exec.result);
                return exec.prepareReturnValueAsIs(objects);
            }
            QueryKey<T> key = new QueryKey<>(type, query);

            // Let's try local cache
            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                SearchResultList<PrismObject<T>> cachedResult = exec.local.cache.get(key);
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
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, cachedResult, true, exec.caches);
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

    private static class WatchingHandler<T extends ObjectType> implements ResultHandler<T> {

        private final ResultHandler<T> innerHandler;
        private final SearchResultList<PrismObject<T>> objectsHandled = new SearchResultList<>();
        private boolean interrupted;

        private WatchingHandler(ResultHandler<T> innerHandler) {
            this.innerHandler = innerHandler;
        }

        @Override
        public boolean handle(PrismObject<T> object, OperationResult result) {
            objectsHandled.add(object);
            boolean cont = innerHandler.handle(object, result);
            if (!cont) {
                interrupted = true;
            }
            return cont;
        }
    }

    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential, OperationResult parentResult) throws SchemaException {

        SearchOpExecution<T> exec = initializeExecution(type, query, options, parentResult, SEARCH_OBJECTS_ITERATIVE);
        WatchingHandler<T> watchingHandler = new WatchingHandler<>(handler);

        try {
            // Checks related to both caches
            PassReason passReason = PassReason.determine(options, type);
            if (passReason != null) {
                exec.reportLocalAndGlobalPass(passReason);
                return searchObjectsIterativeInternal(type, query, watchingHandler, options, strictlySequential, exec.result);
            }
            QueryKey<T> key = new QueryKey<>(type, query);

            // Let's try local cache
            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                SearchResultList<PrismObject<T>> cachedResult = exec.local.cache.get(key);
                if (cachedResult != null) {
                    exec.reportLocalHit();
                    return iterateOverImmutableQueryResult(exec, cachedResult, watchingHandler);
                } else {
                    exec.reportLocalMiss();
                }
            }

            // Then try global cache
            if (!exec.global.available) {
                exec.reportGlobalNotAvailable();
                return executeAndCacheSearchIterative(exec, key, watchingHandler, strictlySequential);
            } else if (!exec.global.supports) {
                exec.reportGlobalPass();
                return executeAndCacheSearchIterative(exec, key, watchingHandler, strictlySequential);
            }

            SearchResultList<PrismObject<T>> cachedResult = globalQueryCache.get(key);
            if (cachedResult != null) {
                exec.reportGlobalHit();
                cachedResult.checkImmutable();
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, cachedResult, true, exec.caches);
                iterateOverImmutableQueryResult(exec, cachedResult, watchingHandler);
                return cachedResult.getMetadata();
            } else {
                exec.reportGlobalMiss();
                return executeAndCacheSearchIterative(exec, key, watchingHandler, strictlySequential);
            }
        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            exec.recordResult(watchingHandler.objectsHandled);
            exec.result.addReturn("interrupted", watchingHandler.interrupted);
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
            trace = new RepositorySearchObjectsTraceType(prismContext)
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .query(prismContext.getQueryConverter().createQueryType(query))
                    .options(String.valueOf(options));
            result.addTrace(trace);
        } else {
            trace = null;
        }
        CacheSetAccessInfo caches = cacheSetAccessInfoFactory.determine(type);
        return new SearchOpExecution<>(type, options, result, query, trace, level, prismContext, caches, opName);
    }

    // returns directly returnable list (frozen if readonly, mutable if not readonly)
    private <T extends ObjectType> SearchResultList<PrismObject<T>> executeAndCacheSearch(SearchOpExecution<T> exec, QueryKey<T> key)
            throws SchemaException {
        try {
            SearchResultList<PrismObject<T>> objects = searchObjectsInternal(key.getType(), key.getQuery(), exec.options, exec.result);
            if (exec.readOnly) {
                SearchResultList<PrismObject<T>> immutableObjectList = objects.toDeeplyFrozenList();
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, immutableObjectList, true, exec.caches);
                cacheUpdater.storeImmutableSearchResultToAllGlobal(key, immutableObjectList, true, exec.caches);
                return immutableObjectList;
            } else {
                cacheUpdater.storeSearchResultToAll(key, objects, exec.caches);
                return objects;
            }
        } catch (SchemaException ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    private <T extends ObjectType> SearchResultMetadata executeAndCacheSearchIterative(SearchOpExecution<T> exec, QueryKey<T> key,
            WatchingHandler<T> watchingHandler, boolean strictlySequential) throws SchemaException {
        try {
            CollectingHandler<T> collectingHandler = new CollectingHandler<>(watchingHandler);
            SearchResultMetadata metadata = searchObjectsIterativeInternal(exec.type, exec.query, collectingHandler, exec.options,
                    strictlySequential, exec.result);
            SearchResultList<PrismObject<T>> list = collectingHandler.getObjects();
            if (list != null) { // todo optimize cloning here
                SearchResultList<PrismObject<T>> immutableList = list.toDeeplyFrozenList();
                cacheUpdater.storeImmutableSearchResultToAllLocal(key, immutableList, !watchingHandler.interrupted, exec.caches);
                cacheUpdater.storeImmutableSearchResultToAllGlobal(key, immutableList, !watchingHandler.interrupted, exec.caches);
            }
            return metadata;
        } catch (SchemaException ex) {
            globalQueryCache.remove(key);
            throw ex;
        }
    }

    private static final class CollectingHandler<T extends ObjectType> implements ResultHandler<T> {

        private boolean overflown = false;
        private final SearchResultList<PrismObject<T>> objects = new SearchResultList<>();
        private final ResultHandler<T> originalHandler;

        private CollectingHandler(ResultHandler<T> handler) {
            originalHandler = handler;
        }

        @Override
        public boolean handle(PrismObject<T> object, OperationResult parentResult) {
            if (objects.size() < QUERY_RESULT_SIZE_LIMIT) {
                objects.add(object.clone()); // todo optimize on read only option
            } else {
                overflown = true;
            }
            return originalHandler.handle(object, parentResult);
        }

        private SearchResultList<PrismObject<T>> getObjects() {
            return overflown ? null : objects;
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
                if (type != null && PassReason.determine(options, type) == null) {
                    boolean readOnly = isReadOnly(findRootOptions(options));
                    CacheSetAccessInfo caches = cacheSetAccessInfoFactory.determine(type);
                    cacheUpdater.storeLoadedObjectToAll(ownerObject, caches, readOnly);
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

    public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
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
