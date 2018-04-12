/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CacheObject<T extends ObjectType> {

    private PrismObject<T> object;

    private long lastVersionCheck;

    public CacheObject(PrismObject<T> object, long lastVersionCheck) {
        this.object = object;
        this.lastVersionCheck = lastVersionCheck;
    }

    public long getLastVersionCheck() {
        return lastVersionCheck;
    }

    public String getObjectOid() {
        return object.getOid();
    }

    public Class<T> getObjectType() {
        return object.getCompileTimeClass();
    }

    public String getObjectVersion() {
        return object.getVersion();
    }

    public PrismObject<T> getObject() {
        return object;
    }

    public void setLastVersionCheck(long lastVersionCheck) {
        this.lastVersionCheck = lastVersionCheck;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CacheObject{");
        sb.append("ttl=").append(lastVersionCheck);
        sb.append(", object=").append(object);
        sb.append('}');
        return sb.toString();
    }
}