/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Search expression evaluator dealing with shadows - requires specific invalidation strategies.
 *
 * @author Pavol Mederly
 */
public class AssociationSearchExpressionEvaluatorCache
        extends AbstractSearchExpressionEvaluatorCache<
            PrismContainerValue<ShadowAssociationType>,
            PrismObject<ShadowType>,
            AssociationSearchQueryKey, AssociationSearchQueryResult> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationSearchExpressionEvaluatorCache.class);

    private static ThreadLocal<AssociationSearchExpressionEvaluatorCache> cacheInstances = new ThreadLocal<>();

    public static AbstractSearchExpressionEvaluatorCache getCache() {
        return cacheInstances.get();
    }
    public static AssociationSearchExpressionEvaluatorCache enterCache() {
        return enter(cacheInstances, AssociationSearchExpressionEvaluatorCache.class, LOGGER);
    }

    public static AssociationSearchExpressionEvaluatorCache exitCache() {
        return exit(cacheInstances, LOGGER);
    }

    @Override
    protected AssociationSearchQueryKey createQueryKey(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, ExpressionEvaluationContext params, PrismContext prismContext) {
        try {
            return new AssociationSearchQueryKey(type, query, searchStrategy, params, prismContext);
        } catch (Exception e) {     // TODO THIS IS REALLY UGLY HACK - query converter / prism serializer refuse to serialize some queries - should be fixed RSN!
            LoggingUtils.logException(LOGGER, "Couldn't create query key. Although this particular exception is harmless, please fix prism implementation!", e);
            return null;            // we "treat" it so that we simply pretend the entry is not in the cache and/or refuse to enter it into the cache
        }
    }

    @Override
    protected AssociationSearchQueryResult createQueryResult(List<PrismContainerValue<ShadowAssociationType>> resultList, List<PrismObject<ShadowType>> rawResultList) {
        return new AssociationSearchQueryResult(resultList, rawResultList);
    }

    // shadow may be null
    public void invalidate(PrismObject<ResourceType> resource, PrismObject<? extends ShadowType> shadow) {
        LOGGER.trace("Invalidating cache for resource = {}, shadow kind = {}",
                resource, shadow != null ? shadow.asObjectable().getKind() : "(no shadow)");

        if (resource == null || resource.getOid() == null) {    // shouldn't occur
            LOGGER.warn("No resource - invalidating all the cache");
            queries.clear();
            return;
        }
        String resourceOid = resource.getOid();
        ShadowKindType kind = null;
        if (shadow != null) {
            kind = shadow.asObjectable().getKind();
        }

        Set<Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult>> entries = queries.entrySet();
        Iterator<Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult> entry = iterator.next();
            if (matches(entry, resourceOid, kind)) {
                LOGGER.trace("Invalidating query key {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    private boolean matches(Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult> entry, String resourceOid, ShadowKindType kind) {
        AssociationSearchQueryResult result = entry.getValue();
        if (result.getResourceOid() == null) {
            return true;        // shouldn't occur
        }
        if (!result.getResourceOid().equals(resourceOid)) {
            return false;
        }
        if (kind == null || result.getKind() == null) {
            return true;
        }
        return result.getKind().equals(kind);
    }
}
