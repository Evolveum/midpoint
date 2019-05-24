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

    private static ThreadLocal<DefaultSearchExpressionEvaluatorCache> cacheInstances = new ThreadLocal<>();

    public static AbstractSearchExpressionEvaluatorCache getCache() {
        return cacheInstances.get();
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
