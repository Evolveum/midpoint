/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.log;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CacheUseCategoryTraceType.MISS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CacheUseCategoryTraceType.WEAK_HIT;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.cache.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetObjectTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

/**
 * Execution of getObject operation.
 */
class GetObjectOpExecution<O extends ObjectType>
        extends CachedOpExecution<RepositoryGetObjectTraceType, LocalObjectCache, GlobalObjectCache, O> {

    final String oid;

    GetObjectOpExecution(
            Class<O> type,
            String oid,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result,
            RepositoryGetObjectTraceType trace,
            TracingLevelType tracingLevel,
            CacheSetAccessInfo<O> caches,
            CacheUseMode cacheUseMode) {
        super(type, options, result, caches, caches.localObject, caches.globalObject,
                trace, tracingLevel, cacheUseMode, "getObject");
        this.oid = oid;
    }

    @Override
    String getDescription() {
        return type.getSimpleName() + ":" + oid;
    }

    @Override
    Class<LocalObjectCache> getLocalCacheClass() {
        return LocalObjectCache.class;
    }

    @Override
    Class<GlobalObjectCache> getGlobalCacheClass() {
        return GlobalObjectCache.class;
    }

    void reportGlobalVersionChangedMiss() {
        CachePerformanceCollector.INSTANCE.registerMiss(GlobalObjectCache.class, type, globalInfo.statisticsLevel);
        log("Cache (global): MISS because of version changed - getObject {}", globalInfo.traceMiss, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(MISS, "version changed"));
            // todo object if needed
        }
    }

    void reportGlobalWeakHit() {
        CachePerformanceCollector.INSTANCE.registerWeakHit(GlobalObjectCache.class, type, globalInfo.statisticsLevel);
        log("Cache (global): HIT with version check - getObject {}", globalInfo.traceMiss, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(WEAK_HIT));
        }
    }

    void recordResult(PrismObject<O> objectToReturn) {
        if (objectToReturn != null) {
            if (trace != null && tracingAtLeastNormal) {
                trace.setObjectRef(ObjectTypeUtil.createObjectRefWithFullObject(objectToReturn.clone()));
            }
            if (objectToReturn.getName() != null) {
                result.addContext("objectName", objectToReturn.getName().getOrig());
            }
        }
    }

    @NotNull PrismObject<O> prepareReturnValueAsIs(PrismObject<O> object) {
        recordResult(object);
        return object;
    }

    @NotNull PrismObject<O> prepareReturnValueWhenImmutable(PrismObject<O> immutable) {
        immutable.checkImmutable();
        recordResult(immutable);
        if (readOnly) {
            return immutable;
        } else {
            return immutable.clone();
        }
    }
}
