/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
class GlobalCacheObjectValue<T extends ObjectType> extends AbstractGlobalCacheValue {

    @NotNull private final PrismObject<T> object;

    private long timeToCheckVersion;

    GlobalCacheObjectValue(@NotNull PrismObject<T> object, long timeToCheckVersion) {
        this.object = object;
        this.timeToCheckVersion = timeToCheckVersion;
    }

    long getTimeToCheckVersion() {
        return timeToCheckVersion;
    }

    String getObjectOid() {
        return object.getOid();
    }

    Class<?> getObjectType() {
        return object.getCompileTimeClass();
    }

    String getObjectVersion() {
        return object.getVersion();
    }

    @NotNull PrismObject<T> getObject() {
        return object;      // cloning is done in RepositoryCache
    }

    void setTimeToCheckVersion(long timeToCheckVersion) {
        this.timeToCheckVersion = timeToCheckVersion;
    }

    @Override
    public String toString() {
        return "GlobalCacheObjectValue{" + "timeToCheckVersion=" + timeToCheckVersion + " (" + (timeToCheckVersion-System.currentTimeMillis()) + " left)"
                + ", object=" + object + " (version " + object.getVersion() + ")}";
    }
}
