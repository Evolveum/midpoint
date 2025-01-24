/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Information needed to access a specific cache.
 *
 * They relate to object of type `T`.
 */
class CacheAccessInfo<C, T extends ObjectType> {

    /**
     * Configuration of the cache as such.
     */
    private final CacheConfiguration cacheConfig;

    /**
     * Configuration of the cache, specific to given object type.
     */
    private final CacheConfiguration.CacheObjectTypeConfiguration typeConfig;

    /**
     * Is the cache available?
     */
    final boolean available;

    /**
     * Information if the cache supports given object type.
     */
    final boolean supports;

    /**
     * Statistics level on which we should report events related to the given cache + type of objects.
     */
    final CacheConfiguration.StatisticsLevel statisticsLevel;

    /**
     * Should we log MISS events for this cache/type?
     */
    final boolean traceMiss;

    /**
     * Should we log PASS events for this cache/type?
     */
    final boolean tracePass;

    /**
     * The cache itself.
     *
     * It is `null` if and only if {@link #available} is `false`.
     */
    final C cache;

    CacheAccessInfo(C cache, CacheConfiguration configuration, Class<T> type, boolean available) {
        this.available = available;
        this.cache = cache;

        if (configuration != null) {
            cacheConfig = configuration;
            typeConfig = configuration.getForObjectType(type);
            supports = configuration.supportsObjectType(type);
            statisticsLevel = CacheConfiguration.getStatisticsLevel(typeConfig, cacheConfig);
            traceMiss = CacheConfiguration.getTraceMiss(typeConfig, cacheConfig);
            tracePass = CacheConfiguration.getTracePass(typeConfig, cacheConfig);
        } else {
            cacheConfig = null;
            typeConfig = null;
            supports = false;
            statisticsLevel = null;
            traceMiss = false;
            tracePass = false;
        }
    }

    private CacheAccessInfo() {
        cacheConfig = null;
        typeConfig = null;
        available = false;
        supports = false;
        statisticsLevel = null;
        traceMiss = false;
        tracePass = false;
        cache = null;
    }

    static <C1, T1 extends ObjectType> CacheAccessInfo<C1, T1> createNotAvailable() {
        return new CacheAccessInfo<>();
    }

    boolean effectivelySupports() {
        return available && supports;
    }

    /**
     * Just to make static nullity checks happy.
     */
    @NotNull C getCache() {
        return Objects.requireNonNull(cache);
    }
}
