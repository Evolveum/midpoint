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

            PassReason passReason = PassReason.determine(null, type);
            if (passReason != null) {
                exec.reportLocalAndGlobalPass(passReason);
                String loaded = getVersionInternal(type, oid, exec.result);
                return exec.prepareReturnValue(loaded);
            }

            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                assert exec.local.cache != null;
                String cachedVersion = exec.local.cache.get(oid);
                if (cachedVersion != null) {
                    exec.reportLocalHit();
                    return exec.prepareReturnValue(cachedVersion);
                } else {
                    exec.reportLocalMiss();
                }
            }

            if (!exec.global.available) {
                exec.reportGlobalNotAvailable();
            } else if (!exec.global.supports) {
                exec.reportGlobalPass();
            } else {
                String cachedVersion = globalVersionCache.get(oid);
                if (cachedVersion != null) {
                    exec.reportGlobalHit();
                    cacheUpdater.storeVersionToVersionLocal(exec.oid, cachedVersion, exec.local);
                    return exec.prepareReturnValue(cachedVersion);
                } else {
                    exec.reportGlobalMiss();
                }
            }

            String version = getVersionInternal(type, oid, exec.result);

            cacheUpdater.storeVersionToVersionGlobal(exec.type, exec.oid, version, exec.global);
            cacheUpdater.storeVersionToVersionLocal(exec.oid, version, exec.local);
            return exec.prepareReturnValue(version);
        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            exec.result.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> GetVersionOpExecution<T> initializeExecution(Class<T> type, String oid,
            OperationResult parentResult) {
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
        return new GetVersionOpExecution<>(type, oid, result, trace, null, prismContext, caches);
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
