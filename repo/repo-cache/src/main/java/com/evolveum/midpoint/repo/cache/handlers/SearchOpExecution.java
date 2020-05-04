/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastNormal;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismContext;
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

import org.jetbrains.annotations.NotNull;

/**
 * Context of searchObjects/searchObjectsIteratively operation.
 *
 * Responsible also for reporting on operation execution.
 */
class SearchOpExecution<O extends ObjectType>
        extends CachedOpExecution<RepositorySearchObjectsTraceType, LocalQueryCache, GlobalQueryCache, O> {

    final ObjectQuery query;

    SearchOpExecution(Class<O> type, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result,
            ObjectQuery query, RepositorySearchObjectsTraceType trace, TracingLevelType tracingLevel,
            PrismContext prismContext, CacheSetAccessInfo caches, String opName) {
        super(type, options, result, caches, caches.localQuery, caches.globalQuery, trace, tracingLevel, prismContext, opName);
        this.query = query;
    }

    void recordResult(SearchResultList<PrismObject<O>> objectsFound) {
        result.addReturn("objectsFound", objectsFound.size());
        if (trace != null) {
            trace.setResultSize(objectsFound.size());
            recordObjectsFound(objectsFound);
        }
    }

    private void recordObjectsFound(SearchResultList<PrismObject<O>> objectsFound) {
        if (isAtLeastNormal(tracingLevel)) {
            CompiledTracingProfile tracingProfile = result.getTracingProfile();
            int maxFullObjects = defaultIfNull(tracingProfile.getDefinition().getRecordObjectsFound(),
                    TraceUtil.DEFAULT_RECORD_OBJECTS_FOUND);
            int maxReferences = defaultIfNull(tracingProfile.getDefinition().getRecordObjectReferencesFound(),
                    TraceUtil.DEFAULT_RECORD_OBJECT_REFERENCES_FOUND);
            int objectsToVisit = Math.min(objectsFound.size(), maxFullObjects + maxReferences);
            assert trace != null;
            for (int i = 0; i < objectsToVisit; i++) {
                PrismObject<O> object = objectsFound.get(i);
                if (i < maxFullObjects) {
                    trace.getObjectRef().add(ObjectTypeUtil.createObjectRefWithFullObject(object.clone(), prismContext));
                } else {
                    trace.getObjectRef().add(ObjectTypeUtil.createObjectRef(object, prismContext));
                }
            }
        }
    }

    @NotNull SearchResultList<PrismObject<O>> prepareReturnValueAsIs(SearchResultList<PrismObject<O>> list) {
        recordResult(list);
        return list;
    }

    @NotNull SearchResultList<PrismObject<O>> prepareReturnValueWhenImmutable(SearchResultList<PrismObject<O>> immutable) {
        immutable.checkImmutable();
        recordResult(immutable);
        if (readOnly) {
            return immutable;
        } else {
            return immutable.deepClone();
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
