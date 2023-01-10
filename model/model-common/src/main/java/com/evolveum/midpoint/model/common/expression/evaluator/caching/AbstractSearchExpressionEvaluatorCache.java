/*
 * Copyright (c) 2010-2017 Evolveum and contributors
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
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for search expression-based evaluators.
 *
 * It needs to be customized in the following ways:
 *
 * - what is in the query key besides basic data - namely, what parts of {@link ExpressionEvaluationContext} should
 * be part of the key?
 * - should we store anything in addition to the resulting list of values? E.g. shadow kind in case of `associationTargetSearch`
 * that is used for invalidation?
 *
 * @param <V> type of cached result items
 * @param <O> type of raw values that we are searching for
 * @param <QK> query key
 * @param <QR> query result
 *
 * After refactoring, this class contains almost nothing ;) Consider removing it altogether.
 */
public abstract class AbstractSearchExpressionEvaluatorCache<
        V extends PrismValue,
        O extends ObjectType,
        QK extends QueryKey,
        QR extends QueryResult<V>>
        extends AbstractThreadLocalCache {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class.getName() + ".content");

    // Making client's life easier - if it stores the cache in ThreadLocal variable and needs any other cache-related
    // information (e.g. custom invalidator), it does not need to create another ThreadLocal for that.
    private Object clientContextInformation;

    public Object getClientContextInformation() {
        return clientContextInformation;
    }

    public void setClientContextInformation(Object clientContextInformation) {
        this.clientContextInformation = clientContextInformation;
    }

    // We need thread-safety here e.g. because of size determination, see getSize() method.
    // Also probably because of MID-5355, although it's a bit unclear.
    Map<QK, QR> queries = new ConcurrentHashMap<>();

    public List<V> getQueryResult(
            Class<O> type,
            ObjectQuery query,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext params,
            PrismContext prismContext) {
        QK queryKey = createQueryKey(type, query, searchStrategy, params, prismContext);
        if (queryKey != null) { // TODO BRUTAL HACK
            QR result = queries.get(queryKey);
            if (result != null) {
                return result.getResultingList();
            }
        }
        return null;
    }

    public void putQueryResult(
            Class<O> type,
            ObjectQuery query,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext params,
            List<V> resultList,
            List<PrismObject<O>> rawResultList,
            PrismContext prismContext) {
        QK queryKey = createQueryKey(type, query, searchStrategy, params, prismContext);
        if (queryKey != null) { // TODO BRUTAL HACK
            QR queryResult = createQueryResult(resultList, rawResultList);
            queries.put(queryKey, queryResult);
        }
    }

    abstract protected QK createQueryKey(
            Class<O> type,
            ObjectQuery query,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext params,
            PrismContext prismContext);

    protected abstract QR createQueryResult(List<V> resultList, List<PrismObject<O>> rawResultList);

    @Override
    public String description() {
        return "Q:"+queries.size();
    }

    @Override
    protected int getSize() {
        return queries.size();
    }

    @Override
    protected void dumpContent(String threadName) {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            queries.forEach((qk, qr) -> LOGGER.info("Cached search expression evaluation [{}] {}: {}", threadName, qk, qr));
        }
    }
}
