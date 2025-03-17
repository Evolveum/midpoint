/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;

import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;

import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.*;
import static com.evolveum.midpoint.schema.cache.CacheType.*;

/**
 * Creates {@link CacheSetAccessInfo} objects.
 */
@Component
public class CacheSetAccessInfoFactory {

    @Autowired GlobalObjectCache globalObjectCache;
    @Autowired GlobalVersionCache globalVersionCache;
    @Autowired GlobalQueryCache globalQueryCache;
    @Autowired CacheConfigurationManager cacheConfigurationManager;

    /** This is used when we don't have object class information. */
    <T extends ObjectType> CacheSetAccessInfo<T> determine(Class<T> type) {
        return determine(type, null, true);
    }

    <T extends ObjectType> CacheSetAccessInfo<T> determine(Class<T> type, QName objectClassName) {
        return determine(type, objectClassName, true);
    }

    <T extends ObjectType> CacheSetAccessInfo<T> determineExceptForQuery(Class<T> type, QName objectClassName) {
        return determine(type, objectClassName, false);
    }

    <T extends ObjectType> CacheSetAccessInfo<T> determineExceptForQuery(PrismObject<T> object) {
        return determine(object.getCompileTimeClass(), ShadowUtil.getObjectClassName(object), false);
    }

    private <T extends ObjectType> CacheSetAccessInfo<T> determine(Class<T> type, QName objectClassName, boolean alsoQuery) {

        CacheAccessInfo<GlobalObjectCache, T> globalObjectInfo = new CacheAccessInfo<>(
                globalObjectCache, globalObjectCache.getConfiguration(), type, objectClassName, globalObjectCache.isAvailable());

        CacheAccessInfo<GlobalVersionCache, T> globalVersionInfo = new CacheAccessInfo<>(
                globalVersionCache, globalVersionCache.getConfiguration(), type, objectClassName, globalVersionCache.isAvailable());

        CacheAccessInfo<GlobalQueryCache, T> globalQueryInfo =
                alsoQuery ?
                        new CacheAccessInfo<>(
                                globalQueryCache, globalQueryCache.getConfiguration(), type, objectClassName, globalQueryCache.isAvailable())
                        : CacheAccessInfo.createNotAvailable();

        LocalObjectCache localObjectCache = getLocalObjectCache();
        CacheAccessInfo<LocalObjectCache, T> localObjectInfo = localObjectCache != null ?
                new CacheAccessInfo<>(localObjectCache, localObjectCache.getConfiguration(), type, objectClassName,true) :
                new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_OBJECT_CACHE), type, objectClassName, false);

        LocalVersionCache localVersionCache = getLocalVersionCache();
        CacheAccessInfo<LocalVersionCache, T> localVersionInfo = localVersionCache != null ?
                new CacheAccessInfo<>(localVersionCache, localVersionCache.getConfiguration(), type, objectClassName, true) :
                new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_VERSION_CACHE), type, objectClassName, false);

        CacheAccessInfo<LocalQueryCache, T> localQueryInfo;
        if (alsoQuery) {
            LocalQueryCache localQueryCache = getLocalQueryCache();
            localQueryInfo = localQueryCache != null ?
                    new CacheAccessInfo<>(localQueryCache, localQueryCache.getConfiguration(), type, objectClassName, true) :
                    new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_QUERY_CACHE), type, objectClassName, false);
        } else {
            localQueryInfo = CacheAccessInfo.createNotAvailable();
        }

        return new CacheSetAccessInfo<>(
                localObjectInfo, localVersionInfo, localQueryInfo,
                globalObjectInfo, globalVersionInfo, globalQueryInfo);
    }
}
