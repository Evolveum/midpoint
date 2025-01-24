/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import org.cache2k.expiry.Expiry;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CacheConfiguration.CacheObjectTypeConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Superclass for global caches handling objects, versions, and queries.
 */
public abstract class AbstractGlobalCache {

    static final int DEFAULT_TIME_TO_LIVE = 60; // see also default-caching-profile.xml in resources

    @Autowired protected CacheConfigurationManager configurationManager;
    @Autowired protected PrismContext prismContext;

    public CacheConfiguration getConfiguration() {
        return configurationManager.getConfiguration(getCacheType());
    }

    public CacheObjectTypeConfiguration getConfiguration(Class<?> type) {
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
            return -1;
        }
    }

    long getExpiryTime(Class<?> type) {
        CacheObjectTypeConfiguration configuration = getConfiguration(type);
        if (configuration == null) {
            return Expiry.NOW;
        } else if (configuration.getEffectiveTimeToLive() != null) {
            return System.currentTimeMillis() + configuration.getEffectiveTimeToLive() * 1000L;
        } else {
            return System.currentTimeMillis() + DEFAULT_TIME_TO_LIVE * 1000L;
        }
    }

    protected abstract CacheType getCacheType();

    public <T extends ObjectType> boolean hasClusterwideInvalidationFor(Class<T> type) {
        CacheConfiguration configuration = getConfiguration();
        return configuration != null && configuration.isClusterwideInvalidation(type);
    }

    public <T extends ObjectType> boolean shouldDoSafeRemoteInvalidationFor(Class<T> type) {
        CacheConfiguration configuration = getConfiguration();
        return configuration != null && configuration.isSafeRemoteInvalidation(type);
    }

    public abstract void clear();
}
