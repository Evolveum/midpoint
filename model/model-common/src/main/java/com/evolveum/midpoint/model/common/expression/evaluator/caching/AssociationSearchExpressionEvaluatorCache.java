/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.model.common.expression.evaluator.AbstractSearchExpressionEvaluator.ObjectFound;
import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.cache.CacheConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Search expression evaluator dealing with shadows - requires specific invalidation strategies.
 */
public class AssociationSearchExpressionEvaluatorCache
        extends AbstractSearchExpressionEvaluatorCache<
            ShadowAssociationValue,
            ShadowType,
            AssociationSearchQueryKey,
            AssociationSearchQueryResult> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationSearchExpressionEvaluatorCache.class);

    private static final ConcurrentHashMap<Thread, AssociationSearchExpressionEvaluatorCache> CACHE_INSTANCES =
            new ConcurrentHashMap<>();

    public static AssociationSearchExpressionEvaluatorCache getCache() {
        return CACHE_INSTANCES.get(Thread.currentThread());
    }

    public static AssociationSearchExpressionEvaluatorCache enterCache(CacheConfiguration configuration) {
        return enter(CACHE_INSTANCES, AssociationSearchExpressionEvaluatorCache.class, configuration, LOGGER);
    }

    public static AssociationSearchExpressionEvaluatorCache exitCache() {
        return exit(CACHE_INSTANCES, LOGGER);
    }

    @Override
    protected @NotNull AssociationSearchQueryKey createKey(
            Class<ShadowType> type,
            Collection<ObjectQuery> queries,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext eeCtx) {
        return new AssociationSearchQueryKey(type, queries, searchStrategy, eeCtx);
    }

    @Override
    protected AssociationSearchQueryResult createQueryResult(
            Collection<? extends ObjectFound<ShadowType, ShadowAssociationValue>> objectsFound) {
        return new AssociationSearchQueryResult(
                List.copyOf(Freezable.freezeAll(objectsFound)));
    }

    // shadow may be null
    public void invalidate(PrismObject<ResourceType> resource, PrismObject<? extends ShadowType> shadow) {
        LOGGER.trace("Invalidating cache for resource = {}, shadow kind = {}",
                resource, shadow != null ? shadow.asObjectable().getKind() : "(no shadow)");

        if (resource == null || resource.getOid() == null) { // shouldn't occur
            LOGGER.warn("No resource - invalidating all the cache");
            cachedSearches.clear();
            return;
        }
        String resourceOid = resource.getOid();
        ShadowKindType kind = null;
        if (shadow != null) {
            kind = shadow.asObjectable().getKind();
        }

        Set<Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult>> entries = cachedSearches.entrySet();
        Iterator<Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult> entry = iterator.next();
            if (matches(entry, resourceOid, kind)) {
                LOGGER.trace("Invalidating query key {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    private boolean matches(
            Map.Entry<AssociationSearchQueryKey, AssociationSearchQueryResult> entry,
            String resourceOid,
            ShadowKindType kind) {
        AssociationSearchQueryResult result = entry.getValue();
        if (result.getResourceOid() == null) {
            return true; // shouldn't occur
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
