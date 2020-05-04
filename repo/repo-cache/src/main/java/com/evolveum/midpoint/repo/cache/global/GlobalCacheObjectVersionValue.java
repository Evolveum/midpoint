/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
