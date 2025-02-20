/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpEnd;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpStart;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.cache.values.CachedObjectValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalCacheObjectValue;
import com.evolveum.midpoint.repo.cache.local.LocalCacheObjectValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetObjectTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

/**
 * Handles `getObject` calls.
 */
@Component
public class GetObjectOpHandler extends CachedOpHandler {

    @NotNull
    public <T extends ObjectType> PrismObject<T> getObject(
            Class<T> type,
            String oid,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        GetObjectOpExecution<T> exec = initializeExecution(type, oid, options, parentResult);

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
            return executeAndCache(exec);

        } catch (Throwable t) {
            exec.result.recordException(t);
            throw t;
        } finally {
            exec.result.close();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private <T extends ObjectType> @Nullable PrismObject<T> tryLocalCache(GetObjectOpExecution<T> exec)
            throws ObjectNotFoundException {

        if (!exec.localInfo.available) {
            exec.reportLocalNotAvailable();
            return null;
        }

        if (!exec.localInfo.supports) {
            exec.reportLocalPass();
            return null;
        }

        assert exec.localInfo.cache != null;
        LocalCacheObjectValue<T> cachedValue = exec.localInfo.cache.get(exec.oid);
        if (cachedValue == null) {
            exec.reportLocalMiss();
            return null;
        }

        if (exec.cacheUseMode.canUseCachedDataOnlyIfComplete() && !cachedValue.isComplete()) {
            exec.reportLocalPass();
            return null;
        }

        // Even if there are "include" options, we can return the cached object, as it is complete.
        // Caches are not updated in this case.
        exec.reportLocalHit();
        return exec.prepareReturnValueWhenImmutable(cachedValue.getObject());
    }

    private <T extends ObjectType> PrismObject<T> tryGlobalCache(GetObjectOpExecution<T> exec)
            throws SchemaException, ObjectNotFoundException {

        if (!exec.globalInfo.available) {
            exec.reportGlobalNotAvailable();
            return null;
        }

        if (!exec.globalInfo.supports) {
            exec.reportGlobalPass();
            return null;
        }

        GlobalCacheObjectValue<T> cachedValue = globalObjectCache.get(exec.oid);
        if (cachedValue == null) {
            exec.reportGlobalMiss();
            return null;
        }

        if (exec.cacheUseMode.canUseCachedDataOnlyIfComplete() && !cachedValue.isComplete()) {
            exec.reportGlobalPass();
            return null;
        }

        if (cachedValue.shouldCheckVersion()) {
            if (hasVersionChanged(cachedValue, exec)) {
                exec.reportGlobalVersionChangedMiss();
                return null;
            } else { // version matches, renew ttl
                exec.reportGlobalWeakHit();
                var cachedObject = cachedValue.getObject();
                cacheUpdater.storeImmutableObjectToAllLocal(cachedObject, exec.cachesInfo, cachedValue.isComplete());
                cachedValue.setCheckVersionTime(
                        exec.globalInfo.getCache().getNextVersionCheckTime(exec.type));
                return exec.prepareReturnValueWhenImmutable(cachedObject);
            }
        }

        exec.reportGlobalHit();
        var cachedObject = cachedValue.getObject();
        cacheUpdater.storeImmutableObjectToAllLocal(cachedObject, exec.cachesInfo, cachedValue.isComplete());
        return exec.prepareReturnValueWhenImmutable(cachedObject);
    }

    private <T extends ObjectType> GetObjectOpExecution<T> initializeExecution(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(RepositoryCache.OP_GET_OBJECT_IMPL)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("options", options)
                .build();

        TracingLevelType tracingLevel = result.getTracingLevel(RepositoryGetObjectTraceType.class);
        RepositoryGetObjectTraceType trace;
        if (isAtLeastMinimal(tracingLevel)) {
            trace = new RepositoryGetObjectTraceType()
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .oid(oid)
                    .options(String.valueOf(options));
            result.addTrace(trace);
        } else {
            trace = null;
        }

        CacheSetAccessInfo<T> cacheInfos = cacheSetAccessInfoFactory.determine(type);
        CacheUseMode cacheUseMode = CacheUseMode.determine(options, type);
        return new GetObjectOpExecution<>(type, oid, options, result, trace, tracingLevel, cacheInfos, cacheUseMode);
    }

    @NotNull
    private <T extends ObjectType> PrismObject<T> getObjectInternal(GetObjectOpExecution<T> exec)
            throws SchemaException, ObjectNotFoundException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.getObject(exec.type, exec.oid, exec.options, exec.result);
        } finally {
            repoOpEnd(startTime);
        }
    }

    // returns directly returnable object (frozen if readonly, mutable if not readonly)
    private <T extends ObjectType> PrismObject<T> executeAndCache(GetObjectOpExecution<T> exec)
            throws SchemaException, ObjectNotFoundException {
        try {
            PrismObject<T> object = getObjectInternal(exec);
            exec.recordResult(object);

            if (exec.cacheUseMode.canUpdateVersionCache()) {
                cacheUpdater.storeObjectToVersionGlobal(object, exec.cachesInfo.globalVersion);
                cacheUpdater.storeObjectToVersionLocal(object, exec.cachesInfo.localVersion);
            }

            if (!exec.cacheUseMode.canUpdateObjectCache() || !exec.cachesInfo.isEffectivelySupportedByAnyObjectCache()) {
                return exec.readOnly ?
                        Freezable.doFreeze(object) :
                        object.cloneIfImmutable();
            }

            PrismObject<T> immutable = CloneUtil.toImmutable(object);
            var complete = CachedObjectValue.computeCompleteFlag(immutable);
            cacheUpdater.storeImmutableObjectToObjectLocal(immutable, exec.cachesInfo, complete);
            cacheUpdater.storeImmutableObjectToObjectGlobal(immutable, complete);
            return exec.readOnly ?
                    immutable :
                    object.cloneIfImmutable();

        } catch (ObjectNotFoundException | SchemaException ex) {
            globalObjectCache.remove(exec.oid);
            globalVersionCache.remove(exec.oid);
            throw ex;
        }
    }

    private boolean hasVersionChanged(GlobalCacheObjectValue<?> object, GetObjectOpExecution<?> exec)
            throws ObjectNotFoundException, SchemaException {
        try {
            // TODO shouldn't we record repoOpStart/repoOpEnd?
            String version = repositoryService.getVersion(exec.type, exec.oid, exec.result);
            return !Objects.equals(version, object.getObjectVersion());
        } catch (ObjectNotFoundException | SchemaException ex) {
            globalObjectCache.remove(exec.oid);
            globalVersionCache.remove(exec.oid);
            throw ex;
        }
    }
}
