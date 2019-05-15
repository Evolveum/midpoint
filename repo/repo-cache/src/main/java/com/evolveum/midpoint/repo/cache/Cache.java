/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.AbstractCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class Cache extends AbstractCache {

    private static final Trace LOGGER = TraceManager.getTrace(Cache.class);

    private Map<String, PrismObject<? extends ObjectType>> objects = new HashMap<>();
    private Map<String, String> versions = new HashMap<>();
    private Map<QueryKey, SearchResultList> queries = new HashMap<>();

    public int size() {
        return objects.size() + versions.size() + queries.size();
    }

    @Override
    public String description() {
        return "O:"+objects.size()+", V:"+versions.size()+", Q:"+queries.size();
    }

    public PrismObject<? extends ObjectType> getObject(String oid) {
        return objects.get(oid);
    }

    public void putObject(String oid, PrismObject<? extends ObjectType> object) {
        objects.put(oid, object);
        versions.put(oid, object.getVersion());
    }

    public void removeObject(String oid) {
        objects.remove(oid);
        versions.remove(oid);
    }

    public <T extends ObjectType> void putQueryResult(Class<T> type, ObjectQuery query, SearchResultList searchResultList, PrismContext prismContext) {
        QueryKey queryKey = createQueryKey(type, query, prismContext);
        if (queryKey != null) {     // TODO BRUTAL HACK
            queries.put(queryKey, searchResultList);
        }
    }

    public <T extends ObjectType> void clearQueryResults(Class<T> type, Object additionalInfo, PrismContext prismContext,
            MatchingRuleRegistry matchingRuleRegistry) {
        // TODO implement more efficiently

        ChangeDescription change;
        if (!LookupTableType.class.equals(type) && !AccessCertificationCampaignType.class.equals(type)) {
            change = ChangeDescription.getFrom(additionalInfo, prismContext);
        } else {
            change = null;      // these objects are tricky to query -- it's safer to evict their queries completely
        }

        int removed = 0;
        Iterator<Map.Entry<QueryKey, SearchResultList>> iterator = queries.entrySet().iterator();
        while (iterator.hasNext()) {
            QueryKey queryKey = iterator.next().getKey();
            if (queryKey.getType().isAssignableFrom(type) && (change == null || change.mayAffect(queryKey, matchingRuleRegistry))) {
                LOGGER.info("Removing query for type={}, change={}: {}", type, change, queryKey.getQuery());
                iterator.remove();
                removed++;
            }
        }
        LOGGER.info("Removed {} query result entries of type {}", removed, type);
    }

    public SearchResultList getQueryResult(Class<? extends ObjectType> type, ObjectQuery query, PrismContext prismContext) {
        return queries.get(createQueryKey(type, query, prismContext));
    }

    private QueryKey createQueryKey(Class<? extends ObjectType> type, ObjectQuery query, PrismContext prismContext) {
        return new QueryKey(type, query);
    }

    public String getObjectVersion(String oid) {
        return versions.get(oid);
    }

    public void putObjectVersion(String oid, String version) {
        versions.put(oid, version);
    }
}
