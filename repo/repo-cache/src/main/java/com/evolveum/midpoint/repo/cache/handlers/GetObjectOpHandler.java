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
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.evolveum.midpoint.prism.Item;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalCacheObjectValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetObjectTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;

/**
 * Handles getObject calls.
 */
@Component
public class GetObjectOpHandler extends CachedOpHandler {

    private static final String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";

    @NotNull
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        GetObjectOpExecution<T> exec = initializeExecution(type, oid, options, parentResult);

        try {
             // Checks related to both caches
            PassReason passReason = PassReason.determine(options, type);
            if (passReason != null && !passReason.isBecauseOfRootIncludeOption()) {
                // Local nor global cache not interested in caching this object.
                // (The "include" option is treated differently, see below.)
                exec.reportLocalAndGlobalPass(passReason);
                return passTheCaches(type, oid, options, exec);
            }

            // Let's try local cache first
            if (!exec.local.available) {
                exec.reportLocalNotAvailable();
            } else if (!exec.local.supports) {
                exec.reportLocalPass();
            } else {
                assert exec.local.cache != null;
                PrismObject<T> cachedObject = exec.local.cache.get(oid);
                if (cachedObject != null) {
                    assert passReason == null || passReason.isBecauseOfRootIncludeOption();
                    if (passReason != null && containsIncompleteItem(cachedObject)) {
                        exec.reportLocalAndGlobalPass(passReason);
                        return passTheCaches(type, oid, options, exec);
                    } else {
                        // Even if there are "include" options, we can return the cached object, as it is complete.
                        // Caches are not updated in this case.
                        exec.reportLocalHit();
                        return exec.prepareReturnValueWhenImmutable(cachedObject);
                    }
                } else {
                    exec.reportLocalMiss();
                }
            }

            // Then try global cache
            if (!exec.global.available) {
                exec.reportGlobalNotAvailable();
                PrismObject<T> object = executeAndCache(exec);
                return exec.prepareReturnValueAsIs(object);
            } else if (!exec.global.supports) {
                exec.reportGlobalPass();
                PrismObject<T> object = executeAndCache(exec);
                return exec.prepareReturnValueAsIs(object);
            }

            GlobalCacheObjectValue<T> cachedValue = globalObjectCache.get(oid);
            if (cachedValue == null) {
                exec.reportGlobalMiss();
                PrismObject<T> object = executeAndCache(exec);
                return exec.prepareReturnValueAsIs(object);
            } else {
                PrismObject<T> cachedObject = cachedValue.getObject();
                if (!cachedValue.shouldCheckVersion()) {
                    assert passReason == null || passReason.isBecauseOfRootIncludeOption();
                    if (passReason != null && containsIncompleteItem(cachedObject)) {
                        exec.reportGlobalPass(passReason);
                        return passTheCaches(type, oid, options, exec);
                    }
                    exec.reportGlobalHit();
                    cacheUpdater.storeImmutableObjectToAllLocal(cachedObject, exec.caches);
                    return exec.prepareReturnValueWhenImmutable(cachedObject);
                } else {
                    if (hasVersionChanged(type, oid, cachedValue, exec.result)) {
                        exec.reportGlobalVersionChangedMiss();
                        assert passReason == null || passReason.isBecauseOfRootIncludeOption();
                        if (passReason != null) {
                            // We cannot store the object, as it MAY contain fetched items that are not returnable by default.
                            // (We could check if it really does contain them, but that code would be currently too fragile.)
                            exec.reportGlobalPass(passReason);
                            return passTheCaches(type, oid, options, exec);
                        }
                        PrismObject<T> object = executeAndCache(exec);
                        return exec.prepareReturnValueAsIs(object);
                    } else { // version matches, renew ttl
                        assert passReason == null || passReason.isBecauseOfRootIncludeOption();
                        if (passReason != null && containsIncompleteItem(cachedObject)) {
                            exec.reportGlobalPass(passReason);
                            return passTheCaches(type, oid, options, exec);
                        }
                        exec.reportGlobalWeakHit();
                        cacheUpdater.storeImmutableObjectToAllLocal(cachedObject, exec.caches);
                        long newTimeToVersionCheck = exec.global.getCache().getNextVersionCheckTime(exec.type);
                        cachedValue.setCheckVersionTime(newTimeToVersionCheck);
                        return exec.prepareReturnValueWhenImmutable(cachedObject);
                    }
                }
            }
        } catch (ObjectNotFoundException e) {
            exec.result.recordException(e);
            throw e;
        } catch (Throwable t) {
            exec.result.recordFatalError(t);
            throw t;
        } finally {
            exec.result.close();
        }
    }

    // TODO as a performance optimization, couldn't the repo indicate that the object is incomplete (at the global level)?
    private <T extends ObjectType> boolean containsIncompleteItem(PrismObject<T> cachedObject) {
        var allItemsComplete = new AtomicBoolean(true);
        cachedObject.acceptVisitor(
                visitable -> {
                    // TODO it would be better (from the performance viewpoint) if we could abort on the first occurrence
                    //  of an incomplete item
                    if (visitable instanceof Item<?, ?> item && item.isIncomplete()) {
                        allItemsComplete.set(false);
                        return false;
                    } else {
                        return true;
                    }
                });
        return !allItemsComplete.get();
    }

    /** This method intentionally does NOT cache the object. */
    private <T extends ObjectType> @NotNull PrismObject<T> passTheCaches(
            Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            GetObjectOpExecution<T> exec) throws SchemaException, ObjectNotFoundException {
        PrismObject<T> loaded = getObjectInternal(type, oid, options, exec.result);
        return exec.prepareReturnValueAsIs(loaded);
    }

    private <T extends ObjectType> GetObjectOpExecution<T> initializeExecution(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(GET_OBJECT)
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

        CacheSetAccessInfo<T> caches = cacheSetAccessInfoFactory.determine(type);
        return new GetObjectOpExecution<>(type, oid, options, result, trace, tracingLevel, prismContext, caches);
    }

    @NotNull
    private <T extends ObjectType> PrismObject<T> getObjectInternal(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.getObject(type, oid, options, parentResult);
        } finally {
            repoOpEnd(startTime);
        }
    }

    // returns directly returnable object (frozen if readonly, mutable if not readonly)
    private <T extends ObjectType> PrismObject<T> executeAndCache(GetObjectOpExecution<T> exec)
            throws SchemaException, ObjectNotFoundException {
        try {
            PrismObject<T> object = getObjectInternal(exec.type, exec.oid, exec.options, exec.result);
            PrismObject<T> immutable = toImmutable(object);

            if (!ObjectType.class.equals(exec.type)) {
                // Only cache object when read is performed by concrete type, reading by ObjectType
                // and caching may actually lead to caching incorrectly read object
                // if repository uses object class specific mappings
                cacheUpdater.storeImmutableObjectToObjectLocal(immutable, exec.caches);
                cacheUpdater.storeImmutableObjectToObjectGlobal(immutable);
                cacheUpdater.storeObjectToVersionGlobal(immutable, exec.caches.globalVersion);
                cacheUpdater.storeObjectToVersionLocal(immutable, exec.caches.localVersion);
            }
            if (exec.readOnly) {
                return immutable;
            } else {
                return object.cloneIfImmutable();
            }
        } catch (ObjectNotFoundException | SchemaException ex) {
            globalObjectCache.remove(exec.oid);
            globalVersionCache.remove(exec.oid);
            throw ex;
        }
    }

    private <T extends ObjectType> PrismObject<T> toImmutable(PrismObject<T> object) {
        if (object.isImmutable()) {
            return object;
        } else {
            PrismObject<T> clone = object.clone();
            clone.freeze();
            return clone;
        }
    }

    private boolean hasVersionChanged(Class<? extends ObjectType> objectType, String oid,
            GlobalCacheObjectValue<?> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
        try {
            // TODO shouldn't we record repoOpStart/repoOpEnd?
            String version = repositoryService.getVersion(objectType, oid, result);
            return !Objects.equals(version, object.getObjectVersion());
        } catch (ObjectNotFoundException | SchemaException ex) {
            globalObjectCache.remove(oid);
            globalVersionCache.remove(oid);
            throw ex;
        }
    }
}
