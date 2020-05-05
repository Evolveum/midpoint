/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GlobalCacheObjectValue<T extends ObjectType> extends AbstractGlobalCacheValue {

    @NotNull private final PrismObject<T> object;

    private volatile long checkVersionTime;

    public GlobalCacheObjectValue(@NotNull PrismObject<T> object, long checkVersionTime) {
        this.object = object;
        this.checkVersionTime = checkVersionTime;
    }

    String getObjectOid() {
        return object.getOid();
    }

    public Class<? extends ObjectType> getObjectType() {
        return object.getCompileTimeClass();
    }

    public String getObjectVersion() {
        return object.getVersion();
    }

    @NotNull
    public PrismObject<T> getObject() {
        return object;      // cloning is done in RepositoryCache
    }

    public void setCheckVersionTime(long checkVersionTime) {
        this.checkVersionTime = checkVersionTime;
    }

    public boolean shouldCheckVersion() {
        return System.currentTimeMillis() > checkVersionTime;
    }

    @Override
    public String toString() {
        return "GlobalCacheObjectValue{" + "checkVersionTime=" + checkVersionTime + " (" + (checkVersionTime -System.currentTimeMillis()) + " left)"
                + ", object=" + object + " (version " + object.getVersion() + ")}";
    }
}
