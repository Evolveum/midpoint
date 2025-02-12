/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Collection;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.local.QueryKey;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositorySearchObjectsTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

/**
 * Context of `searchObjects` and `searchObjectsIteratively` operations.
 *
 * Responsible also for reporting on operation execution.
 */
class SearchOpExecution<O extends ObjectType>
        extends CachedOpExecution<RepositorySearchObjectsTraceType, LocalQueryCache, GlobalQueryCache, O> {

    final ObjectQuery query;
    final QueryKey<O> queryKey;
    private final int maxFullObjectsToTrace;
    private final int maxReferencesToTrace;

    SearchOpExecution(
            Class<O> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result,
            ObjectQuery query,
            RepositorySearchObjectsTraceType trace,
            TracingLevelType tracingLevel,
            CacheSetAccessInfo<O> caches,
            CacheUseMode cacheUseMode,
            String opName) {
        super(type, options, result, caches, caches.localQuery, caches.globalQuery, trace, tracingLevel, cacheUseMode, opName);
        this.query = query;
        this.queryKey = new QueryKey<>(type, query);

        CompiledTracingProfile tracingProfile = result.getTracingProfile();
        if (tracingProfile != null) {
            maxFullObjectsToTrace = defaultIfNull(tracingProfile.getDefinition().getRecordObjectsFound(),
                    TraceUtil.DEFAULT_RECORD_OBJECTS_FOUND);
            maxReferencesToTrace = defaultIfNull(tracingProfile.getDefinition().getRecordObjectReferencesFound(),
                    TraceUtil.DEFAULT_RECORD_OBJECT_REFERENCES_FOUND);
        } else {
            maxFullObjectsToTrace = 0;
            maxReferencesToTrace = 0;
        }
    }

    void recordSearchResult(SearchResultList<PrismObject<O>> objectsFound) {
        if (objectsFound == null) {
            return;
        }
        recordNumberOfObjectsFound(objectsFound.size());
        if (trace != null) {
            trace.setResultSize(objectsFound.size());
            for (PrismObject<O> objectFound : objectsFound) {
                boolean canContinue = recordObjectFound(objectFound);
                if (!canContinue) {
                    break;
                }
            }
        }
    }

    void recordSearchResult(int numberOfObjectsFound, boolean wasInterrupted) {
        recordNumberOfObjectsFound(numberOfObjectsFound);
        result.addReturn(RepositoryService.RETURN_INTERRUPTED, wasInterrupted);
        // objects themselves were already recorded by ReportingResultHandler
    }

    private void recordNumberOfObjectsFound(int number) {
        result.addReturn(RepositoryService.RETURN_OBJECTS_FOUND, number);
    }

    /**
     * Puts the object found into RepositorySearchObjectsTraceType object.
     * @return false if we are sure we don't want to be called again
     */
    boolean recordObjectFound(PrismObject<O> object) {
        if (trace != null && tracingAtLeastNormal) {
            if (trace.getObjectRef().size() < maxFullObjectsToTrace) {
                trace.getObjectRef().add(ObjectTypeUtil.createObjectRefWithFullObject(object.clone()));
                return true;
            } else if (trace.getObjectRef().size() < maxFullObjectsToTrace + maxReferencesToTrace) {
                trace.getObjectRef().add(ObjectTypeUtil.createObjectRef(object));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    String getDescription() {
        return type.getSimpleName() + ": " + query;
    }

    @Override
    Class<LocalQueryCache> getLocalCacheClass() {
        return LocalQueryCache.class;
    }

    @Override
    Class<GlobalQueryCache> getGlobalCacheClass() {
        return GlobalQueryCache.class;
    }
}
