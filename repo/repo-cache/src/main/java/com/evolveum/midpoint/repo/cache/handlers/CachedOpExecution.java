/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismContext;
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
 * Single execution of cached operation (getObject, getVersion, searchObjects, searchObjectsIteratively).
 *
 * It is hard to split responsibilities between this class and CachedOpHandler. But, generally, here
 * are auxiliary methods - in particular,
 * 1) performance monitoring and logging ones,
 * 2) preparation of result to be returned - cloning as needed (plus recording it as needed).
 *
 * The CachedOpHandler contains the main caching-related logic.
 *
 * Instances of this type should be immutable (should contain only final fields).
 */
abstract class CachedOpExecution<RT extends RepositoryOperationTraceType, LC extends AbstractThreadLocalCache,
        GC extends AbstractGlobalCache, O extends ObjectType> {

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
    @NotNull final CacheSetAccessInfo<O> caches;

    /**
     * Access information for related local cache (object, version, query).
     */
    @NotNull final CacheAccessInfo<LC, O> local;

    /**
     * Access information for related global cache (object, version, query).
     */
    @NotNull final CacheAccessInfo<GC, O> global;

    final long started = System.currentTimeMillis();

    @NotNull final PrismContext prismContext;

    CachedOpExecution(@NotNull Class<O> type,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result,
            @NotNull CacheSetAccessInfo<O> caches,
            @NotNull CacheAccessInfo<LC, O> local,
            @NotNull CacheAccessInfo<GC, O> global,
            @Nullable RT trace,
            @Nullable TracingLevelType tracingLevel,
            @NotNull PrismContext prismContext,
            @NotNull String opName) {
        this.type = type;
        this.options = options;
        this.result = result;
        this.caches = caches;
        this.local = local;
        this.global = global;
        this.trace = trace;
        this.tracingLevel = tracingLevel;
        this.tracingAtLeastNormal = isAtLeastNormal(tracingLevel);
        this.readOnly = isReadOnly(findRootOptions(options));
        this.prismContext = prismContext;
        this.opName = opName;
    }

    void reportLocalAndGlobalPass(PassReason passReason) {
        if (local.cache != null) {
            local.cache.registerPass();
        }
        CachePerformanceCollector.INSTANCE.registerPass(getLocalCacheClass(), type, local.statisticsLevel);
        CachePerformanceCollector.INSTANCE.registerPass(getGlobalCacheClass(), type, global.statisticsLevel);
        log("Cache (local/global): PASS:{} {} {}", local.tracePass || global.tracePass, passReason,
                opName, getDescription());
        if (trace != null) {
            CacheUseTraceType use = passReason.toCacheUse();
            trace.setLocalCacheUse(use);
            trace.setGlobalCacheUse(use);
        }
    }

    void reportLocalNotAvailable() {
        log("Cache (local): NULL {} {}", false, opName, getDescription());
        CachePerformanceCollector.INSTANCE.registerNotAvailable(getLocalCacheClass(), type, local.statisticsLevel);
        if (trace != null) {
            trace.setLocalCacheUse(createUse(NOT_AVAILABLE));
        }
    }

    void reportLocalPass() {
        local.cache.registerPass();
        CachePerformanceCollector.INSTANCE.registerPass(getLocalCacheClass(), type, local.statisticsLevel);
        log("Cache: PASS:CONFIGURATION {} {}", local.tracePass, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(PASS, "configuration"));
        }
    }

    void reportLocalMiss() {
        local.cache.registerMiss();
        CachePerformanceCollector.INSTANCE.registerMiss(getLocalCacheClass(), type, local.statisticsLevel);
        log("Cache: MISS {} {}", local.traceMiss, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(MISS));
        }
    }

    private void reportLocalHitNoClone() {
        local.cache.registerHit();
        CachePerformanceCollector.INSTANCE.registerHit(getLocalCacheClass(), type, local.statisticsLevel);
        log("Cache: HIT {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setLocalCacheUse(createUse(HIT));
        }
    }

    private void reportLocalHitWithClone() {
        local.cache.registerHit();
        CachePerformanceCollector.INSTANCE.registerHit(getLocalCacheClass(), type, local.statisticsLevel);
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
        CachePerformanceCollector.INSTANCE.registerNotAvailable(getGlobalCacheClass(), type, global.statisticsLevel);
        log("Cache (global): NOT_AVAILABLE {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(NOT_AVAILABLE));
        }
    }

    void reportGlobalPass() {
        reportGlobalPass("configuration");
    }

    void reportGlobalPass(@NotNull PassReason reason) {
        reportGlobalPass(reason.toCacheUse().getComment());
    }

    private void reportGlobalPass(String comment) {
        CachePerformanceCollector.INSTANCE.registerPass(getGlobalCacheClass(), type, global.statisticsLevel);
        log("Cache (global): PASS:CONFIGURATION {} {}", global.tracePass, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(PASS, comment));
        }
    }

    void reportGlobalHit() {
        CachePerformanceCollector.INSTANCE.registerHit(getGlobalCacheClass(), type, global.statisticsLevel);
        log("Cache (global): HIT {} {}", false, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(HIT));
        }
    }

    void reportGlobalMiss() {
        CachePerformanceCollector.INSTANCE.registerMiss(getGlobalCacheClass(), type, global.statisticsLevel);
        log("Cache (global): MISS {} {}", global.traceMiss, opName, getDescription());
        if (trace != null) {
            trace.setGlobalCacheUse(createUse(MISS));
        }
    }

    CacheUseTraceType createUse(CacheUseCategoryTraceType category) {
        return new CacheUseTraceType(prismContext).category(category);
    }

    CacheUseTraceType createUse(CacheUseCategoryTraceType category, String comment) {
        return new CacheUseTraceType(prismContext).category(category).comment(comment);
    }

    abstract String getDescription();
    abstract Class<LC> getLocalCacheClass();
    abstract Class<GC> getGlobalCacheClass();
}
