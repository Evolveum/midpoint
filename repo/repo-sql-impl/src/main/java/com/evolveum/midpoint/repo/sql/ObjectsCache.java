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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.Objects;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectsCache {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectsCache.class);

    private static final Set<Class<? extends ObjectType>> SUPPORTED_TYPES;

    static {
        Set<Class<? extends ObjectType>> set = new HashSet<>();
        set.add(ConnectorType.class);
        set.add(ObjectTemplateType.class);
        set.add(SecurityPolicyType.class);
        set.add(SystemConfigurationType.class);
        set.add(ValuePolicyType.class);

        SUPPORTED_TYPES = Collections.unmodifiableSet(set);
    }

    private long maxTTL;

    private Map<CacheKey, CacheObject> cache = new HashMap<>();

    private SqlRepositoryServiceImpl repositoryService;

    public ObjectsCache(SqlRepositoryServiceImpl repositoryService) {
        this.repositoryService = repositoryService;

        maxTTL = repositoryService.getConfiguration().getCacheMaxTTL() * 1000;
    }

    public <T extends ObjectType> boolean supportsCaching(
            Class<T> type, Collection<SelectorOptions<GetOperationOptions>> options) {

        if (maxTTL <= 0) {
            return false;
        }

        if (!SUPPORTED_TYPES.contains(type)) {
            return false;
        }

        if (options != null && !options.isEmpty()) {    //todo support probably raw flag
            return false;
        }

        return true;
    }

    public <T extends ObjectType> PrismObject<T> getObject(
            Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        if (!supportsCaching(type, options)) {
            return null;
        }

        CacheKey key = new CacheKey(type, oid);
        CacheObject<T> cacheObject = cache.get(key);
        if (cacheObject == null) {
            return reloadObject(key, options, result);
        }

        if (!shouldCheckVersion(cacheObject)) {
            LOGGER.trace("Cache HIT {}", key);
            return cacheObject.getObject();
        }

        if (hasVersionChanged(key, cacheObject, result)) {
            return reloadObject(key, options, result);
        }

        // version matches, renew ttl
        cacheObject.setTimeToLive(System.currentTimeMillis() + maxTTL);

        LOGGER.trace("Cache HIT, version check {}", key);
        return cacheObject.getObject();
    }

    public <T extends ObjectType> void removeObject(Class<T> type, String oid) {
        Validate.notNull(type, "Type must not be null");
        Validate.notNull(oid, "Oid must not be null");

        cache.remove(new CacheKey(type, oid));
    }

    private boolean hasVersionChanged(CacheKey key, CacheObject object, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        try {
            String version = repositoryService.getVersion(object.getObjectType(), object.getObjectOid(), result);

            return !Objects.equals(version, object.getObjectVersion());
        } catch (ObjectNotFoundException | SchemaException ex) {
            cache.remove(key);

            throw ex;
        }
    }

    private boolean shouldCheckVersion(CacheObject object) {
        return object.getTimeToLive() < System.currentTimeMillis();
    }

    private <T extends ObjectType> PrismObject<T> reloadObject(
            CacheKey key, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        LOGGER.trace("Cache MISS {}", key);

        try {
            PrismObject object = repositoryService.getObjectInternal(key.getType(), key.getOid(), options, result);

            long ttl = System.currentTimeMillis() + maxTTL;
            CacheObject<T> cacheObject = new CacheObject<>(object, ttl);

            cache.put(key, cacheObject);

            return cacheObject.getObject();
        } catch (ObjectNotFoundException | SchemaException ex) {
            cache.remove(key);

            throw ex;
        }
    }

    private static class CacheKey {

        private Class<? extends ObjectType> type;
        private String oid;

        public CacheKey(Class<? extends ObjectType> type, String oid) {
            this.type = type;
            this.oid = oid;
        }

        public Class<? extends ObjectType> getType() {
            return type;
        }

        public String getOid() {
            return oid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (type != null ? !type.equals(cacheKey.type) : cacheKey.type != null) return false;
            return oid != null ? oid.equals(cacheKey.oid) : cacheKey.oid == null;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (oid != null ? oid.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CacheKey{");
            sb.append(type.getSimpleName());
            sb.append("[").append(oid).append("]}");
            return sb.toString();
        }
    }

    private static class CacheObject<T extends ObjectType> {

        private PrismObject<T> object;

        private long timeToLive;

        public CacheObject(PrismObject<T> object, long timeToLive) {
            this.object = object;
            this.timeToLive = timeToLive;
        }

        public long getTimeToLive() {
            return timeToLive;
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
            return object.clone();
        }

        public void setTimeToLive(long timeToLive) {
            this.timeToLive = timeToLive;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CacheObject{");
            sb.append("ttl=").append(timeToLive);
            sb.append(", object=").append(object);
            sb.append('}');
            return sb.toString();
        }
    }
}
