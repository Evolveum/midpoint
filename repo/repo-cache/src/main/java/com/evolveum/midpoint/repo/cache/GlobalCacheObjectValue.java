/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GlobalCacheObjectValue<T extends ObjectType> {

    private PrismObject<T> object;

    private long timeToLive;

    public GlobalCacheObjectValue(PrismObject<T> object, long timeToLive) {
        this.object = object;
        this.timeToLive = timeToLive;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public String getObjectOid() {
        return object.getOid();
    }

    public Class<?> getObjectType() {
        return object.getCompileTimeClass();
    }

    public String getObjectVersion() {
        return object.getVersion();
    }

    public PrismObject<T> getObject() {
        return object;      // cloning is done in RepositoryCache
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Override
    public String toString() {
        return "CacheObject{" + "ttl=" + timeToLive
                + ", object=" + object
                + '}';
    }
}
