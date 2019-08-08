/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class GlobalCacheObjectVersionValue<T extends ObjectType> {

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