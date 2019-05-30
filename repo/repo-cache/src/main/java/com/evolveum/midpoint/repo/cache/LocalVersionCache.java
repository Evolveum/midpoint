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

import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class LocalVersionCache extends AbstractThreadLocalCache {
    private Map<String, String> data = new HashMap<>();

    public String get(String oid) {
        return data.get(oid);
    }

    public void put(String oid, String version) {
        data.put(oid, version);
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "V:" + data.size();
    }
}
