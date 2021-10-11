/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CacheConfiguration.CacheObjectTypeConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.cache2k.expiry.Expiry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public abstract class AbstractGlobalCache {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractGlobalCache.class);

    static final int DEFAULT_TIME_TO_LIVE = 60;                     // see also default-caching-profile.xml in resources

    @Autowired protected CacheConfigurationManager configurationManager;
    @Autowired protected PrismContext prismContext;

    boolean supportsObjectType(Class<?> type) {
        CacheType cacheType = getCacheType();
        CacheConfiguration configuration = configurationManager.getConfiguration(cacheType);
        if (configuration != null) {
            return configuration.supportsObjectType(type);
        } else {
            LOGGER.warn("Global cache configuration for {} not found", cacheType);
            return false;
        }
    }

    protected CacheConfiguration getConfiguration() {
        return configurationManager.getConfiguration(getCacheType());
    }

    protected CacheObjectTypeConfiguration getConfiguration(Class<?> type) {
        CacheConfiguration configuration = getConfiguration();
        return configuration != null ? configuration.getForObjectType(type) : null;
    }

    long getCapacity() {
        CacheConfiguration configuration = configurationManager.getConfiguration(getCacheType());
        if (configuration == null) {
            return 0;
        } else if (configuration.getMaxSize() != null) {
            return configuration.getMaxSize();
        } else {
            return Long.MAX_VALUE;
        }
    }

    protected long getExpiryTime(Class<?> type) {
        CacheObjectTypeConfiguration configuration = getConfiguration(type);
        if (configuration == null) {
            return Expiry.NO_CACHE;
        } else if (configuration.getEffectiveTimeToLive() != null) {
            return System.currentTimeMillis() + configuration.getEffectiveTimeToLive() * 1000L;
        } else {
            return System.currentTimeMillis() + DEFAULT_TIME_TO_LIVE * 1000L;
        }
    }

    protected abstract CacheType getCacheType();

    <T extends ObjectType> boolean isClusterwideInvalidation(Class<T> type) {
        CacheConfiguration configuration = getConfiguration();
        return configuration != null && configuration.isClusterwideInvalidation(type);
    }

    <T extends ObjectType> boolean isSafeRemoteInvalidation(Class<T> type) {
        CacheConfiguration configuration = getConfiguration();
        return configuration != null && configuration.isSafeRemoteInvalidation(type);
    }

    public abstract void clear();
}
