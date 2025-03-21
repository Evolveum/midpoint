/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.cache.global.AbstractGlobalCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.log;
import static com.evolveum.midpoint.schema.GetOperationOptions.isReadOnly;
import static com.evolveum.midpoint.schema.SelectorOptions.findRootOptions;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastNormal;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CacheUseCategoryTraceType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CacheUseCategoryTraceType.MISS;

/**
 * Single execution of cached operation (`getObject`, `getVersion`, `searchObjects`, `searchObjectsIteratively`).
 *
 * It is hard to split responsibilities between this class and {@link CachedOpHandler}. But, generally, here
 * are auxiliary methods - in particular,
 *
 * 1. performance monitoring and logging ones,
 * 2. preparation of result to be returned - cloning as needed (plus recording it as needed).
 *
 * The {@link CachedOpHandler} contains the main caching-related logic.
 *
 * Instances of this type should be immutable (should contain only final fields).
 *
 * @see CachedOpHandler
 */
abstract class CachedOpExecution<
        RT extends RepositoryOperationTraceType,
        LC extends AbstractThreadLocalCache,
        GC extends AbstractGlobalCache,
        O extends ObjectType> {

    /**
     * Object type (input parameter).
     */
    @NotNull final Class<O> type;

    /**
     * Options (input parameter).
     */
    @Nullable final Collection<SelectorOptions<GetOperationOptions>> options;

    /**
     * Is the READ-ONLY option set?
     */
    final boolean readOnly;

    /**
     * Operation result - a child of parentResult input parameter.
     */
    @NotNull final OperationResult result;

    /**
     * Trace created for the operation result (if any).
     */
    @Nullable final RT trace;

    /**
     * Tracing level (if any).
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable final TracingLevelType tracingLevel;

    /**
     * Often-used indicator: true if tracing is enabled and at least on NORMAL level.
     */
    final boolean tracingAtLeastNormal;

    /**
     * Operation name e.g. getObject.
     */
    @NotNull private final String opName;

    /**
     * Access information for all the caches.
     */
    @NotNull final CacheSetAccessInfo<O> cachesInfo;

    /**
     * Access information for related local cache (object, version, query).
     */
    @NotNull final CacheAccessInfo<LC, O> localInfo;

    /**
     * Access information for related global cache (object, version, query).
     */
    @NotNull final CacheAccessInfo<GC, O> globalInfo;

    /**
     * Whether and how should be the cache(s) used. Driven *not* by the configuration of the caches, but by the operation itself
     * (object type, options). Evolved as a generalization of the `PassReason`.
     */
    @NotNull final CacheUseMode cacheUseMode;

    private final long started = System.currentTimeMillis();

    CachedOpExecution(@NotNull Class<O> type,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result,
            @NotNull CacheSetAccessInfo<O> cachesInfo,
            @NotNull CacheAccessInfo<LC, O> localInfo,
            @NotNull CacheAccessInfo<GC, O> globalInfo,
            @Nullable RT trace,
            @Nullable TracingLevelType tracingLevel,
            @NotNull CacheUseMode cacheUseMode,
            @NotNull String opName) {
        this.type = type;
        this.options = options;
        this.result = result;
        this.cachesInfo = cachesInfo;
        this.localInfo = localInfo;
        this.globalInfo = globalInfo;
        this.trace = trace;
        this.tracingLevel = tracingLevel;
        this.tracingAtLeastNormal = isAtLeastNormal(tracingLevel);
        this.readOnly = isReadOnly(findRootOptions(options));
        this.cacheUseMode = cacheUseMode;
        this.opName = opName;
    }

    void reportLocalAndGlobalPass() {
        if (localInfo.cache != null) {
            localInfo.cache.registerPass();
        }
        CachePerformanceCollector.INSTANCE.registerPass(getLocalCacheClass(), type, localInfo.statisticsLevel);
        CachePerformanceCollector.INSTANCE.registerPass(getGlobalCacheClass(), type, globalInfo.statisticsLevel);
        log("Cache (local/global): PASS:{} {} {}", localInfo.tracePass || globalInfo.tracePass, cacheUseMode,
                opName, getDescription());
        if (trace != null) {
            CacheUseTraceType use = cacheUseMode.toCacheUseForPass();
            trace.setLocalCacheUse(use);
            trace.setGlobalCacheUse(use);
        }
    }

    void reportLocalNotAvailable() {
        log("Cache (local): NULL {} {}", false, opName, getDescription());
        CachePerformanceCollector.INSTANCE.registerNotAvailable(getLocalCacheClass(), type, localInfo.statisticsLevel);
        if (trace != null) {
            trace.setLocalCacheUse(createUse(NOT_AVAILABLE));
        }
    }

    void reportLocalPass() {
        localInfo.cache.registerPass();
        CachePerformanceCollector.INSTANCE.registerPass(getLocalCacheClass(), type, localInfo.statisticsLevel);
        log("Cache: PASS:CONFIGURATION {} {}", localInfo.tracePass, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(PASS, cacheUseMode.getComment()));
        }
    }

    void reportLocalMiss() {
        localInfo.cache.registerMiss();
        CachePerformanceCollector.INSTANCE.registerMiss(getLocalCacheClass(), type, localInfo.statisticsLevel);
        log("Cache: MISS {} {}", localInfo.traceMiss, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(MISS));
        }
    }

    private void reportLocalHitNoClone() {
        localInfo.cache.registerHit();
        CachePerformanceCollector.INSTANCE.registerHit(getLocalCacheClass(), type, localInfo.statisticsLevel);
        log("Cache: HIT {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(HIT));
        }
    }

    private void reportLocalHitWithClone() {
        localInfo.cache.registerHit();
        CachePerformanceCollector.INSTANCE.registerHit(getLocalCacheClass(), type, localInfo.statisticsLevel);
        log("Cache: HIT(clone) {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(HIT));
        }
    }

    void reportLocalHit() {
        if (readOnly) {
            reportLocalHitNoClone();
        } else {
            reportLocalHitWithClone();
        }
    }

    void reportGlobalNotAvailable() {
        CachePerformanceCollector.INSTANCE.registerNotAvailable(getGlobalCacheClass(), type, globalInfo.statisticsLevel);
        log("Cache (global): NOT_AVAILABLE {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(NOT_AVAILABLE));
        }
    }

    void reportGlobalPass() {
        CachePerformanceCollector.INSTANCE.registerPass(getGlobalCacheClass(), type, globalInfo.statisticsLevel);
        log("Cache (global): PASS:CONFIGURATION {} {}", globalInfo.tracePass, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(PASS, cacheUseMode.getComment()));
        }
    }

    void reportGlobalHit() {
        CachePerformanceCollector.INSTANCE.registerHit(getGlobalCacheClass(), type, globalInfo.statisticsLevel);
        log("Cache (global): HIT {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(HIT));
        }
    }

    void reportGlobalMiss() {
        CachePerformanceCollector.INSTANCE.registerMiss(getGlobalCacheClass(), type, globalInfo.statisticsLevel);
        log("Cache (global): MISS {} {}", globalInfo.traceMiss, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(MISS));
        }
    }

    CacheUseTraceType createUse(CacheUseCategoryTraceType category) {
        return new CacheUseTraceType().category(category);
    }

    CacheUseTraceType createUse(CacheUseCategoryTraceType category, String comment) {
        return new CacheUseTraceType().category(category).comment(comment);
    }

    /** Prepares immutable object (presumably from the cache) for returning to the caller. */
    @NotNull <X extends Freezable & Cloneable> X toReturnValueFromImmutable(X immutable) {
        immutable.checkImmutable();
        if (readOnly) {
            return immutable; // caller is OK with immutable version
        } else {
            return CloneUtil.cloneCloneable(immutable); // caller expects a mutable copy
        }
    }

    /** Prepares mutable or immutable object (presumably from the repo service) for returning to the caller. */
    @NotNull <X extends Freezable & Cloneable> X toReturnValueFromAny(X any) {
        if (readOnly) {
            return any; // The returned object may or may not be immutable
        } else if (any.isImmutable()) {
            return CloneUtil.cloneCloneable(any); // caller expects a mutable copy
        } else {
            return any; // caller expects a mutable version
        }
    }

    long getAge() {
        return System.currentTimeMillis() - started;
    }

    abstract Object getDescription();
    abstract Class<LC> getLocalCacheClass();
    abstract Class<GC> getGlobalCacheClass();
}
