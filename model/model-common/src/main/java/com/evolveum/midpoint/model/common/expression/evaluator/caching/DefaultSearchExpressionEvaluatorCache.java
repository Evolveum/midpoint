/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default search expression evaluator cache.
 *
 * Unfinished -- cache invalidation is missing.
 *
 * Currently unused. The (almost) equivalent functionality is provided by global and local repo query cache.
 *
 * TODO eventually remove
 */
public class DefaultSearchExpressionEvaluatorCache
        extends AbstractSearchExpressionEvaluatorCache<
            PrismValue,
            PrismObject,
            QueryKey, QueryResult> {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultSearchExpressionEvaluatorCache.class);

    private static ConcurrentHashMap<Thread, DefaultSearchExpressionEvaluatorCache> cacheInstances = new ConcurrentHashMap<>();

    public static AbstractSearchExpressionEvaluatorCache getCache() {
        return cacheInstances.get(Thread.currentThread());
    }

    public static void enterCache(CacheConfiguration configuration) {
        enter(cacheInstances, DefaultSearchExpressionEvaluatorCache.class, configuration, LOGGER);
    }

    public static void exitCache() {
        exit(cacheInstances, LOGGER);
    }

    @Override
    protected QueryKey createQueryKey(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, ExpressionEvaluationContext params, PrismContext prismContext) {
        try {
            return new QueryKey(type, query, searchStrategy, prismContext);
        } catch (Exception e) {     // TODO THIS IS REALLY UGLY HACK - query converter / prism serializer refuse to serialize some queries - should be fixed RSN!
            LoggingUtils.logException(LOGGER, "Couldn't create query key. Although this particular exception is harmless, please fix prism implementation!", e);
            return null;            // we "treat" it so that we simply pretend the entry is not in the cache and/or refuse to enter it into the cache
        }
    }

    @Override
    protected QueryResult createQueryResult(List<PrismValue> resultList, List<PrismObject> rawResultList) {
        //noinspection unchecked
        return new QueryResult(resultList);
    }

}
