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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class LocalObjectCache extends AbstractThreadLocalCache {

    private Map<String, PrismObject<? extends ObjectType>> data = new HashMap<>();

    public PrismObject<? extends ObjectType> get(String oid) {
        return data.get(oid);
    }

    public void put(String oid, PrismObject<? extends ObjectType> object) {
        data.put(oid, object);
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "O:" + data.size();
    }
}
