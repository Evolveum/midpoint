/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetVersionTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

/**
 * Execution of getVersion operation.
 */
class GetVersionOpExecution<O extends ObjectType>
        extends CachedOpExecution<RepositoryGetVersionTraceType, LocalVersionCache, GlobalVersionCache, O> {

    final String oid;

    GetVersionOpExecution(Class<O> type, String oid, OperationResult result,
            RepositoryGetVersionTraceType trace, TracingLevelType tracingLevel,
            CacheSetAccessInfo<O> caches, CacheUseMode cacheUseMode) {
        super(type, null, result,
                caches, caches.localVersion, caches.globalVersion,
                trace, tracingLevel, cacheUseMode, "getVersion");
        this.oid = oid;
    }

    @Override
    String getDescription() {
        return type.getSimpleName() + ":" + oid;
    }

    @Override
    Class<LocalVersionCache> getLocalCacheClass() {
        return LocalVersionCache.class;
    }

    @Override
    Class<GlobalVersionCache> getGlobalCacheClass() {
        return GlobalVersionCache.class;
    }

    private void recordResult(String version) {
        if (trace != null) {
            trace.setVersion(version);
        }
        result.addReturn("version", version);
    }

    String prepareReturnValue(String version) {
        recordResult(version);
        return version;
    }
}
