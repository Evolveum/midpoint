/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.util.caching.CacheConfiguration;

/**
 * Information needed to access a specific cache.
 */
class CacheAccessInfo<C> {

    /**
     * Configuration of the cache as such.
     */
    final CacheConfiguration cacheConfig;

    /**
     * Configuration of the cache, specific to given object type.
     */
    final CacheConfiguration.CacheObjectTypeConfiguration typeConfig;

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
     * It is null if and only if available is false.
     */
    final C cache;

    CacheAccessInfo(C cache, CacheConfiguration configuration, Class<?> type, boolean available) {
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
}
