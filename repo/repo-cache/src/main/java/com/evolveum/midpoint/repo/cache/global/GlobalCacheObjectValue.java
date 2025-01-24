/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import com.evolveum.midpoint.repo.cache.values.CachedObjectValue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Objects;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GlobalCacheObjectValue<T extends ObjectType> extends AbstractGlobalCacheValue implements CachedObjectValue<T> {

    @NotNull private final PrismObject<T> object;

    private volatile long checkVersionTime;

    private final boolean complete;

    public GlobalCacheObjectValue(@NotNull PrismObject<T> object, long checkVersionTime, boolean complete) {
        this.object = object;
        this.checkVersionTime = checkVersionTime;
        this.complete = complete;
    }

    @NotNull String getObjectOid() {
        return Objects.requireNonNull(object.getOid());
    }

    public @NotNull Class<? extends ObjectType> getObjectType() {
        return Objects.requireNonNull(object.getCompileTimeClass());
    }

    public String getObjectVersion() {
        return object.getVersion();
    }

    @Override
    public @NotNull PrismObject<T> getObject() {
        return object;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    public void setCheckVersionTime(long checkVersionTime) {
        this.checkVersionTime = checkVersionTime;
    }

    public boolean shouldCheckVersion() {
        return System.currentTimeMillis() > checkVersionTime;
    }

    @Override
    public String toString() {
        return "GlobalCacheObjectValue{"
                + "checkVersionTime=" + checkVersionTime + " (" + (checkVersionTime - System.currentTimeMillis()) + " ms left)"
                + ", object=" + object
                + " (version " + object.getVersion() + ")"
                + ", complete=" + complete
                + "}";
    }
}
