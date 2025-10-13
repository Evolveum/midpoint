/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.global;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class GlobalCacheObjectVersionValue<T extends ObjectType> extends AbstractGlobalCacheValue {

    @NotNull private final Class<?> objectType;
    private final String version;

    GlobalCacheObjectVersionValue(@NotNull Class<?> objectType, String version) {
        this.objectType = objectType;
        this.version = version;
    }

    @NotNull
    public Class<?> getObjectType() {
        return objectType;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "GlobalCacheObjectVersionValue{" +
                "objectType=" + objectType +
                ", version='" + version + '\'' +
                '}';
    }
}
