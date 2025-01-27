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

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetVersionTraceType;

/**
 * Handler for getVersion operation.
 */
@Component
public class GetVersionOpHandler extends CachedOpHandler {

    private static final String GET_VERSION = CLASS_NAME_WITH_DOT + "getVersion";

    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        GetVersionOpExecution<T> exec = initializeExecution(type, oid, parentResult);

        try {

            if (exec.cacheUseMode.canNeverUseCachedData()) {
                exec.reportLocalAndGlobalPass();
            } else {
                var fromLocalCache = tryLocalCache(exec);
                if (fromLocalCache != null) {
                    return fromLocalCache;
                }

                var fromGlobalCache = tryGlobalCache(exec);
                if (fromGlobalCache != null) {
                    return fromGlobalCache;
                }
            }

            String version = getVersionInternal(type, oid, exec.result);
            if (exec.cacheUseMode.canUpdateVersionCache()) {
                cacheUpdater.storeVersionToVersionGlobal(exec.type, exec.oid, version, exec.globalInfo);
                cacheUpdater.storeVersionToVersionLocal(exec.oid, version, exec.localInfo);
            }
            return exec.prepareReturnValue(version);

        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            exec.result.computeStatusIfUnknown();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private <T extends ObjectType> String tryLocalCache(GetVersionOpExecution<T> exec) {

        if (!exec.localInfo.available) {
            exec.reportLocalNotAvailable();
            return null;
        }

        if (!exec.localInfo.supports) {
            exec.reportLocalPass();
            return null;
        }

        assert exec.localInfo.cache != null;
        String cachedVersion = exec.localInfo.cache.get(exec.oid);
        if (cachedVersion == null) {
            exec.reportLocalMiss();
            return null;
        }

        exec.reportLocalHit();
        return exec.prepareReturnValue(cachedVersion);
    }

    private <T extends ObjectType> String tryGlobalCache(GetVersionOpExecution<T> exec) {

        if (!exec.globalInfo.available) {
            exec.reportGlobalNotAvailable();
            return null;
        }

        if (!exec.globalInfo.supports) {
            exec.reportGlobalPass();
            return null;
        }

        String cachedVersion = globalVersionCache.get(exec.oid);
        if (cachedVersion == null) {
            exec.reportGlobalMiss();
            return null;
        }

        exec.reportGlobalHit();
        cacheUpdater.storeVersionToVersionLocal(exec.oid, cachedVersion, exec.localInfo);
        return exec.prepareReturnValue(cachedVersion);
    }

    private <T extends ObjectType> GetVersionOpExecution<T> initializeExecution(
            Class<T> type, String oid, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(GET_VERSION)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .build();

        RepositoryGetVersionTraceType trace;
        if (result.isTracingAny(RepositoryGetVersionTraceType.class)) {
            trace = new RepositoryGetVersionTraceType()
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .oid(oid);
            result.addTrace(trace);
        } else {
            trace = null;
        }

        CacheSetAccessInfo<T> caches = cacheSetAccessInfoFactory.determine(type);
        CacheUseMode cacheUseMode = CacheUseMode.determineForGetVersion(type);
        return new GetVersionOpExecution<>(type, oid, result, trace, null, caches, cacheUseMode);
    }

    private <T extends ObjectType> String getVersionInternal(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.getVersion(type, oid, result);
        } finally {
            repoOpEnd(startTime);
        }
    }
}
