/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.model.common.expression.evaluator;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.caching.AbstractCache;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EXPERIMENTAL. Quickly extracted from repository cache Cache implementation.
 *
 * Currently please use only for shadows.
 *
 * @author Pavol Mederly
 */
public class AbstractSearchExpressionEvaluatorCache extends AbstractCache {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class);

    private static ThreadLocal<AbstractSearchExpressionEvaluatorCache> cacheInstances = new ThreadLocal<>();

    public static AbstractSearchExpressionEvaluatorCache getCache() {
        return cacheInstances.get();
    }

    public static AbstractSearchExpressionEvaluatorCache enterCache() {
        return enter(cacheInstances, AbstractSearchExpressionEvaluatorCache.class, LOGGER);
    }

    public static AbstractSearchExpressionEvaluatorCache exitCache() {
        return exit(cacheInstances, LOGGER);
    }

    private Invalidator invalidator;

    public static void init() {
    }

    private Map<QueryKey, List> queries = new HashMap<>();
    private Map<String, Map<ShadowKindType, Set<QueryKey>>> keysByCoordinates = new HashMap<>();        // auxiliary structure to enable fast invalidation of shadows

    @Override
    public String description() {
        return "Q:"+queries.size();
    }

    public Invalidator getInvalidator() {
        return invalidator;
    }

    public void setInvalidator(Invalidator invalidator) {
        this.invalidator = invalidator;
    }

    // resultList must not be empty
    public <T extends ObjectType> void putQueryResult(Class<T> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, Object qualifier, List resultList, ShadowKindType kind, PrismContext prismContext) {
        QueryKey queryKey = createQueryKey(type, query, searchStrategy, qualifier, prismContext);
        if (queryKey != null) {     // TODO BRUTAL HACK
            queries.put(queryKey, resultList);
            if (ShadowType.class.isAssignableFrom(type)) {
                categorizeQueryKey(query, resultList, queryKey, kind);
            }
        }
    }

    private void categorizeQueryKey(ObjectQuery query, List resultList, QueryKey queryKey, ShadowKindType defaultKind) {
        if (query == null) {
            throw new IllegalStateException("Null query in search expression evaluator");
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            throw new IllegalStateException("Null search filter in search expression evaluator");
        }
        filter = ObjectQueryUtil.simplify(filter);

        ResourceShadowDiscriminator coordinates;
        try {
            coordinates = ObjectQueryUtil.getCoordinates(filter);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't parse search filter query", e);
        }

        String resourceOid = coordinates.getResourceOid();
        ShadowKindType kind = coordinates.getKind();
        if (kind == null) {
            kind = defaultKind;
        }
        // TODO determine kind somehow...
//        if (kind == null && !resultList.isEmpty()) {
//            // let's take the kind from the resultList
//            kind = ((PrismObject<ShadowType>) resultList.get(0)).asObjectable().getKind();
//        }

        Map<ShadowKindType, Set<QueryKey>> resourceContent = keysByCoordinates.get(resourceOid);
        if (resourceContent == null) {
            resourceContent = new HashMap<>();
            keysByCoordinates.put(resourceOid, resourceContent);
        }

        Set<QueryKey> keysForKind = resourceContent.get(kind);
        if (keysForKind == null) {
            keysForKind = new HashSet<>();
            resourceContent.put(kind, keysForKind);
        }

        keysForKind.add(queryKey);
    }

    private QueryKey createQueryKey(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, Object qualifier, PrismContext prismContext) {
        try {
            return new QueryKey(type, query, searchStrategy, qualifier, prismContext);
        } catch (Exception e) {     // TODO THIS IS REALLY UGLY HACK - query converter / prism serializer refuse to serialize some queries - should be fixed RSN!
            LoggingUtils.logException(LOGGER, "Couldn't create query key. Although this particular exception is harmless, please fix prism implementation!", e);
            return null;            // we "treat" it so that we simply pretend the entry is not in the cache and/or refuse to enter it into the cache
        }
    }

    public List getQueryResult(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, Object qualifier, PrismContext prismContext) {
        QueryKey queryKey = createQueryKey(type, query, searchStrategy, qualifier, prismContext);
        if (queryKey != null) {         // TODO BRUTAL HACK
            return queries.get(queryKey);
        } else {
            return null;
        }
    }

    // shadow may be null
    public void invalidate(PrismObject<ResourceType> resource, PrismObject<? extends ShadowType> shadow) {
        if (resource == null || resource.getOid() == null) {    // shouldn't occur
            LOGGER.warn("No resource - invalidating all the cache");
            invalidateAll();
            return;
        }
        String resourceOid = resource.getOid();
        ShadowKindType kind = null;
        if (shadow != null) {
            kind = shadow.asObjectable().getKind();
        }

        Map<ShadowKindType, Set<QueryKey>> resourceContent = keysByCoordinates.get(resourceOid);
        if (resourceContent == null) {
            return;
        }

        if (kind == null) {
            for (Map.Entry<ShadowKindType, Set<QueryKey>> entry : resourceContent.entrySet()) {
                invalidateKind(entry.getValue());
            }
        } else {
            invalidateKind(resourceContent.get(null));
            invalidateKind(resourceContent.get(kind));
        }
    }

    private void invalidateKind(Set<QueryKey> keys) {
        if (keys == null) {
            return;
        }
        for (QueryKey key : keys) {
            LOGGER.info("Invalidating query key {}", key);
            queries.remove(key);
        }
        keys.clear();
    }

    private void invalidateAll() {
        LOGGER.info("Invalidating whole cache");
        queries.clear();
        keysByCoordinates.clear();
    }

    public class QueryKey {

        private Class<? extends ObjectType> type;
        private QueryType query;
        private ObjectSearchStrategyType searchStrategy;
        private Object qualifier;

        public <T extends ObjectType> QueryKey(Class<T> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, Object qualifier, PrismContext prismContext) {
            this.type = type;
            try {
                this.query = query != null ? QueryJaxbConvertor.createQueryType(query, prismContext) : null;
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
            this.searchStrategy = searchStrategy;
            this.qualifier = qualifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            QueryKey queryKey = (QueryKey) o;

            if (query != null ? !query.equals(queryKey.query) : queryKey.query != null) return false;
            if (type != null ? !type.equals(queryKey.type) : queryKey.type != null) return false;
            if (searchStrategy != null ? !searchStrategy.equals(queryKey.searchStrategy) : queryKey.searchStrategy != null) return false;
            if (qualifier != null ? !qualifier.equals(queryKey.qualifier) : queryKey.qualifier != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (query != null ? query.hashCode() : 0);
            result = 31 * result + (searchStrategy != null ? searchStrategy.hashCode() : 0);
            result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
            return result;
        }

        public Class<? extends ObjectType> getType() {
            return type;
        }

        @Override
        public String toString() {
            return "QueryKey{" +
                    "type=" + type +
                    ", query=" + query +
                    ", searchStrategy=" + searchStrategy +
                    ", qualifier=" + qualifier +
                    '}';
        }
    }

    public interface Invalidator {

    }

}
